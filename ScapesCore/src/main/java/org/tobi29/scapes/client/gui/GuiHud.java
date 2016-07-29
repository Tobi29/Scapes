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

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.gui.GuiState;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

public class GuiHud extends GuiState {
    private static final float CROSS_SIZE = 8.0f;
    private final Model cross;

    public GuiHud(GameState state, GuiStyle style) {
        super(state, style);
        cross = VAOUtility.createVCTI(state.engine(),
                new float[]{-CROSS_SIZE, -CROSS_SIZE, 0.0f, CROSS_SIZE,
                        -CROSS_SIZE, 0.0f, CROSS_SIZE, CROSS_SIZE, 0.0f,
                        -CROSS_SIZE, CROSS_SIZE, 0.0f},
                new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
                new int[]{0, 1, 2, 0, 2, 3}, RenderType.TRIANGLES);
    }

    @Override
    public void render(GL gl, Shader shader, Vector2 size, Vector2 pixelSize,
            double delta) {
        super.render(gl, shader, size, pixelSize, delta);
        if (visible) {
            MatrixStack matrixStack = gl.matrixStack();
            Matrix matrix = matrixStack.push();
            matrix.translate(
                    (float) gl.sceneWidth() / gl.sceneHeight() * 270.0f, 270.0f,
                    0.0f);
            gl.textures().bind("Scapes:image/gui/Cross", gl);
            gl.setBlending(BlendingMode.INVERT);
            cross.render(gl, shader);
            gl.setBlending(BlendingMode.NORMAL);
            matrixStack.pop();
        }
    }
}
