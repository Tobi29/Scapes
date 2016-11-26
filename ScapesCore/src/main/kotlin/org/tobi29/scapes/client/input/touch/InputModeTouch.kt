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
package org.tobi29.scapes.client.input.touch

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.gui.GuiMessage
import org.tobi29.scapes.client.gui.GuiPause
import org.tobi29.scapes.client.input.InputMode
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.input.ControllerTouch
import org.tobi29.scapes.engine.utils.math.angleDiff
import org.tobi29.scapes.engine.utils.math.matrix.Matrix4f
import org.tobi29.scapes.engine.utils.math.toDeg
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.packets.PacketInteraction

class InputModeTouch(engine: ScapesEngine, private val controller: ControllerTouch) : InputMode {
    private val guiController: GuiController
    private val swipe = MutableVector2d()
    private val direction = MutableVector2d()
    private val matrix1 = Matrix4f()
    private val matrix2 = Matrix4f()
    private var swipeStart: Vector2d? = null
    private var walkUp = false
    private var walkDown = false
    private var walkLeft = false
    private var walkRight = false
    private var leftHand = false
    private var rightHand = false
    private var lastTouch: Long = 0

    init {
        guiController = GuiControllerTouch(engine, controller)
    }

    override fun poll(): Boolean {
        controller.poll()
        return controller.isActive
    }

    override fun createControlsGUI(state: GameState,
                                   prev: Gui): Gui {
        return GuiMessage(state, prev, "Oi!", "No haxing pls >:V",
                prev.style)
    }

    override fun createInGameGUI(gui: Gui,
                                 world: WorldClient) {
        val size = 70
        val gap = 5
        val buttonsColumn = gui.addHori(0.0, 0.0,
                80.0, -1.0, ::GuiComponentGroup)
        val buttonsRow = buttonsColumn.addVert(0.0, 0.0,
                -1.0, 40.0, ::GuiComponentGroupSlab)
        val menu = buttonsRow.addHori(5.0, 5.0, -1.0, -1.0,
                ::GuiComponentButton)
        val inventory = buttonsRow.addHori(5.0, 5.0, -1.0, -1.0,
                ::GuiComponentButton)
        gui.spacer()
        val padColumn = gui.addHori(0.0, 0.0,
                (size * 3 + (gap shl 1)).toDouble(), -1.0,
                ::GuiComponentGroup)
        padColumn.spacer()
        val pad = padColumn.addVert(0.0, 0.0, -1.0,
                ((size shl 1) + gap).toDouble(), ::GuiComponentGroup)
        val padTop = pad.addVert(0.0, 0.0, -1.0, -1.0, ::GuiComponentGroupSlab)
        val padBottom = pad.addVert(0.0, 0.0, -1.0, -1.0,
                ::GuiComponentGroupSlab)
        padTop.addHori(5.0, 5.0, -1.0, -1.0, ::GuiComponentGroup)
        val padUp = padTop.addHori(5.0, 5.0, -1.0, -1.0, ::GuiComponentButton)
        padTop.addHori(5.0, 5.0, -1.0, -1.0, ::GuiComponentGroup)
        val padLeft = padBottom.addHori(5.0, 5.0, -1.0, -1.0,
                ::GuiComponentButton)
        val padDown = padBottom.addHori(5.0, 5.0, -1.0, -1.0,
                ::GuiComponentButton)
        val padRight = padBottom.addHori(5.0, 5.0, -1.0, -1.0,
                ::GuiComponentButton)
        val swipe = gui.add(0.0, 0.0, -1.0, -1.0, ::GuiComponentPane)

        padUp.on(GuiEvent.PRESS_LEFT) { event -> walkUp = true }
        padUp.on(GuiEvent.DROP_LEFT) { event -> walkUp = false }
        padDown.on(GuiEvent.PRESS_LEFT) { event -> walkDown = true }
        padDown.on(GuiEvent.DROP_LEFT) { event -> walkDown = false }
        padLeft.on(GuiEvent.PRESS_LEFT) { event -> walkLeft = true }
        padLeft.on(GuiEvent.DROP_LEFT) { event -> walkLeft = false }
        padRight.on(GuiEvent.PRESS_LEFT) { event -> walkRight = true }
        padRight.on(GuiEvent.DROP_LEFT) { event -> walkRight = false }
        inventory.on(GuiEvent.CLICK_LEFT) { event ->
            if (!world.player.closeGui()) {
                world.send(PacketInteraction(
                        PacketInteraction.OPEN_INVENTORY))
            }
        }
        menu.on(GuiEvent.CLICK_LEFT) { event ->
            if (!world.player.closeGui()) {
                world.player.openGui(
                        GuiPause(world.game, world.player,
                                world.game.engine.guiStyle))
            }
        }
        swipe.on(GuiEvent.DRAG_LEFT, { event ->
            this.swipe.plusX(event.relativeX / event.size.x * 960.0)
            this.swipe.plusY(event.relativeY / event.size.y * 540.0)
        })
        swipe.on(GuiEvent.PRESS_LEFT) { event ->
            swipeStart = Vector2d(event.x, event.y)
            if (rightHand) {
                rightHand = false
            } else if (System.currentTimeMillis() - lastTouch < 250) {
                rightHand = true
            }
            lastTouch = System.currentTimeMillis()
        }
        swipe.on(GuiEvent.DRAG_LEFT) { event ->
            var x = event.x / event.size.x * 2.0 - 1.0
            var y = 1.0 - event.y / event.size.y * 2.0
            val cam = world.scene.cam()
            matrix1.identity()
            matrix1.perspective(cam.fov,
                    (event.size.x / event.size.y).toFloat(), cam.near, cam.far)
            matrix1.rotateAccurate((-cam.tilt).toDouble(), 0.0f, 0.0f, 1.0f)
            matrix1.rotateAccurate((-cam.pitch - 90.0f).toDouble(), 1.0f, 0.0f,
                    0.0f)
            matrix1.rotateAccurate((-cam.yaw + 90.0f).toDouble(), 0.0f, 0.0f,
                    1.0f)
            matrix1.invert(matrix1, matrix2)
            val pos = matrix2.multiply(Vector3d(x, y, 1.0))
            x = pos.direction().toDeg()
            y = direction((pos as Vector2d).length(), pos.z).toDeg()
            x = angleDiff(cam.yaw.toDouble(), x)
            y -= cam.pitch.toDouble()
            direction.set(x, y)
            val swipeStart = swipeStart
            if (swipeStart != null) {
                if (!leftHand && !rightHand &&
                        System.currentTimeMillis() - lastTouch >= 250) {
                    leftHand = true
                }
                if (swipeStart.distance(
                        Vector2d(event.x, event.y)) > 10.0) {
                    this.swipeStart = null
                }
            }
        }
        swipe.on(GuiEvent.DROP_LEFT) { event ->
            leftHand = false
            rightHand = false
            swipeStart = null
        }
    }

    override fun playerController(
            player: MobPlayerClientMain): MobPlayerClientMain.Controller {
        return PlayerController(player)
    }

    override fun guiController(): GuiController {
        return guiController
    }

    override fun toString(): String {
        return "Touchscreen"
    }

    private inner class PlayerController(player: MobPlayerClientMain) : MobPlayerClientMain.Controller {
        override fun walk(): Vector2d {
            var x = 0.0
            var y = 0.0
            if (walkUp) {
                y += 1.0
            }
            if (walkDown) {
                y -= 1.0
            }
            if (walkLeft) {
                x -= 1.0
            }
            if (walkRight) {
                x += 1.0
            }
            return Vector2d(x, y)
        }

        override fun camera(delta: Double): Vector2d {
            val camera = swipe.now()
            swipe.set(0.0, 0.0)
            return camera
        }

        override fun hitDirection(): Vector2d {
            return direction.now()
        }

        override fun left(): Boolean {
            return leftHand
        }

        override fun right(): Boolean {
            return rightHand
        }

        override fun jump(): Boolean {
            return false
        }

        override fun hotbarLeft(previous: Int): Int {
            return 0
        }

        override fun hotbarRight(previous: Int): Int {
            return 9
        }
    }
}
