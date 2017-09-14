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

package org.tobi29.scapes.vanilla.basics.gui

import org.tobi29.scapes.client.gui.GuiComponentItemButton
import org.tobi29.scapes.client.gui.GuiMenuSingle
import org.tobi29.scapes.client.gui.GuiUtils
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.push
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.packets.PacketInventoryInteraction
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB

open class GuiInventory(name: String,
                        protected val player: MobPlayerClientMainVB,
                        style: GuiStyle) : GuiMenuSingle(player.game, name, style) {
    protected val topPane = pane.addVert(0.0, 0.0, -1.0, -1.0,
            ::GuiComponentGroup)
    protected val inventoryPane = pane.addVert(16.0, 5.0, 350.0, 150.0,
            ::GuiComponentVisiblePane)

    init {
        val buttons = ArrayList<GuiComponent>(10)
        var i = 10
        buttons.clear()
        for (y in 0..2) {
            val row = inventoryPane.addVert(0.0, 0.0, -1.0, -1.0,
                    ::GuiComponentGroupSlab)
            for (x in 0..9) {
                buttons.add(
                        row.addHori(5.0, 5.0, -1.0, -1.0) { button(it, i) })
                i++
            }
            buttons.clear()
        }
        inventoryPane.addVert(0.0, 0.0, -1.0, 10.0, ::GuiComponentGroup)
        i = 0
        val row = inventoryPane.addVert(0.0, 0.0, -1.0, -1.0,
                ::GuiComponentGroupSlab)
        for (x in 0..9) {
            buttons.add(
                    row.addHori(5.0, 5.0, -1.0, -1.0) { button(it, i) })
            i++
        }

        on(GuiAction.BACK, { player.closeGui() })
    }

    protected fun button(parent: GuiLayoutData,
                         slot: Int): GuiComponentItemButton {
        return button(parent, "Container", slot)
    }

    protected fun button(parent: GuiLayoutData,
                         id: String,
                         slot: Int): GuiComponentItemButton {
        val inventory = player.inventories().accessUnsafe(id)
        val button = GuiComponentItemButton(parent, inventory.item(slot))
        button.on(GuiEvent.CLICK_LEFT) { leftClick(id, slot) }
        button.on(GuiEvent.CLICK_RIGHT) { rightClick(id, slot) }
        return button
    }

    protected fun leftClick(id: String,
                            i: Int) {
        player.connection().send(
                PacketInventoryInteraction(player.registry, player,
                        PacketInventoryInteraction.LEFT, id, i))
    }

    protected fun rightClick(id: String,
                             i: Int) {
        player.connection().send(
                PacketInventoryInteraction(player.registry, player,
                        PacketInventoryInteraction.RIGHT, id, i))
    }

    public override fun renderOverlay(gl: GL,
                                      shader: Shader,
                                      pixelSize: Vector2d) {
        val cursorX: Double
        val cursorY: Double
        val cursor = engine.guiController.cursors().firstOrNull()
        if (cursor != null) {
            val guiPos = cursor.currentPos()
            cursorX = guiPos.x
            cursorY = guiPos.y
        } else {
            cursorX = Double.NaN
            cursorY = Double.NaN
        }
        gl.matrixStack.push { matrix ->
            matrix.identity()
            matrix.modelViewProjection().orthogonal(0.0f, 0.0f,
                    gl.contentWidth.toFloat(),
                    gl.contentHeight.toFloat())
            player.inventories().access("Hold") { inventory ->
                GuiUtils.items(cursorX.toFloat(), cursorY.toFloat(), 60.0f,
                        60.0f, inventory.item(0), gl, shader, style.font,
                        pixelSize)
            }
        }
    }
}
