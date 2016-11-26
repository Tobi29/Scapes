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
package org.tobi29.scapes.client.input.gamepad

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.gui.GuiChatWrite
import org.tobi29.scapes.client.gui.GuiControlsGamepad
import org.tobi29.scapes.client.gui.GuiPause
import org.tobi29.scapes.client.input.InputMode
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiController
import org.tobi29.scapes.engine.gui.PressEvent
import org.tobi29.scapes.engine.input.ControllerJoystick
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.engine.input.ControllerKeyReference
import org.tobi29.scapes.engine.utils.filter
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.mix
import org.tobi29.scapes.engine.utils.math.sqrNoAbs
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.packets.PacketInteraction

class InputModeGamepad(engine: ScapesEngine, private val controller: ControllerJoystick,
                       tagStructure: TagStructure) : InputMode {
    private val tagStructure: TagStructure
    private val guiController: GuiController

    init {
        val id = controller.id()
        this.tagStructure = tagStructure.structure(id)
        defaultConfig(this.tagStructure)
        val guiTag = this.tagStructure.structure("GUI")
        val primaryButton = ControllerKeyReference.valueOf(
                guiTag.getString("Primary"))
        val secondaryButton = ControllerKeyReference.valueOf(
                guiTag.getString("Secondary"))
        val upButton = ControllerKeyReference.valueOf(guiTag.getString("Up"))
        val downButton = ControllerKeyReference.valueOf(
                guiTag.getString("Down"))
        val leftButton = ControllerKeyReference.valueOf(
                guiTag.getString("Left"))
        val rightButton = ControllerKeyReference.valueOf(
                guiTag.getString("Right"))
        guiController = GuiControllerGamepad(engine, controller, primaryButton,
                secondaryButton, upButton, downButton, leftButton,
                rightButton)
    }

    private fun defaultConfig(tagStructure: TagStructure) {
        val movementTag = tagStructure.structure("Movement")
        check("X", 0, movementTag)
        check("Y", 1, movementTag)
        check("Jump", ControllerKey.BUTTON_0, movementTag)

        val cameraTag = tagStructure.structure("Camera")
        check("X", 3, cameraTag)
        check("Y", 4, cameraTag)
        check("Sensitivity", 1.0, cameraTag)

        val guiTag = tagStructure.structure("GUI")
        check("Primary", ControllerKey.BUTTON_0, guiTag)
        check("Secondary", ControllerKey.BUTTON_1, guiTag)
        check("Up", ControllerKey.AXIS_NEG_1, guiTag)
        check("Down", ControllerKey.AXIS_1, guiTag)
        check("Left", ControllerKey.AXIS_NEG_0, guiTag)
        check("Right", ControllerKey.AXIS_0, guiTag)

        val actionTag = tagStructure.structure("Action")
        check("Left", ControllerKey.AXIS_2, actionTag)
        check("Right", ControllerKey.AXIS_5, actionTag)

        val menuTag = tagStructure.structure("Menu")
        check("Inventory", ControllerKey.BUTTON_7, menuTag)
        check("Menu", ControllerKey.BUTTON_10, menuTag)
        check("Chat", ControllerKey.BUTTON_6, menuTag)

        val hotbarTag = tagStructure.structure("Hotbar")
        check("Add", ControllerKey.BUTTON_5, hotbarTag)
        check("Subtract", ControllerKey.BUTTON_4, hotbarTag)
        check("Left", ControllerKey.BUTTON_2, hotbarTag)
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

    private fun check(id: String,
                      def: Int,
                      tagStructure: TagStructure) {
        if (!tagStructure.has(id)) {
            tagStructure.setInt(id, def)
        }
    }

    override fun toString(): String {
        return controller.name()
    }

    override fun poll(): Boolean {
        controller.poll()
        return controller.isActive
    }

    override fun createControlsGUI(state: GameState,
                                   prev: Gui): Gui {
        return GuiControlsGamepad(state, prev,
                state.engine.game as ScapesClient, tagStructure, controller,
                prev.style)
    }

    override fun playerController(
            player: MobPlayerClientMain): MobPlayerClientMain.Controller {
        return PlayerController(player)
    }

    override fun guiController(): GuiController {
        return guiController
    }

    private inner class PlayerController(player: MobPlayerClientMain) : MobPlayerClientMain.Controller {
        private val axisWalkX: Int
        private val axisWalkY: Int
        private val axisCameraX: Int
        private val axisCameraY: Int
        private val jump: ControllerKeyReference
        private val inventory: ControllerKeyReference
        private val menu: ControllerKeyReference
        private val chat: ControllerKeyReference
        private val left: ControllerKeyReference
        private val right: ControllerKeyReference
        private val hotbarAdd: ControllerKeyReference
        private val hotbarSubtract: ControllerKeyReference
        private val hotbarLeft: ControllerKeyReference
        private val cameraSensitivity: Double

        init {
            val movementTag = tagStructure.getStructure("Movement")
            axisWalkX = movementTag?.getInt("X") ?: 0
            axisWalkY = movementTag?.getInt("Y") ?: 0
            jump = ControllerKeyReference.valueOf(
                    movementTag?.getString("Jump"))

            val cameraTag = tagStructure.getStructure("Camera")
            axisCameraX = cameraTag?.getInt("X") ?: 0
            axisCameraY = cameraTag?.getInt("Y") ?: 0
            cameraSensitivity = (cameraTag?.getDouble(
                    "Sensitivity") ?: 0.0) * 400.0

            val menuTag = tagStructure.getStructure("Menu")
            inventory = ControllerKeyReference.valueOf(
                    menuTag?.getString("Inventory"))
            menu = ControllerKeyReference.valueOf(menuTag?.getString("Menu"))
            chat = ControllerKeyReference.valueOf(menuTag?.getString("Chat"))

            val actionTag = tagStructure.getStructure("Action")
            left = ControllerKeyReference.valueOf(actionTag?.getString("Left"))
            right = ControllerKeyReference.valueOf(
                    actionTag?.getString("Right"))

            val hotbarTag = tagStructure.getStructure("Hotbar")
            hotbarAdd = ControllerKeyReference.valueOf(
                    hotbarTag?.getString("Add"))
            hotbarSubtract = ControllerKeyReference.valueOf(
                    hotbarTag?.getString("Subtract"))
            hotbarLeft = ControllerKeyReference.valueOf(
                    hotbarTag?.getString("Left"))

            guiController.events.listener<PressEvent>(player) { event ->
                if (event.muted) {
                    return@listener
                }
                if (player.currentGui()?.filter { it is GuiChatWrite } == null && menu.isPressed(
                        event.key, controller)) {
                    if (!player.closeGui()) {
                        player.openGui(GuiPause(player.game, player,
                                player.game.engine.guiStyle))
                    }
                    event.muted = true
                    return@listener
                }
                if (player.currentGui()?.filter { it is GuiChatWrite } == null && inventory.isPressed(
                        event.key, controller)) {
                    if (!player.closeGui()) {
                        player.world.send(PacketInteraction(
                                PacketInteraction.OPEN_INVENTORY))
                    }
                    event.muted = true
                    return@listener
                }
                if (player.currentGui()?.filter { it is GuiChatWrite } == null && chat.isPressed(
                        event.key, controller)) {
                    if (!player.hasGui()) {
                        player.openGui(GuiChatWrite(player.game, player,
                                player.game.engine.guiStyle))
                        event.muted = true
                        return@listener
                    }
                }
            }
        }

        override fun walk(): Vector2d {
            return Vector2d(controller.axis(axisWalkX),
                    -controller.axis(axisWalkY))
        }

        override fun camera(delta: Double): Vector2d {
            var x = controller.axis(axisCameraX)
            var y = controller.axis(axisCameraY)
            val cx = sqrNoAbs(x)
            val cy = sqrNoAbs(y)
            x = mix(x, cx, 0.5)
            y = mix(y, cy, 0.5)
            return Vector2d(x, y).times(cameraSensitivity * delta)
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

        override fun hotbarLeft(previous: Int): Int {
            var previous = previous
            if (hotbarLeft.isDown(controller)) {
                if (hotbarAdd.isPressed(controller)) {
                    previous++
                }
                if (hotbarSubtract.isPressed(controller)) {
                    previous--
                }
            }
            previous %= 10
            if (previous < 0) {
                previous += 10
            }
            return previous
        }

        override fun hotbarRight(previous: Int): Int {
            var previous = previous
            if (!hotbarLeft.isDown(controller)) {
                if (hotbarAdd.isPressed(controller)) {
                    previous++
                }
                if (hotbarSubtract.isPressed(controller)) {
                    previous--
                }
            }
            previous %= 10
            if (previous < 0) {
                previous += 10
            }
            return previous
        }
    }
}
