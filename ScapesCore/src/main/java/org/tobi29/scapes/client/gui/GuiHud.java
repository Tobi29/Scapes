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
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiState;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;

public class GuiHud extends GuiState {
    private static final float CROSS_SIZE = 8.0f;
    private static final VAO CROSS = VAOUtility.createVCTI(
            new float[]{-CROSS_SIZE, -CROSS_SIZE, 0.0f, CROSS_SIZE, -CROSS_SIZE,
                    0.0f, CROSS_SIZE, CROSS_SIZE, 0.0f, -CROSS_SIZE, CROSS_SIZE,
                    0.0f},
            new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                    1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
            new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
            new int[]{0, 1, 2, 0, 2, 3}, RenderType.TRIANGLES);

    public GuiHud(GameState state, GuiStyle style) {
        super(state, style, GuiAlignment.LEFT);
    }

    @Override
    public void renderGUI(GL gl, Shader shader, double delta) {
        super.renderGUI(gl, shader, delta);
        if (visible) {
            MatrixStack matrixStack = gl.matrixStack();
            Matrix matrix = matrixStack.push();
            matrix.translate(480.0f, 270.0f, 0.0f);
            float ratio = (float) gl.sceneHeight() / gl.sceneWidth() / 540 *
                            960;
            matrix.scale(ratio, 1.0f, 1.0f);
            gl.textures().bind("Scapes:image/gui/Cross", gl);
            gl.setBlending(BlendingMode.INVERT);
            CROSS.render(gl, shader);
            gl.setBlending(BlendingMode.NORMAL);
            matrixStack.pop();
        }
    }
}