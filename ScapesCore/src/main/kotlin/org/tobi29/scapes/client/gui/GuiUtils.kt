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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.graphics.FontRenderer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.push
import org.tobi29.scapes.engine.gui.GuiRenderBatch
import org.tobi29.scapes.engine.math.vector.Vector2d

object GuiUtils {

    fun items(x: Float,
              y: Float,
              width: Float,
              height: Float,
              item: ItemStack?,
              gl: GL,
              shader: Shader,
              font: FontRenderer,
              pixelSize: Vector2d) {
        if (item == null) {
            return
        }
        items(x, y, width, height, item, item.amount() > 1, gl, shader, font,
                pixelSize)
    }

    fun items(x: Float,
              y: Float,
              width: Float,
              height: Float,
              item: ItemStack?,
              number: Boolean,
              gl: GL,
              shader: Shader,
              font: FontRenderer,
              pixelSize: Vector2d) {
        if (item == null) {
            return
        }
        if (item.amount() > 0) {
            gl.matrixStack.push { matrix ->
                matrix.translate(x + width / 4.0f, y + height / 4.0f, 4f)
                matrix.scale(width / 2.0f, height / 2.0f, 4f)
                item.material().renderInventory(item, gl, shader)
            }
            if (number) {
                val batch = GuiRenderBatch(pixelSize)
                font.render(
                        FontRenderer.to(batch, 2.0f, -18.0f, 1.0f, 1.0f, 1.0f,
                                1.0f),
                        item.amount().toString(), 16.0f)
                val text = batch.finish()
                gl.matrixStack.push { matrix ->
                    matrix.translate(x, y + height, 0.0f)
                    text.forEach {
                        it.second.bind(gl)
                        it.first.render(gl, shader)
                    }
                }
            }
        }
    }
}
