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

import org.tobi29.scapes.client.input.InputModeDummy
import org.tobi29.scapes.client.input.InputModeScapes
import org.tobi29.scapes.client.input.gamepad.InputModeGamepad
import org.tobi29.scapes.client.input.keyboard.InputModeKeyboard
import org.tobi29.scapes.client.input.spi.InputModeProvider
import org.tobi29.scapes.client.input.touch.InputModeTouch
import org.tobi29.scapes.engine.ComponentLifecycle
import org.tobi29.scapes.engine.ComponentStep
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.GuiNotificationSimple
import org.tobi29.scapes.engine.input.*
import org.tobi29.scapes.engine.utils.AtomicBoolean
import org.tobi29.scapes.engine.utils.ComponentTypeRegistered
import org.tobi29.scapes.engine.utils.ComponentTypeRegisteredPermission
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.tag.*
import java.util.*

class ScapesClient(val engine: ScapesEngine,
                   val home: FilePath,
                   val pluginCache: FilePath,
                   savesSupplier: (ScapesClient) -> SaveStorage,
                   lightDefaults: Boolean = false) : ComponentLifecycle, ComponentStep {
    val configMap = engine[ScapesEngine.CONFIG_MAP_COMPONENT].mapMut("Scapes")
    val saves = savesSupplier(this)

    var animations by configMap.tagBoolean("Animations", !lightDefaults)
    var bloom by configMap.tagBoolean("Bloom", !lightDefaults)
    var autoExposure by configMap.tagBoolean("AutoExposure", !lightDefaults)
    var fxaa by configMap.tagBoolean("FXAA", !lightDefaults)
    var renderDistance by configMap.tagDouble("RenderDistance",
            if (lightDefaults) 64.0 else 128.0)
    var resolutionMultiplier by configMap.tagDouble("ResolutionMultiplier",
            if (lightDefaults) 0.5 else 1.0)

    override fun init() {
        if (!configMap.containsKey("IntegratedServer")) {
            configMap["IntegratedServer"] = TagMap {
                this["Server"] = TagMap {
                    this["MaxLoadingRadius"] = 288.toTag()
                    this["Socket"] = TagMap {
                        this["MaxPlayers"] = 5.toTag()
                        this["WorkerCount"] = 1.toTag()
                    }
                }
            }
        }
    }

    companion object : KLogging() {
        val COMPONENT = ComponentTypeRegistered<ScapesClient>()
    }
}

class InputManagerScapes(
        engine: ScapesEngine,
        configMap: MutableTagMap
) : InputManager<InputModeScapes>(engine, configMap,
        InputModeDummy(engine)) {
    private var initialMode = AtomicBoolean(true)

    override fun inputMode(controller: Controller) =
            loadService(engine, controller)

    override fun inputModeChanged(inputMode: InputModeScapes) {
        events.fire(InputModeChangeEvent(inputMode))
        if (!initialMode.getAndSet(false)) {
            engine.notifications.add {
                GuiNotificationSimple(it,
                        engine.graphics.textures["Scapes:image/gui/input/Default"],
                        inputMode.toString())
            }
        }
    }

    companion object : KLogging() {
        val COMPONENT = ComponentTypeRegistered<InputManagerScapes>()

        private fun loadService(engine: ScapesEngine,
                                controller: Controller): ((MutableTagMap) -> InputModeScapes)? {
            for (provider in ServiceLoader.load(
                    InputModeProvider::class.java)) {
                try {
                    provider.get(engine, controller)?.let { return it }
                } catch (e: ServiceConfigurationError) {
                    ScapesClient.logger.warn { "Unable to load input mode provider: $e" }
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

class InputModeChangeEvent(val inputMode: InputModeScapes)

interface DialogProvider {
    fun openMusicDialog(result: (String, ReadableByteStream) -> Unit)

    fun openPluginDialog(result: (String, ReadableByteStream) -> Unit)

    fun openSkinDialog(result: (String, ReadableByteStream) -> Unit)

    fun saveScreenshotDialog(result: (FilePath) -> Unit)

    companion object {
        val COMPONENT = ComponentTypeRegisteredPermission<DialogProvider>(
                "scapes.dialogs")
    }
}
