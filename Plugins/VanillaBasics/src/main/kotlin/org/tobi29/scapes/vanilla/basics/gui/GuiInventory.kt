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

package org.tobi29.scapes.vanilla.basics.gui

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.client.gui.GuiComponentItemButton
import org.tobi29.scapes.client.gui.GuiMenu
import org.tobi29.scapes.client.gui.GuiUtils
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.packets.PacketInventoryInteraction
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import java.util.*

open class GuiInventory(name: String, protected val player: MobPlayerClientMainVB,
                        style: GuiStyle) : GuiMenu(player.game, name, style) {
    protected val topPane: GuiComponentGroup
    protected val inventoryPane: GuiComponentVisiblePane
    private var hoverT: String? = null
    private var hoverNew: String? = null
    private var hoverName: String? = null
    private var hoverText = emptyList<Pair<Model, Texture>>()
    private var cursorX = Double.NaN
    private var cursorY = Double.NaN

    init {
        topPane = pane.addVert(0.0, 0.0, -1.0, -1.0, ::GuiComponentGroup)
        inventoryPane = pane.addVert(16.0, 5.0, 350.0, 150.0,
                :: GuiComponentVisiblePane)
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
            selection(-1, buttons)
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
        selection(-1, buttons)

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
        button.on(GuiEvent.CLICK_LEFT) { event -> leftClick(id, slot) }
        button.on(GuiEvent.CLICK_RIGHT) { event -> rightClick(id, slot) }
        button.on(GuiEvent.HOVER) { event -> setTooltip(inventory.item(slot)) }
        return button
    }

    protected fun leftClick(id: String,
                            i: Int) {
        player.connection().send(PacketInventoryInteraction(player,
                PacketInventoryInteraction.LEFT, id, i))
    }

    protected fun rightClick(id: String,
                             i: Int) {
        player.connection().send(PacketInventoryInteraction(player,
                PacketInventoryInteraction.RIGHT, id, i))
    }

    public override fun updateComponent(delta: Double) {
        val cursor = engine.guiController.cursors().findAny()?.orElse(null)
        if (cursor != null) {
            val guiPos = cursor.currentGuiPos()
            cursorX = guiPos.x
            cursorY = guiPos.y
        } else {
            cursorX = Double.NaN
            cursorY = Double.NaN
        }
        hoverT = hoverNew
        hoverNew = null
    }

    public override fun renderOverlay(gl: GL,
                                      shader: Shader,
                                      pixelSize: Vector2d) {
        val font = style.font
        hoverT?.let {
            val matrixStack = gl.matrixStack()
            if (it != hoverName) {
                updateText(it, font, pixelSize)
            }
            val matrix = matrixStack.push()
            matrix.translate(cursorX.toFloat(), cursorY.toFloat(), 0.0f)
            hoverText.forEach {
                it.second.bind(gl)
                it.first.render(gl, shader)
            }
            matrixStack.pop()
        }
        player.inventories().access("Hold") { inventory ->
            GuiUtils.items(cursorX.toFloat(), cursorY.toFloat(), 30.0f, 30.0f,
                    inventory.item(0), gl, shader, font, pixelSize)
        }
    }

    protected fun setTooltip(item: ItemStack,
                             prefix: String = "") {
        hoverNew = prefix + item.material().name(item)
    }

    private fun updateText(text: String,
                           font: FontRenderer,
                           pixelSize: Vector2d) {
        hoverName = text
        val batch = GuiRenderBatch(pixelSize)
        font.render(FontRenderer.to(batch, 1.0f, 1.0f, 1.0f, 1.0f), text,
                12.0f)
        hoverText = batch.finish()
    }
}
