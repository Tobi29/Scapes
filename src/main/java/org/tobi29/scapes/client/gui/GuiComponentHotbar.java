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
import org.tobi29.scapes.engine.gui.GuiUtils;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class GuiComponentHotbar extends GuiComponent {
    private final MobPlayerClientMain player;
    private final VAO vao1, vao2, vao3;

    public GuiComponentHotbar(int x, int y, int width, int height,
            MobPlayerClientMain player) {
        super(x, y, width, height);
        this.player = player;
        vao1 = VAOUtility.createVTI(
                new float[]{0.0f, height - 32.0f, 0.0f, height, height - 32.0f,
                        0.0f, 0.0f, -32.0f, 0.0f, height, -32.0f, 0.0f},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
        vao2 = VAOUtility.createVTI(
                new float[]{0.0f, height, 0.0f, height, height, 0.0f, 0.0f,
                        0.0f, 0.0f, height, 0.0f, 0.0f},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
        Mesh mesh = new Mesh(true);
        GuiUtils.renderShadow(mesh, 0.0f, 0.0f, height, height, 0.2f);
        vao3 = mesh.finish();
    }

    @Override
    public void renderComponent(GL gl, Shader shader,
            FontRenderer font, double delta) {
        MatrixStack matrixStack = gl.getMatrixStack();
        OpenGL openGL = gl.getOpenGL();
        for (int i = 0; i < 10; i++) {
            if (i == player.getInventorySelectLeft()) {
                openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, 0.0f, 0.0f,
                        0.8f);
                Matrix matrix = matrixStack.push();
                matrix.translate(i * (height + 10.0f), 0, 0.0f);
                gl.getTextureManager().unbind(gl);
                vao2.render(gl, shader);
                vao3.render(gl, shader);
                org.tobi29.scapes.client.gui.GuiUtils
                        .renderItem(0.0f, 0.0f, height, height,
                                player.getInventory().getItem(i), gl,
                                shader, font);
                gl.getTextureManager().bind("Scapes:image/gui/HotbarLeft", gl);
                openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f,
                        1.0f);
                vao1.render(gl, shader);
                matrixStack.pop();
            } else if (i == player.getInventorySelectRight()) {
                openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, 0.0f, 0.0f,
                        0.8f);
                Matrix matrix = matrixStack.push();
                matrix.translate(i * (height + 10.0f), 0.0f, 0.0f);
                gl.getTextureManager().unbind(gl);
                vao2.render(gl, shader);
                vao3.render(gl, shader);
                org.tobi29.scapes.client.gui.GuiUtils
                        .renderItem(0.0f, 0.0f, height, height,
                                player.getInventory().getItem(i), gl,
                                shader, font);
                gl.getTextureManager().bind("Scapes:image/gui/HotbarRight", gl);
                openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f,
                        1.0f);
                vao1.render(gl, shader);
                matrixStack.pop();
            } else {
                openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, 0.0f, 0.0f,
                        0.6f);
                Matrix matrix = matrixStack.push();
                matrix.translate(i * (height + 10.0f), 0.0f, 0.0f);
                gl.getTextureManager().unbind(gl);
                vao2.render(gl, shader);
                vao3.render(gl, shader);
                openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f,
                        1.0f);
                org.tobi29.scapes.client.gui.GuiUtils
                        .renderItem(0.0f, 0.0f, height, height,
                                player.getInventory().getItem(i), gl,
                                shader, font);
                matrixStack.pop();
            }
        }
    }
}
