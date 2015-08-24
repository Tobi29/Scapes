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

import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;

public class GuiHud extends Gui {
    private static final float CROSS_SIZE = 8.0f;
    private static final VAO CROSS = VAOUtility.createVCTI(
            new float[]{-CROSS_SIZE, -CROSS_SIZE, 0.0f, CROSS_SIZE, -CROSS_SIZE,
                    0.0f, CROSS_SIZE, CROSS_SIZE, 0.0f, -CROSS_SIZE, CROSS_SIZE,
                    0.0f},
            new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                    1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
            new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
            new int[]{0, 1, 2, 0, 2, 3}, RenderType.TRIANGLES);

    public GuiHud() {
        super(GuiAlignment.LEFT);
    }

    @Override
    public void render(GL gl, Shader shader, FontRenderer font, double delta) {
        super.render(gl, shader, font, delta);
        if (visible) {
            MatrixStack matrixStack = gl.matrixStack();
            Matrix matrix = matrixStack.push();
            matrix.translate(400.0f, 256.0f, 0.0f);
            float ratio = (float) gl.sceneHeight() / gl.sceneWidth() * 1.5625f;
            matrix.scale(ratio, 1.0f, 1.0f);
            gl.textures().bind("Scapes:image/gui/Cross", gl);
            gl.setBlending(BlendingMode.INVERT);
            CROSS.render(gl, shader);
            gl.setBlending(BlendingMode.NORMAL);
            matrixStack.pop();
        }
    }
}
