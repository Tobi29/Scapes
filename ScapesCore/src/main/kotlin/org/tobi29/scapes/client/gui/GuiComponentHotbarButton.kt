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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.GuiComponentButtonHeavy
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.gui.GuiRenderer
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.entity.client.MobPlayerClientMain

class GuiComponentHotbarButton(parent: GuiLayoutData, item: ItemStack,
                               private val player: MobPlayerClientMain, private val slot: Int) : GuiComponentButtonHeavy(
        parent) {
    private val item: GuiComponentItem
    private var model: Model? = null

    init {
        this.item = addSubHori(0.0, 0.0, -1.0, -1.0) {
            GuiComponentItem(it, item)
        }
    }

    fun item(): ItemStack {
        return item.item()
    }

    fun setItem(item: ItemStack) {
        this.item.setItem(item)
    }

    public override fun renderComponent(gl: GL,
                                        shader: Shader,
                                        size: Vector2d,
                                        pixelSize: Vector2d,
                                        delta: Double) {
        if (player.inventorySelectLeft() == slot) {
            gl.textures().bind("Scapes:image/gui/HotbarLeft", gl)
            model!!.render(gl, shader)
        } else if (player.inventorySelectRight() == slot) {
            gl.textures().bind("Scapes:image/gui/HotbarRight", gl)
            model!!.render(gl, shader)
        }
    }

    public override fun updateMesh(renderer: GuiRenderer,
                                   size: Vector2d) {
        super.updateMesh(renderer, size)
        model = createVCTI(engine,
                floatArrayOf(0.0f, size.floatY() - 32.0f, 0.0f, size.floatY(),
                        size.floatY() - 32.0f, 0.0f, 0.0f, -32.0f, 0.0f,
                        size.floatY(), -32.0f, 0.0f),
                floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
                floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f),
                intArrayOf(0, 1, 2, 3, 2, 1), RenderType.TRIANGLES)
    }
}