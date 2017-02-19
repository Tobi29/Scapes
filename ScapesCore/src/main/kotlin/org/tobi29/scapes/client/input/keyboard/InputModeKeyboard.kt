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
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.entity.client.MobPlayerClientMain

class InputModeKeyboard(engine: ScapesEngine,
                        private val controller: ControllerDefault,
                        configMap: MutableTagMap) : InputMode {
    override val events = EventDispatcher()
    override val listenerOwner = ListenerOwnerHandle()
    private val tagMap: MutableTagMap
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
        tagMap = configMap.mapMut("Default")
        defaultConfig(tagMap)

        val miscTag = tagMap.mapMut("Misc")
        val miscScrollTag = miscTag.mapMut("Scroll")
        val scrollSensitivity = miscScrollTag["Sensitivity"]?.toDouble() ?: 0.0

        guiController = GuiControllerMouse(engine, controller,
                scrollSensitivity)

        val movementTag = tagMap.mapMut("Movement")
        walkForward = ControllerKeyReference.valueOf(
                movementTag["Forward"].toString())
        walkBackward = ControllerKeyReference.valueOf(
                movementTag["Backward"].toString())
        walkLeft = ControllerKeyReference.valueOf(
                movementTag["Left"].toString())
        walkRight = ControllerKeyReference.valueOf(
                movementTag["Right"].toString())
        walkSprint = ControllerKeyReference.valueOf(
                movementTag["Sprint"].toString())
        jump = ControllerKeyReference.valueOf(
                movementTag["Jump"].toString())

        val cameraTag = tagMap.mapMut("Camera")
        val cameraSensitivity = cameraTag["Sensitivity"]?.toDouble() ?: 0.0

        val actionTag = tagMap.mapMut("Action")
        left = ControllerKeyReference.valueOf(actionTag["Left"].toString())
        right = ControllerKeyReference.valueOf(
                actionTag["Right"].toString())

        val menuTag = tagMap.mapMut("Menu")
        val inventory = ControllerKeyReference.valueOf(
                menuTag["Inventory"].toString())
        val menu = ControllerKeyReference.valueOf(menuTag["Menu"].toString())
        val chat = ControllerKeyReference.valueOf(menuTag["Chat"].toString())

        val hotbarTag = tagMap.mapMut("Hotbar")
        val hotbarAdd = ControllerKeyReference.valueOf(
                hotbarTag["Add"].toString())
        val hotbarSubtract = ControllerKeyReference.valueOf(
                hotbarTag["Subtract"].toString())
        val hotbarLeft = ControllerKeyReference.valueOf(
                hotbarTag["Left"].toString())
        val hotbarBoth = ControllerKeyReference.valueOf(
                hotbarTag["Both"].toString())
        val hotbar = Array(10) {
            ControllerKeyReference.valueOf(hotbarTag["$it"].toString())
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

    private fun defaultConfig(tagMap: MutableTagMap) {
        val movementTag = tagMap.mapMut("Movement")
        check("Forward", ControllerKey.KEY_W, movementTag)
        check("Backward", ControllerKey.KEY_S, movementTag)
        check("Left", ControllerKey.KEY_A, movementTag)
        check("Right", ControllerKey.KEY_D, movementTag)
        check("Sprint", ControllerKey.KEY_LEFT_SHIFT, movementTag)
        check("Jump", ControllerKey.KEY_SPACE, movementTag)

        val cameraTag = tagMap.mapMut("Camera")
        check("Sensitivity", 0.6, cameraTag)

        val actionTag = tagMap.mapMut("Action")
        check("Left", ControllerKey.BUTTON_LEFT, actionTag)
        check("Right", ControllerKey.BUTTON_RIGHT, actionTag)

        val menuTag = tagMap.mapMut("Menu")
        check("Inventory", ControllerKey.KEY_E, menuTag)
        check("Chat", ControllerKey.KEY_R, menuTag)
        check("Menu", ControllerKey.KEY_ESCAPE, menuTag)

        val hotbarTag = tagMap.mapMut("Hotbar")
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

        val miscTag = tagMap.mapMut("Misc")

        val miscScrollTag = miscTag.mapMut("Scroll")
        check("Sensitivity", 1.0, miscScrollTag)
    }

    private fun check(id: String,
                      def: ControllerKey,
                      tagMap: MutableTagMap) {
        if (!tagMap.containsKey(id)) {
            tagMap[id] = def.toString()
        }
    }

    private fun check(id: String,
                      def: Double,
                      tagMap: MutableTagMap) {
        if (!tagMap.containsKey(id)) {
            tagMap[id] = def
        }
    }

    override fun poll(delta: Double): Boolean {
        controller.poll()
        return controller.isActive
    }

    override fun createControlsGUI(state: GameState,
                                   prev: Gui): Gui {
        return GuiControlsDefault(state, prev,
                state.engine.game as ScapesClient, tagMap, controller,
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
