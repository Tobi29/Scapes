/*
 * Copyright 2012-2016 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tobi29.scapes.client

import java8.util.stream.Stream
import mu.KLogging
import org.tobi29.scapes.VERSION
import org.tobi29.scapes.client.input.InputMode
import org.tobi29.scapes.client.input.gamepad.InputModeGamepad
import org.tobi29.scapes.client.input.keyboard.InputModeKeyboard
import org.tobi29.scapes.client.input.spi.InputModeProvider
import org.tobi29.scapes.client.input.touch.InputModeTouch
import org.tobi29.scapes.client.states.GameStateMenu
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.Game
import org.tobi29.scapes.engine.GameStateStartup
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.SceneEmpty
import org.tobi29.scapes.engine.gui.GuiNotificationSimple
import org.tobi29.scapes.engine.input.*
import org.tobi29.scapes.engine.server.ConnectionWorker
import org.tobi29.scapes.engine.utils.Version
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPath
import org.tobi29.scapes.engine.utils.io.filesystem.createDirectories
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.stream
import java.io.IOException
import java.util.*

class ScapesClient(engine: ScapesEngine, private val skipIntro: Boolean,
                   private val savesSupplier: (ScapesClient) -> SaveStorage) : Game(
        engine) {
    val connection = ConnectionWorker(engine.taskExecutor)
    private val inputModes = ArrayList<InputMode>()
    private lateinit var saves: SaveStorage
    private lateinit var inputMode: InputMode
    private var freezeInputMode = false

    fun saves(): SaveStorage {
        return saves
    }

    fun inputMode(): InputMode {
        return inputMode
    }

    override val name: String
        get() = "Scapes"

    override val id: String
        get() = "Scapes"

    override val version: Version
        get() = VERSION

    override fun initEarly() {
        try {
            val path = engine.home
            val playlistsPath = path.resolve("playlists")
            createDirectories(playlistsPath.resolve("day"))
            createDirectories(playlistsPath.resolve("night"))
            createDirectories(playlistsPath.resolve("battle"))
            createDirectories(path.resolve("plugins"))
            val files = engine.files
            files.registerFileSystem("Scapes",
                    ClasspathPath(javaClass.classLoader,
                            "assets/scapes/tobi29/"))
            saves = savesSupplier(this)
        } catch (e: IOException) {
            engine.crash(e)
        }

    }

    override fun init() {
        connection.workers(1)
        if (skipIntro) {
            engine.switchState(GameStateMenu(engine))
        } else {
            engine.switchState(GameStateStartup(GameStateMenu(engine),
                    "Engine:image/Logo", 0.5, SceneEmpty(engine), engine))
        }
    }

    override fun initLate() {
        val tagStructure = engine.tagStructure
        if (!tagStructure.has("Scapes")) {
            tagStructure.setStructure("Scapes", structure {
                val lightDefaults = engine.container.formFactor() == Container.FormFactor.PHONE
                if (lightDefaults) {
                    setBoolean("Animations", false)
                    setBoolean("Bloom", false)
                    setBoolean("AutoExposure", false)
                    setBoolean("FXAA", false)
                    setDouble("RenderDistance", 64.0)
                } else {
                    setBoolean("Animations", true)
                    setBoolean("Bloom", true)
                    setBoolean("AutoExposure", true)
                    setBoolean("FXAA", true)
                    setDouble("RenderDistance", 128.0)
                }
                setStructure("IntegratedServer") {
                    setStructure("Server") {
                        setInt("MaxLoadingRadius", 288)
                        setStructure("Socket") {
                            setInt("MaxPlayers", 5)
                            setInt("WorkerCount", 1)
                            setInt("RSASize", 1024)
                        }
                    }
                }
            })
        }
        loadInput()
    }

    override fun step() {
        if (engine.container.joysticksChanged()) {
            loadInput()
        }
        var newInputMode: InputMode? = null
        for (inputMode in inputModes) {
            if (inputMode.poll()) {
                newInputMode = inputMode
            }
        }
        if (newInputMode != null && inputMode !== newInputMode &&
                !freezeInputMode) {
            logger.info { "Setting input mode to $newInputMode" }
            changeInput(newInputMode)
            engine.notifications.add {
                GuiNotificationSimple(it,
                        engine.graphics.textures()["Scapes:image/gui/Playlist"],
                        inputMode.toString())
            }
        }
    }

    override fun dispose() {
        connection.stop()
    }

    fun loadInput() {
        logger.info { "Loading input" }
        val tagStructure = engine.tagStructure.structure("Scapes").structure(
                "Input")
        inputModes.clear()
        val controller = engine.container.controller()
        if (controller != null) {
            loadService(engine, controller,
                    tagStructure)?.let { inputModes.add(it) }
        }
        val touch = engine.container.touch()
        if (touch != null) {
            loadService(engine, touch, tagStructure)?.let { inputModes.add(it) }
        }
        for (joystick in engine.container.joysticks()) {
            loadService(engine, joystick, tagStructure)?.let {
                inputModes.add(it)
            }
        }
        if (inputModes.isEmpty()) {
            throw InputException("No input mode available")
        }
        changeInput(inputModes[0])
    }

    private fun changeInput(inputMode: InputMode) {
        this.inputMode = inputMode
        engine.guiController = inputMode.guiController()
        engine.events.fire(InputModeChangeEvent(inputMode))
    }

    fun setFreezeInputMode(freezeInputMode: Boolean) {
        this.freezeInputMode = freezeInputMode
    }

    fun inputModes(): Stream<InputMode> {
        return inputModes.stream()
    }

    companion object : KLogging() {
        private fun loadService(engine: ScapesEngine,
                                controller: Controller,
                                tagStructure: TagStructure): InputMode? {
            for (provider in ServiceLoader.load(
                    InputModeProvider::class.java)) {
                try {
                    val inputMode = provider[engine, controller, tagStructure]
                    if (inputMode != null) {
                        logger.debug { "Loaded input mode: ${provider.javaClass.name}" }
                        return inputMode
                    }
                } catch (e: ServiceConfigurationError) {
                    logger.warn { "Unable to load input mode provider: $e" }
                }
            }
            if (controller is ControllerDefault) {
                return InputModeKeyboard(engine, controller, tagStructure)
            } else if (controller is ControllerJoystick) {
                return InputModeGamepad(engine, controller, tagStructure)
            } else if (controller is ControllerTouch) {
                return InputModeTouch(engine, controller)
            }
            return null
        }
    }
}

class InputModeChangeEvent(val inputMode: InputMode)
