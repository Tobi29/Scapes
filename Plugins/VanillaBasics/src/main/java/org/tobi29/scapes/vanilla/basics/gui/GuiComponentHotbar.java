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

import org.tobi29.scapes.client.gui.GuiUtils;
import org.tobi29.scapes.engine.gui.GuiComponentHeavy;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.gui.GuiRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.RenderType;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.VAOUtility;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2f;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

public class GuiComponentHotbar extends GuiComponentHeavy {
    private final MobPlayerClientMainVB player;
    private VAO vao;

    public GuiComponentHotbar(GuiLayoutData parent,
            MobPlayerClientMainVB player) {
        super(parent);
        this.player = player;
    }

    @Override
    public void renderComponent(GL gl, Shader shader, Vector2 size,
            double delta) {
        MatrixStack matrixStack = gl.matrixStack();
        player.inventories().access("Container", inventory -> {
            for (int i = 0; i < 10; i++) {
                Matrix matrix = matrixStack.push();
                matrix.translate((float) (i * (size.doubleY() + 10.0)), 0.0f,
                        0.0f);
                if (i == player.inventorySelectLeft()) {
                    gl.textures().bind("Scapes:image/gui/HotbarLeft", gl);
                    vao.render(gl, shader);
                } else if (i == player.inventorySelectRight()) {
                    gl.textures().bind("Scapes:image/gui/HotbarRight", gl);
                    vao.render(gl, shader);
                }
                GuiUtils.items(0.0f, 0.0f, size.floatY(), size.floatY(),
                        inventory.item(i), gl, shader, gui.style().font());
                matrixStack.pop();
            }
        });
    }

    @Override
    public void updateMesh(GuiRenderer renderer, Vector2 size) {
        MatrixStack matrixStack = renderer.matrixStack();
        for (int i = 0; i < 10; i++) {
            Matrix matrix = matrixStack.push();
            matrix.translate(i * (size.floatY() + 10.0f), 0.0f, 0.0f);
            gui.style().button(renderer,
                    new Vector2f(size.floatY(), size.floatY()), false);
            matrixStack.pop();
        }
        vao = VAOUtility.createVCTI(gui.style().engine(),
                new float[]{0.0f, size.floatY() - 32.0f, 0.0f, size.floatY(),
                        size.floatY() - 32.0f, 0.0f, 0.0f, -32.0f, 0.0f,
                        size.floatY(), -32.0f, 0.0f},
                new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
    }
}
