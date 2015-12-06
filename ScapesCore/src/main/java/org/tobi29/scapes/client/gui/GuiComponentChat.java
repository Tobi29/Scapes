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

import org.tobi29.scapes.client.ChatHistory;
import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuiComponentChat extends GuiComponent {
    private final ChatHistory chatHistory;
    private Map<String, FontRenderer.Text> cache = new ConcurrentHashMap<>(),
            swapCache = new ConcurrentHashMap<>();

    public GuiComponentChat(GuiLayoutData parent, ChatHistory chatHistory,
            int width, int height) {
        super(parent, width, height);
        this.chatHistory = chatHistory;
    }

    @Override
    public void renderComponent(GL gl, Shader shader, double delta) {
        MatrixStack matrixStack = gl.matrixStack();
        int yy = -16;
        Iterator<String> iterator = chatHistory.lines().iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            FontRenderer.Text vao = cache.get(line);
            if (vao == null) {
                vao = gui.style().font()
                        .render(line, 0.0f, 0.0f, 16.0f, 1.0f, 1.0f, 1.0f,
                                1.0f);
            }
            swapCache.put(line, vao);
            Matrix matrix = matrixStack.push();
            matrix.translate(0.0f, yy, 0.0f);
            vao.render(gl, shader);
            matrixStack.pop();
            yy -= 20;
        }
        Map<String, FontRenderer.Text> oldCache = cache;
        cache = swapCache;
        oldCache.clear();
        swapCache = oldCache;
    }
}
