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
package org.tobi29.scapes.vanilla.basics.gui;

import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.gui.GuiUtils;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

public class GuiComponentHotbar extends GuiComponent {
    private final MobPlayerClientMainVB player;
    private final VAO vao1, vao2, vao3;

    public GuiComponentHotbar(GuiComponent parent, int x, int y, int width,
            int height, MobPlayerClientMainVB player) {
        super(parent, x, y, width, height);
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
    public void renderComponent(GL gl, Shader shader, double delta) {
        MatrixStack matrixStack = gl.matrixStack();
        for (int i = 0; i < 10; i++) {
            if (i == player.inventorySelectLeft()) {
                gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, 0.0f, 0.0f,
                        0.8f);
                Matrix matrix = matrixStack.push();
                matrix.translate(i * (height + 10.0f), 0, 0.0f);
                gl.textures().unbind(gl);
                vao2.render(gl, shader);
                vao3.render(gl, shader);
                org.tobi29.scapes.client.gui.GuiUtils
                        .renderItem(0.0f, 0.0f, height, height,
                                player.inventory("Container").item(i), gl,
                                shader, gui.style().font());
                gl.textures().bind("Scapes:image/gui/HotbarLeft", gl);
                gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f,
                        1.0f);
                vao1.render(gl, shader);
                matrixStack.pop();
            } else if (i == player.inventorySelectRight()) {
                gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, 0.0f, 0.0f,
                        0.8f);
                Matrix matrix = matrixStack.push();
                matrix.translate(i * (height + 10.0f), 0.0f, 0.0f);
                gl.textures().unbind(gl);
                vao2.render(gl, shader);
                vao3.render(gl, shader);
                org.tobi29.scapes.client.gui.GuiUtils
                        .renderItem(0.0f, 0.0f, height, height,
                                player.inventory("Container").item(i), gl,
                                shader, gui.style().font());
                gl.textures().bind("Scapes:image/gui/HotbarRight", gl);
                gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f,
                        1.0f);
                vao1.render(gl, shader);
                matrixStack.pop();
            } else {
                gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, 0.0f, 0.0f,
                        0.6f);
                Matrix matrix = matrixStack.push();
                matrix.translate(i * (height + 10.0f), 0.0f, 0.0f);
                gl.textures().unbind(gl);
                vao2.render(gl, shader);
                vao3.render(gl, shader);
                gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f,
                        1.0f);
                org.tobi29.scapes.client.gui.GuiUtils
                        .renderItem(0.0f, 0.0f, height, height,
                                player.inventory("Container").item(i), gl,
                                shader, gui.style().font());
                matrixStack.pop();
            }
        }
    }
}
