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
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiComponentChat extends GuiComponent {
    private final List<ChatLine> meshText = new ArrayList<>();
    private FontRenderer font;

    public GuiComponentChat(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public void addLine(String line) {
        String[] lines = line.split("\n");
        for (String singleLine : lines) {
            if (font != null) {
                FontRenderer.Text vao =
                        font.render(singleLine, 0.0f, 0.0f, 16.0f, 1.0f, 1.0f,
                                1.0f, 1.0f);
                synchronized (meshText) {
                    meshText.add(0, new ChatLine(vao));
                }
            }
        }
    }

    @Override
    public void renderComponent(GraphicsSystem graphics, Shader shader,
            FontRenderer font, double delta) {
        MatrixStack matrixStack = graphics.getMatrixStack();
        if (this.font != font) {
            this.font = font;
        }
        long time = System.currentTimeMillis();
        synchronized (meshText) {
            int yy = -16;
            for (ChatLine line : meshText) {
                Matrix matrix = matrixStack.push();
                matrix.translate(0.0f, yy, 0.0f);
                line.vao.render(graphics, shader);
                matrixStack.pop();
                yy -= 20;
            }
            meshText.removeAll(
                    meshText.stream().filter(line -> time - line.time > 10000)
                            .collect(Collectors.toList()));
        }
    }

    private static class ChatLine {
        private final FontRenderer.Text vao;
        private final long time;

        public ChatLine(FontRenderer.Text vao) {
            this.vao = vao;
            time = System.currentTimeMillis();
        }
    }
}
