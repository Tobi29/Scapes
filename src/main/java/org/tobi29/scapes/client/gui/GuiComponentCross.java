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

import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.shader.Shader;

public class GuiComponentCross extends GuiComponent {
    private final VAO vao;

    public GuiComponentCross(int x, int y, int width, int height) {
        super(x, y, width, height);
        vao = VAOUtility.createVCTI(
                new float[]{0.0f, height, 0.0f, width, height, 0.0f, 0.0f, 0.0f,
                        0.0f, width, 0.0f, 0.0f},
                new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
    }

    @Override
    public void renderComponent(GL gl, Shader shader, FontRenderer font,
            double delta) {
        gl.textures().bind("Scapes:image/gui/Cross", gl);
        gl.setBlending(BlendingMode.INVERT);
        vao.render(gl, shader);
        gl.setBlending(BlendingMode.NORMAL);
    }
}
