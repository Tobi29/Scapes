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

package org.tobi29.scapes.client.input.keyboard

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.gui.GuiChatWrite
import org.tobi29.scapes.client.gui.desktop.GuiControlsDefault
import org.tobi29.scapes.client.gui.desktop.GuiPause
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
import org.tobi29.scapes.engine.utils.filter
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.packets.PacketInteraction

class InputModeKeyboard(engine: ScapesEngine, private val controller: ControllerDefault,
                        tagStructure: TagStructure) : InputMode {
    private val tagStructure: TagStructure
    private val guiController: GuiControllerMouse

    init {
        this.tagStructure = tagStructure.structure("Default")
        defaultConfig(this.tagStructure)
        val miscTag = this.tagStructure.structure("Misc")
        val miscScrollTag = miscTag.structure("Scroll")
        val scrollSensitivity = miscScrollTag.getDouble("Sensitivity") ?: 0.0
        guiController = GuiControllerMouse(engine, controller,
                scrollSensitivity)
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

    override fun poll(): Boolean {
        controller.poll()
        return controller.isActive
    }

    override fun createControlsGUI(state: GameState,
                                   prev: Gui): Gui {
        return GuiControlsDefault(state, prev,
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

    override fun toString(): String {
        return "Keyboard + Mouse"
    }

    private inner class PlayerController(player: MobPlayerClientMain) : MobPlayerClientMain.Controller {
        private val walkForward: ControllerKeyReference
        private val walkBackward: ControllerKeyReference
        private val walkLeft: ControllerKeyReference
        private val walkRight: ControllerKeyReference
        private val walkSprint: ControllerKeyReference
        private val jump: ControllerKeyReference
        private val inventory: ControllerKeyReference
        private val menu: ControllerKeyReference
        private val chat: ControllerKeyReference
        private val left: ControllerKeyReference
        private val right: ControllerKeyReference
        private val hotbarAdd: ControllerKeyReference
        private val hotbarSubtract: ControllerKeyReference
        private val hotbarLeft: ControllerKeyReference
        private val hotbarBoth: ControllerKeyReference
        private val hotbar0: ControllerKeyReference
        private val hotbar1: ControllerKeyReference
        private val hotbar2: ControllerKeyReference
        private val hotbar3: ControllerKeyReference
        private val hotbar4: ControllerKeyReference
        private val hotbar5: ControllerKeyReference
        private val hotbar6: ControllerKeyReference
        private val hotbar7: ControllerKeyReference
        private val hotbar8: ControllerKeyReference
        private val hotbar9: ControllerKeyReference
        private val cameraSensitivity: Double
        private val scrollSensitivity: Double

        init {
            val movementTag = tagStructure.getStructure("Movement")
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

            val cameraTag = tagStructure.getStructure("Camera")
            cameraSensitivity = cameraTag?.getDouble("Sensitivity") ?: 0.0

            val actionTag = tagStructure.getStructure("Action")
            left = ControllerKeyReference.valueOf(actionTag?.getString("Left"))
            right = ControllerKeyReference.valueOf(
                    actionTag?.getString("Right"))

            val menuTag = tagStructure.getStructure("Menu")
            inventory = ControllerKeyReference.valueOf(
                    menuTag?.getString("Inventory"))
            menu = ControllerKeyReference.valueOf(menuTag?.getString("Menu"))
            chat = ControllerKeyReference.valueOf(menuTag?.getString("Chat"))

            val hotbarTag = tagStructure.getStructure("Hotbar")
            hotbarAdd = ControllerKeyReference.valueOf(
                    hotbarTag?.getString("Add"))
            hotbarSubtract = ControllerKeyReference.valueOf(
                    hotbarTag?.getString("Subtract"))
            hotbarLeft = ControllerKeyReference.valueOf(
                    hotbarTag?.getString("Left"))
            hotbarBoth = ControllerKeyReference.valueOf(
                    hotbarTag?.getString("Both"))
            hotbar0 = ControllerKeyReference.valueOf(hotbarTag?.getString("0"))
            hotbar1 = ControllerKeyReference.valueOf(hotbarTag?.getString("1"))
            hotbar2 = ControllerKeyReference.valueOf(hotbarTag?.getString("2"))
            hotbar3 = ControllerKeyReference.valueOf(hotbarTag?.getString("3"))
            hotbar4 = ControllerKeyReference.valueOf(hotbarTag?.getString("4"))
            hotbar5 = ControllerKeyReference.valueOf(hotbarTag?.getString("5"))
            hotbar6 = ControllerKeyReference.valueOf(hotbarTag?.getString("6"))
            hotbar7 = ControllerKeyReference.valueOf(hotbarTag?.getString("7"))
            hotbar8 = ControllerKeyReference.valueOf(hotbarTag?.getString("8"))
            hotbar9 = ControllerKeyReference.valueOf(hotbarTag?.getString("9"))
            scrollSensitivity = hotbarTag?.getDouble("Sensitivity") ?: 0.0

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

        override fun camera(delta: Double): Vector2d {
            val camera = Vector2d(controller.deltaX(), controller.deltaY())
            return camera.times(cameraSensitivity)
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
            if (hotbarLeft.isDown(controller) || hotbarBoth.isDown(
                    controller)) {
                if (hotbarAdd.isPressed(controller)) {
                    previous++
                }
                if (hotbarSubtract.isPressed(controller)) {
                    previous--
                }
                val hotbar = ControllerKeyReference.isDown(controller,
                        hotbar0, hotbar1, hotbar2,
                        hotbar3, hotbar4, hotbar5, hotbar6,
                        hotbar7, hotbar8, hotbar9)
                if (hotbar != null) {
                    if (hotbar === hotbar0) {
                        previous = 0
                    } else if (hotbar === hotbar1) {
                        previous = 1
                    } else if (hotbar === hotbar2) {
                        previous = 2
                    } else if (hotbar === hotbar3) {
                        previous = 3
                    } else if (hotbar === hotbar4) {
                        previous = 4
                    } else if (hotbar === hotbar5) {
                        previous = 5
                    } else if (hotbar === hotbar6) {
                        previous = 6
                    } else if (hotbar === hotbar7) {
                        previous = 7
                    } else if (hotbar === hotbar8) {
                        previous = 8
                    } else if (hotbar === hotbar9) {
                        previous = 9
                    }
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
                val hotbar = ControllerKeyReference.isDown(controller,
                        hotbar0, hotbar1, hotbar2,
                        hotbar3, hotbar4, hotbar5, hotbar6,
                        hotbar7, hotbar8, hotbar9)
                if (hotbar != null) {
                    if (hotbar === hotbar0) {
                        previous = 0
                    } else if (hotbar === hotbar1) {
                        previous = 1
                    } else if (hotbar === hotbar2) {
                        previous = 2
                    } else if (hotbar === hotbar3) {
                        previous = 3
                    } else if (hotbar === hotbar4) {
                        previous = 4
                    } else if (hotbar === hotbar5) {
                        previous = 5
                    } else if (hotbar === hotbar6) {
                        previous = 6
                    } else if (hotbar === hotbar7) {
                        previous = 7
                    } else if (hotbar === hotbar8) {
                        previous = 8
                    } else if (hotbar === hotbar9) {
                        previous = 9
                    }
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
