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

package org.tobi29.scapes.client.input.keyboard

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.gui.GuiControlsDefault
import org.tobi29.scapes.client.input.InputMode
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiController
import org.tobi29.scapes.engine.gui.GuiControllerMouse
import org.tobi29.scapes.engine.gui.PressEvent
import org.tobi29.scapes.engine.input.ControllerDefault
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.engine.input.ControllerKeyReference
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.ListenerOwnerHandle
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.entity.client.MobPlayerClientMain

class InputModeKeyboard(engine: ScapesEngine,
                        private val controller: ControllerDefault,
                        tagStructure: TagStructure) : InputMode {
    override val events = EventDispatcher()
    override val listenerOwner = ListenerOwnerHandle()
    private val tagStructure: TagStructure
    private val guiController: GuiControllerMouse
    private val walkForward: ControllerKeyReference
    private val walkBackward: ControllerKeyReference
    private val walkLeft: ControllerKeyReference
    private val walkRight: ControllerKeyReference
    private val walkSprint: ControllerKeyReference
    private val jump: ControllerKeyReference
    private val left: ControllerKeyReference
    private val right: ControllerKeyReference

    override val requiresCameraSmoothing get() = false

    init {
        this.tagStructure = tagStructure.structure("Default")
        defaultConfig(this.tagStructure)

        val miscTag = this.tagStructure.structure("Misc")
        val miscScrollTag = miscTag.structure("Scroll")
        val scrollSensitivity = miscScrollTag.getDouble("Sensitivity") ?: 0.0

        guiController = GuiControllerMouse(engine, controller,
                scrollSensitivity)

        val movementTag = this.tagStructure.getStructure("Movement")
        walkForward = ControllerKeyReference.valueOf(
                movementTag?.getString("Forward"))
        walkBackward = ControllerKeyReference.valueOf(
                movementTag?.getString("Backward"))
        walkLeft = ControllerKeyReference.valueOf(
                movementTag?.getString("Left"))
        walkRight = ControllerKeyReference.valueOf(
                movementTag?.getString("Right"))
        walkSprint = ControllerKeyReference.valueOf(
                movementTag?.getString("Sprint"))
        jump = ControllerKeyReference.valueOf(
                movementTag?.getString("Jump"))

        val cameraTag = this.tagStructure.getStructure("Camera")
        val cameraSensitivity = cameraTag?.getDouble("Sensitivity") ?: 0.0

        val actionTag = this.tagStructure.getStructure("Action")
        left = ControllerKeyReference.valueOf(actionTag?.getString("Left"))
        right = ControllerKeyReference.valueOf(
                actionTag?.getString("Right"))

        val menuTag = this.tagStructure.getStructure("Menu")
        val inventory = ControllerKeyReference.valueOf(
                menuTag?.getString("Inventory"))
        val menu = ControllerKeyReference.valueOf(menuTag?.getString("Menu"))
        val chat = ControllerKeyReference.valueOf(menuTag?.getString("Chat"))

        val hotbarTag = this.tagStructure.getStructure("Hotbar")
        val hotbarAdd = ControllerKeyReference.valueOf(
                hotbarTag?.getString("Add"))
        val hotbarSubtract = ControllerKeyReference.valueOf(
                hotbarTag?.getString("Subtract"))
        val hotbarLeft = ControllerKeyReference.valueOf(
                hotbarTag?.getString("Left"))
        val hotbarBoth = ControllerKeyReference.valueOf(
                hotbarTag?.getString("Both"))
        val hotbar = Array(10) {
            ControllerKeyReference.valueOf(hotbarTag?.getString("$it"))
        }

        controller.events.listener<ControllerDefault.MouseDeltaSyncEvent>(
                this) { event ->
            val dir = event.delta * cameraSensitivity
            events.fire(MobPlayerClientMain.InputDirectionEvent(dir))
        }
        guiController.events.listener<PressEvent>(this) { event ->
            if (event.muted) {
                return@listener
            }
            if (menu.isPressed(event.key, controller)) {
                if (MobPlayerClientMain.MenuOpenEvent().apply {
                    events.fire(this)
                }.success) {
                    event.muted = true
                    return@listener
                }
            }
            if (inventory.isPressed(event.key, controller)) {
                if (MobPlayerClientMain.MenuInventoryEvent().apply {
                    events.fire(this)
                }.success) {
                    event.muted = true
                    return@listener
                }
            }
            if (chat.isPressed(event.key, controller)) {
                if (MobPlayerClientMain.MenuChatEvent().apply {
                    events.fire(this)
                }.success) {
                    event.muted = true
                    return@listener
                }
            }
            if (hotbarLeft.isDown(controller) || hotbarBoth.isDown(
                    controller)) {
                if (hotbarAdd.isPressed(event.key, controller)) {
                    events.fire(MobPlayerClientMain.HotbarChangeLeftEvent(1))
                }
                if (hotbarSubtract.isPressed(event.key, controller)) {
                    events.fire(MobPlayerClientMain.HotbarChangeLeftEvent(-1))
                }
                val hotbarSet = ControllerKeyReference.isDown(controller,
                        *hotbar)
                if (hotbarSet != null) {
                    events.fire(MobPlayerClientMain.HotbarSetLeftEvent(
                            hotbar.indexOf(hotbarSet)))
                }
            }
            if (!hotbarLeft.isDown(controller) || hotbarBoth.isDown(
                    controller)) {
                if (hotbarAdd.isPressed(event.key, controller)) {
                    events.fire(MobPlayerClientMain.HotbarChangeRightEvent(1))
                }
                if (hotbarSubtract.isPressed(event.key, controller)) {
                    events.fire(MobPlayerClientMain.HotbarChangeRightEvent(-1))
                }
                val hotbarSet = ControllerKeyReference.isDown(controller,
                        *hotbar)
                if (hotbarSet != null) {
                    events.fire(MobPlayerClientMain.HotbarSetRightEvent(
                            hotbar.indexOf(hotbarSet)))
                }
            }
        }
    }

    private fun defaultConfig(tagStructure: TagStructure) {
        val movementTag = tagStructure.structure("Movement")
        check("Forward", ControllerKey.KEY_W, movementTag)
        check("Backward", ControllerKey.KEY_S, movementTag)
        check("Left", ControllerKey.KEY_A, movementTag)
        check("Right", ControllerKey.KEY_D, movementTag)
        check("Sprint", ControllerKey.KEY_LEFT_SHIFT, movementTag)
        check("Jump", ControllerKey.KEY_SPACE, movementTag)

        val cameraTag = tagStructure.structure("Camera")
        check("Sensitivity", 0.6, cameraTag)

        val actionTag = tagStructure.structure("Action")
        check("Left", ControllerKey.BUTTON_LEFT, actionTag)
        check("Right", ControllerKey.BUTTON_RIGHT, actionTag)

        val menuTag = tagStructure.structure("Menu")
        check("Inventory", ControllerKey.KEY_E, menuTag)
        check("Chat", ControllerKey.KEY_R, menuTag)
        check("Menu", ControllerKey.KEY_ESCAPE, menuTag)

        val hotbarTag = tagStructure.structure("Hotbar")
        check("Add", ControllerKey.SCROLL_DOWN, hotbarTag)
        check("Subtract", ControllerKey.SCROLL_UP, hotbarTag)
        check("Left", ControllerKey.KEY_LEFT_CONTROL, hotbarTag)
        check("Both", ControllerKey.KEY_LEFT_ALT, hotbarTag)
        check("0", ControllerKey.KEY_1, hotbarTag)
        check("1", ControllerKey.KEY_2, hotbarTag)
        check("2", ControllerKey.KEY_3, hotbarTag)
        check("3", ControllerKey.KEY_4, hotbarTag)
        check("4", ControllerKey.KEY_5, hotbarTag)
        check("5", ControllerKey.KEY_6, hotbarTag)
        check("6", ControllerKey.KEY_7, hotbarTag)
        check("7", ControllerKey.KEY_8, hotbarTag)
        check("8", ControllerKey.KEY_9, hotbarTag)
        check("9", ControllerKey.KEY_0, hotbarTag)

        val miscTag = tagStructure.structure("Misc")

        val miscScrollTag = miscTag.structure("Scroll")
        miscScrollTag.setDouble("Sensitivity", 1.0)
    }

    private fun check(id: String,
                      def: ControllerKey,
                      tagStructure: TagStructure) {
        if (!tagStructure.has(id)) {
            tagStructure.setString(id, def.toString())
        }
    }

    private fun check(id: String,
                      def: Double,
                      tagStructure: TagStructure) {
        if (!tagStructure.has(id)) {
            tagStructure.setDouble(id, def)
        }
    }

    override fun poll(delta: Double): Boolean {
        controller.poll()
        return controller.isActive
    }

    override fun createControlsGUI(state: GameState,
                                   prev: Gui): Gui {
        return GuiControlsDefault(state, prev,
                state.engine.game as ScapesClient, tagStructure, controller,
                prev.style)
    }

    override fun walk(): Vector2d {
        var x = 0.0
        var y = 0.0
        if (walkForward.isDown(controller)) {
            y += 1.0
        }
        if (walkBackward.isDown(controller)) {
            y -= 1.0
        }
        if (walkLeft.isDown(controller)) {
            x -= 1.0
        }
        if (walkRight.isDown(controller)) {
            x += 1.0
        }
        if (!walkSprint.isDown(controller)) {
            x *= 0.4
            y *= 0.4
        }
        return Vector2d(x, y)
    }

    override fun hitDirection(): Vector2d {
        return Vector2d.ZERO
    }

    override fun left(): Boolean {
        return left.isDown(controller)
    }

    override fun right(): Boolean {
        return right.isDown(controller)
    }

    override fun jump(): Boolean {
        return jump.isDown(controller)
    }

    override fun guiController(): GuiController {
        return guiController
    }

    override fun toString(): String {
        return "Keyboard + Mouse"
    }
}
