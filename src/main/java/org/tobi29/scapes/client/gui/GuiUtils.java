/*
 * Copyright 2012-2015 Tobi29
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
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;

public class GuiUtils {
    private static final FontRenderer.Text[] NUMBERS =
            new FontRenderer.Text[257];

    public static void renderItem(float x, float y, float width, float height,
            ItemStack item, GL gl, Shader shader, FontRenderer font) {
        if (item == null) {
            return;
        }
        renderItem(x, y, width, height, item, item.getAmount() > 1, gl, shader,
                font);
    }

    public static void renderItem(float x, float y, float width, float height,
            ItemStack item, boolean number, GL gl, Shader shader,
            FontRenderer font) {
        if (item == null) {
            return;
        }
        if (item.getAmount() > 0) {
            MatrixStack matrixStack = gl.getMatrixStack();
            Matrix matrix = matrixStack.push();
            matrix.translate(x + width / 4.0f, y + height / 4.0f, 4);
            matrix.scale(width / 2.0f, height / 2.0f, 4);
            item.getMaterial().renderInventory(item, gl, shader, 1, 1, 1, 1);
            matrixStack.pop();
            if (number) {
                int i = item.getAmount();
                if (NUMBERS[i] == null) {
                    NUMBERS[i] =
                            font.render(String.valueOf(i), 2.0f, -18.0f, 16.0f,
                                    1, 1, 1, 1);
                }
                matrix = matrixStack.push();
                matrix.translate(x, y + height, 0.0f);
                NUMBERS[i].render(gl, shader);
                matrixStack.pop();
            }
        }
    }
}
