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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.gui.GuiComponentButtonHeavy
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.gui.GuiRenderer
import org.tobi29.math.vector.Vector2d
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.inventory.Item

class GuiComponentHotbarButton(
        parent: GuiLayoutData,
        private val item: () -> Item?,
        private val player: MobPlayerClientMain,
        private val slot: Int
) : GuiComponentButtonHeavy(parent) {
    private var model: Model? = null
    private val textureHotbarLeft = gui.engine.graphics.textures["Scapes:image/gui/HotbarLeft"]
    private val textureHotbarRight = gui.engine.graphics.textures["Scapes:image/gui/HotbarRight"]

    init {
        addSubHori(0.0, 0.0, -1.0, -1.0) {
            GuiComponentItem(it, item)
        }
    }

    public override fun renderComponent(gl: GL,
                                        shader: Shader,
                                        size: Vector2d,
                                        pixelSize: Vector2d,
                                        delta: Double) {
        if (player.inventorySelectLeft() == slot) {
            textureHotbarLeft.tryGet()?.let {
                it.bind(gl)
                model?.render(gl, shader)
            }
        } else if (player.inventorySelectRight() == slot) {
            textureHotbarRight.tryGet()?.let {
                it.bind(gl)
                model?.render(gl, shader)
            }
        }
    }

    public override fun updateMesh(renderer: GuiRenderer,
                                   size: Vector2d) {
        super.updateMesh(renderer, size)
        model = engine.graphics.createVCTI(
                floatArrayOf(0.0f, size.y.toFloat() - 32.0f, 0.0f,
                        size.y.toFloat(), size.y.toFloat() - 32.0f, 0.0f, 0.0f,
                        -32.0f, 0.0f, size.y.toFloat(), -32.0f, 0.0f),
                floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
                floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f),
                intArrayOf(0, 1, 2, 3, 2, 1), RenderType.TRIANGLES)
    }
}
