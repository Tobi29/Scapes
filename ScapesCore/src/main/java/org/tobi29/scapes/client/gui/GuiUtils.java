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

package org.tobi29.scapes.client.gui;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.gui.GuiRenderBatch;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

import java.util.List;

public final class GuiUtils {
    private GuiUtils() {
    }

    public static void items(float x, float y, float width, float height,
            ItemStack item, GL gl, Shader shader, FontRenderer font,
            Vector2 pixelSize) {
        if (item == null) {
            return;
        }
        items(x, y, width, height, item, item.amount() > 1, gl, shader, font,
                pixelSize);
    }

    public static void items(float x, float y, float width, float height,
            ItemStack item, boolean number, GL gl, Shader shader,
            FontRenderer font, Vector2 pixelSize) {
        if (item == null) {
            return;
        }
        if (item.amount() > 0) {
            MatrixStack matrixStack = gl.matrixStack();
            Matrix matrix = matrixStack.push();
            matrix.translate(x + width / 4.0f, y + height / 4.0f, 4);
            matrix.scale(width / 2.0f, height / 2.0f, 4);
            item.material().renderInventory(item, gl, shader);
            matrixStack.pop();
            if (number) {
                GuiRenderBatch batch = new GuiRenderBatch(pixelSize);
                font.render(FontRenderer
                                .to(batch, 2.0f, -18.0f, 1.0f, 1.0f, 1.0f, 1.0f),
                        String.valueOf(item.amount()), 16.0f);
                List<Pair<Model, Texture>> text = batch.finish();
                matrix = matrixStack.push();
                matrix.translate(x, y + height, 0.0f);
                Streams.forEach(text, mesh -> {
                    mesh.b.bind(gl);
                    mesh.a.render(gl, shader);
                });
                matrixStack.pop();
            }
        }
    }
}
