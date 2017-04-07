/*
 * Copyright 2012-2017 Tobi29
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

import mu.KLogging
import org.tobi29.scapes.VERSION
import org.tobi29.scapes.client.input.InputMode
import org.tobi29.scapes.client.input.InputModeDummy
import org.tobi29.scapes.client.input.gamepad.InputModeGamepad
import org.tobi29.scapes.client.input.keyboard.InputModeKeyboard
import org.tobi29.scapes.client.input.spi.InputModeProvider
import org.tobi29.scapes.client.input.touch.InputModeTouch
import org.tobi29.scapes.client.states.GameStateMenu
import org.tobi29.scapes.engine.Game
import org.tobi29.scapes.engine.GameStateStartup
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.GuiNotificationSimple
import org.tobi29.scapes.engine.input.*
import org.tobi29.scapes.engine.server.ConnectionManager
import org.tobi29.scapes.engine.utils.io.filesystem.FileCache
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPath
import org.tobi29.scapes.engine.utils.io.filesystem.createDirectories
import org.tobi29.scapes.engine.utils.tag.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ScapesClient(engine: ScapesEngine,
                   val home: FilePath,
                   val pluginCache: FilePath,
                   private val savesSupplier: (ScapesClient) -> SaveStorage,
                   lightDefaults: Boolean = false) : Game(
        engine) {
    override val name = "Scapes"
    override val id = "Scapes"
    override val version = VERSION
    val configMap = engine.configMap.mapMut("Scapes")
    val connection = ConnectionManager(engine.taskExecutor, 10)
    private val inputModesMut = ConcurrentHashMap<Controller, (MutableTagMap) -> InputMode>()
    var inputModes = emptyList<InputMode>()
        private set
    lateinit var saves: SaveStorage
        private set
    lateinit var inputMode: InputMode
        private set
    private var freezeInputMode = false

    var animations by configMap.tagBoolean("Animations", !lightDefaults)
    var bloom by configMap.tagBoolean("Bloom", !lightDefaults)
    var autoExposure by configMap.tagBoolean("AutoExposure", !lightDefaults)
    var fxaa by configMap.tagBoolean("FXAA", !lightDefaults)
    var renderDistance by configMap.tagDouble("RenderDistance",
            if (lightDefaults) 64.0 else 128.0)
    var resolutionMultiplier by configMap.tagDouble("ResolutionMultiplier",
            if (lightDefaults) 0.5 else 1.0)

    override fun initEarly() {
        val playlistsPath = home.resolve("playlists")
        createDirectories(playlistsPath.resolve("day"))
        createDirectories(playlistsPath.resolve("night"))
        createDirectories(playlistsPath.resolve("battle"))
        createDirectories(home.resolve("plugins"))
        createDirectories(home.resolve("screenshots"))
        createDirectories(pluginCache)
        FileCache.check(pluginCache)
        val files = engine.files
        files.registerFileSystem("Scapes",
                ClasspathPath(this::class.java.classLoader,
                        "assets/scapes/tobi29"))
        saves = savesSupplier(this)
    }

    override fun init() {
        if (!configMap.containsKey("IntegratedServer")) {
            configMap["IntegratedServer"] = TagMap {
                this["Server"] = TagMap {
                    this["MaxLoadingRadius"] = 288
                    this["Socket"] = TagMap {
                        this["MaxPlayers"] = 5
                        this["WorkerCount"] = 1
                    }
                }
            }
        }
        connection.workers(1)
        engine.switchState(GameStateStartup(engine) { GameStateMenu(engine) })
        engine.events.listener<ControllerAddEvent>(this) { event ->
            loadService(engine, event.controller)?.let {
                inputModesMut[event.controller] = it
                loadInput()
            }
        }
        engine.events.listener<ControllerRemoveEvent>(this) { event ->
            inputModesMut.remove(event.controller)
            loadInput()
        }
    }

    override fun start() {
        loadInput()
    }

    override fun halt() {
    }

    override fun step(delta: Double) {
        var newInputMode: InputMode? = null
        for (inputMode in inputModes) {
            if (inputMode.poll(delta)) {
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
        super.dispose()
        connection.stop()
    }

    fun loadInput() {
        logger.info { "Loading input" }
        val configMap = configMap.mapMut("Input")
        inputModes = inputModesMut.values.asSequence().map {
            it(configMap)
        }.toList()
        changeInput(inputModes.firstOrNull() ?: InputModeDummy(engine))
    }

    private fun changeInput(inputMode: InputMode) {
        synchronized(inputModesMut) {
            this.inputMode = inputMode
            engine.guiController = inputMode.guiController()
            engine.events.fire(InputModeChangeEvent(inputMode))
        }
    }

    fun setFreezeInputMode(freezeInputMode: Boolean) {
        this.freezeInputMode = freezeInputMode
    }

    companion object : KLogging() {
        private fun loadService(engine: ScapesEngine,
                                controller: Controller): ((MutableTagMap) -> InputMode)? {
            for (provider in ServiceLoader.load(
                    InputModeProvider::class.java)) {
                try {
                    provider.get(engine, controller)?.let { return it }
                } catch (e: ServiceConfigurationError) {
                    logger.warn { "Unable to load input mode provider: $e" }
                }
            }
            if (controller is ControllerDefault) {
                return { InputModeKeyboard(engine, controller, it) }
            } else if (controller is ControllerJoystick) {
                return { InputModeGamepad(engine, controller, it) }
            } else if (controller is ControllerTouch) {
                return { InputModeTouch(engine, controller) }
            }
            return null
        }
    }
}

class InputModeChangeEvent(val inputMode: InputMode)
