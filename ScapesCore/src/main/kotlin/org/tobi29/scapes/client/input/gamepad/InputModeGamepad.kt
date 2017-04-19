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
package org.tobi29.scapes.client.input.gamepad

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.gui.GuiControlsGamepad
import org.tobi29.scapes.client.input.InputMode
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiController
import org.tobi29.scapes.engine.gui.PressEvent
import org.tobi29.scapes.engine.input.ControllerJoystick
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.engine.input.ControllerKeyReference
import org.tobi29.scapes.engine.input.isDown
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.math.mix
import org.tobi29.scapes.engine.utils.math.sqrNoAbs
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.entity.client.MobPlayerClientMain

class InputModeGamepad(engine: ScapesEngine,
                       private val controller: ControllerJoystick,
                       configMap: MutableTagMap) : InputMode {
    val events: EventDispatcher
    private val tagMap: MutableTagMap
    private val guiController: GuiController
    private val axisWalkX: Int
    private val axisWalkY: Int
    private val jump: ControllerKeyReference?
    private val axisCameraX: Int
    private val axisCameraY: Int
    private val left: ControllerKeyReference?
    private val right: ControllerKeyReference?
    private val cameraSensitivity: Double

    init {
        val id = controller.id()
        tagMap = configMap.mapMut(id)
        defaultConfig(tagMap)

        val guiTag = tagMap.mapMut("GUI")
        val primaryButton = ControllerKeyReference.valueOf(
                guiTag["Primary"].toString())
        val secondaryButton = ControllerKeyReference.valueOf(
                guiTag["Secondary"].toString())
        val upButton = ControllerKeyReference.valueOf(guiTag["Up"].toString())
        val downButton = ControllerKeyReference.valueOf(
                guiTag["Down"].toString())
        val leftButton = ControllerKeyReference.valueOf(
                guiTag["Left"].toString())
        val rightButton = ControllerKeyReference.valueOf(
                guiTag["Right"].toString())

        guiController = GuiControllerGamepad(engine, controller, primaryButton,
                secondaryButton, upButton, downButton, leftButton,
                rightButton)

        val movementTag = tagMap.mapMut("Movement")
        axisWalkX = movementTag["X"]?.toInt() ?: 0
        axisWalkY = movementTag["Y"]?.toInt() ?: 0
        jump = ControllerKeyReference.valueOf(movementTag["Jump"].toString())

        val cameraTag = tagMap.mapMut("Camera")
        axisCameraX = cameraTag["X"]?.toInt() ?: 0
        axisCameraY = cameraTag["Y"]?.toInt() ?: 0
        cameraSensitivity = (cameraTag["Sensitivity"]?.toDouble() ?: 0.0) * 400.0

        val actionTag = tagMap.mapMut("Action")
        left = ControllerKeyReference.valueOf(actionTag["Left"].toString())
        right = ControllerKeyReference.valueOf(actionTag["Right"].toString())

        val menuTag = tagMap.mapMut("Menu")
        val inventory = ControllerKeyReference.valueOf(
                menuTag["Inventory"].toString())
        val menu = ControllerKeyReference.valueOf(menuTag["Menu"].toString())
        val chat = ControllerKeyReference.valueOf(menuTag["Chat"].toString())

        val hotbarTag = tagMap.mapMut("Hotbar")
        val hotbarAddRight = ControllerKeyReference.valueOf(
                hotbarTag["AddRight"].toString())
        val hotbarSubtractRight = ControllerKeyReference.valueOf(
                hotbarTag["SubtractRight"].toString())
        val hotbarAddLeft = ControllerKeyReference.valueOf(
                hotbarTag["AddLeft"].toString())
        val hotbarSubtractLeft = ControllerKeyReference.valueOf(
                hotbarTag["SubtractLeft"].toString())
        val hotbarAdd = ControllerKeyReference.valueOf(
                hotbarTag["Add"].toString())
        val hotbarSubtract = ControllerKeyReference.valueOf(
                hotbarTag["Subtract"].toString())

        val references = arrayOf(menu, inventory, chat, hotbarAddRight,
                hotbarSubtractRight, hotbarAddLeft, hotbarSubtractLeft,
                hotbarAdd, hotbarSubtract)

        events = EventDispatcher(engine.events) {
            listen<PressEvent>(
                    { it.controller == guiController }) { event ->
                if (event.muted) {
                    return@listen
                }
                val pressed = ControllerKeyReference.isPressed(event.key,
                        controller, *references)
                when (pressed) {
                    menu -> {
                        if (MobPlayerClientMain.MenuOpenEvent().apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    inventory -> {
                        if (MobPlayerClientMain.MenuInventoryEvent().apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    chat -> {
                        if (MobPlayerClientMain.MenuChatEvent().apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarAddRight -> {
                        if (MobPlayerClientMain.HotbarChangeRightEvent(
                                1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarSubtractRight -> {
                        if (MobPlayerClientMain.HotbarChangeRightEvent(
                                -1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarAddLeft -> {
                        if (MobPlayerClientMain.HotbarChangeLeftEvent(1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarSubtractLeft -> {
                        if (MobPlayerClientMain.HotbarChangeLeftEvent(
                                -1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarAdd -> {
                        if (MobPlayerClientMain.HotbarChangeRightEvent(
                                1).apply {
                            events.fire(this)
                        }.success or MobPlayerClientMain.HotbarChangeLeftEvent(
                                1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarSubtract -> {
                        if (MobPlayerClientMain.HotbarChangeRightEvent(
                                -1).apply {
                            events.fire(this)
                        }.success or MobPlayerClientMain.HotbarChangeLeftEvent(
                                -1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                }
            }
        }
    }

    private fun defaultConfig(tagMap: MutableTagMap) {
        val movementTag = tagMap.mapMut("Movement")
        check("X", 0, movementTag)
        check("Y", 1, movementTag)
        check("Jump", ControllerKey.BUTTON_A, movementTag)

        val cameraTag = tagMap.mapMut("Camera")
        check("X", 3, cameraTag)
        check("Y", 4, cameraTag)
        check("Sensitivity", 1.0, cameraTag)

        val guiTag = tagMap.mapMut("GUI")
        check("Primary", ControllerKey.BUTTON_A, guiTag)
        check("Secondary", ControllerKey.BUTTON_B, guiTag)
        check("Up", ControllerKey.AXIS_NEG_1, guiTag)
        check("Down", ControllerKey.AXIS_1, guiTag)
        check("Left", ControllerKey.AXIS_NEG_0, guiTag)
        check("Right", ControllerKey.AXIS_0, guiTag)

        val actionTag = tagMap.mapMut("Action")
        check("Left", ControllerKey.BUTTON_TRIGGER_LEFT, actionTag)
        check("Right", ControllerKey.BUTTON_TRIGGER_RIGHT, actionTag)

        val menuTag = tagMap.mapMut("Menu")
        check("Inventory", ControllerKey.BUTTON_SELECT, menuTag)
        check("Menu", ControllerKey.BUTTON_START, menuTag)
        check("Chat", ControllerKey.BUTTON_DPAD_DOWN, menuTag)

        val hotbarTag = tagMap.mapMut("Hotbar")
        check("AddRight", ControllerKey.BUTTON_BUMPER_RIGHT, hotbarTag)
        check("SubtractRight", ControllerKey.BUTTON_BUMPER_LEFT, hotbarTag)
        check("AddLeft",
                ControllerKeyReference(ControllerKey.BUTTON_BUMPER_RIGHT,
                        ControllerKey.BUTTON_X), hotbarTag)
        check("SubtractLeft",
                ControllerKeyReference(ControllerKey.BUTTON_BUMPER_LEFT,
                        ControllerKey.BUTTON_X), hotbarTag)
        check("Add",
                ControllerKeyReference(ControllerKey.BUTTON_BUMPER_RIGHT,
                        ControllerKey.BUTTON_Y), hotbarTag)
        check("Subtract",
                ControllerKeyReference(ControllerKey.BUTTON_BUMPER_LEFT,
                        ControllerKey.BUTTON_Y), hotbarTag)
    }

    private fun check(id: String,
                      def: ControllerKey,
                      tagMap: MutableTagMap) {
        if (!tagMap.containsKey(id)) {
            tagMap[id] = def.toString()
        }
    }

    private fun check(id: String,
                      def: ControllerKeyReference,
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

    private fun check(id: String,
                      def: Int,
                      tagMap: MutableTagMap) {
        if (!tagMap.containsKey(id)) {
            tagMap[id] = def
        }
    }

    override fun toString(): String {
        return controller.name()
    }

    override fun enabled() {
        events.enable()
    }

    override fun disabled() {
        events.disable()
    }

    override fun poll(delta: Double): Boolean {
        controller.poll()
        val dir = run {
            var x = controller.axis(axisCameraX)
            var y = controller.axis(axisCameraY)
            val cx = sqrNoAbs(x)
            val cy = sqrNoAbs(y)
            x = mix(x, cx, 0.5)
            y = mix(y, cy, 0.5)
            Vector2d(x, y).times(cameraSensitivity * delta)
        }
        events.fire(MobPlayerClientMain.InputDirectionEvent(dir))
        return controller.isActive
    }

    override fun createControlsGUI(state: GameState,
                                   prev: Gui): Gui {
        return GuiControlsGamepad(state, prev,
                state.engine.game as ScapesClient, tagMap, controller,
                prev.style)
    }

    override fun walk(): Vector2d {
        return Vector2d(controller.axis(axisWalkX),
                -controller.axis(axisWalkY))
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
}
