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

package org.tobi29.scapes.client.input

import org.tobi29.io.tag.MutableTagMap
import org.tobi29.logging.KLogging
import org.tobi29.math.vector.Vector2d
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.gui.GuiMessage
import org.tobi29.scapes.client.input.spi.InputModeProvider
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiControllerDummy
import org.tobi29.scapes.engine.gui.GuiNotificationSimple
import org.tobi29.scapes.engine.input.*
import org.tobi29.stdex.atomic.AtomicBoolean
import org.tobi29.utils.ComponentTypeRegistered
import org.tobi29.utils.spiLoad

interface InputModeScapes : InputMode {
    val requiresCameraSmoothing get() = true

    fun createControlsGUI(state: GameState,
                          prev: Gui): Gui

    fun createInGameGUI(gui: Gui,
                        world: WorldClient) {
    }

    fun walk(): Vector2d

    fun hitDirection(): Vector2d

    fun left(): Boolean

    fun right(): Boolean

    fun jump(): Boolean
}

class InputModeDummy(private val engine: ScapesEngine) : InputModeScapes {
    override fun poll(delta: Double) = 0L

    override fun createControlsGUI(state: GameState,
                                   prev: Gui) =
            GuiMessage(state, prev, "", "", prev.style)

    override fun walk() = Vector2d.ZERO

    override fun hitDirection() = Vector2d.ZERO

    override fun left() = false

    override fun right() = false

    override fun jump() = false

    override fun guiController() = GuiControllerDummy(engine)
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
        val COMPONENT = ComponentTypeRegistered<ScapesEngine, InputManagerScapes, Any>()

        private fun loadService(engine: ScapesEngine,
                                controller: Controller): ((MutableTagMap) -> InputModeScapes)? {
            spiLoad(spiLoad<InputModeProvider>(
                    InputModeProvider::class.java.classLoader), { e ->
                logger.warn { "Unable to load input mode provider: $e" }
            }).asSequence().mapNotNull {
                it.get(engine, controller)
            }.firstOrNull()?.let { return it }
            if (controller is ControllerDesktop) {
                return {
                    InputModeKeyboard(engine,
                            controller, it)
                }
            } else if (controller is ControllerJoystick) {
                return {
                    InputModeGamepad(engine,
                            controller, it)
                }
            } else if (controller is ControllerTouch) {
                return {
                    InputModeTouch(engine,
                            controller)
                }
            }
            return null
        }
    }
}

class InputModeChangeEvent(val inputMode: InputModeScapes)
