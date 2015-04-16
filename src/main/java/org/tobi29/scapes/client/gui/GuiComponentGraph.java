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
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiComponentGraph extends GuiComponent {
    private final float r1, g1, b1, a1, r2, g2, b2, a2;
    private int i;
    private float[] data;

    public GuiComponentGraph(int x, int y, int width, int height, float r,
            float g, float b, float a) {
        this(x, y, width, height, r, g, b, a, r * 0.5f, g * 0.5f, b * 0.5f,
                a * 0.5f);
    }

    public GuiComponentGraph(int x, int y, int width, int height, float r1,
            float g1, float b1, float a1, float r2, float g2, float b2,
            float a2) {
        super(x, y, width, height);
        this.r1 = r1;
        this.g1 = g1;
        this.b1 = b1;
        this.a1 = a1;
        this.r2 = r2;
        this.g2 = g2;
        this.b2 = b2;
        this.a2 = a2;
        data = new float[width];
    }

    @Override
    public void renderComponent(GraphicsSystem graphics, Shader shader,
            FontRenderer font, double delta) {
        if (data.length != width) {
            data = new float[width];
        }
        float[] vertex = new float[data.length * 3];
        float[] color = new float[data.length << 2];
        int limit = data.length - 1;
        for (int i = 0; i < data.length; i++) {
            int x = i + this.i;
            if (x >= data.length) {
                x -= data.length;
            }
            x = FastMath.clamp(x, 0, limit);
            int j = i * 3;
            vertex[j++] = i;
            vertex[j++] = data[x] * height;
            vertex[j] = 0.0f;
            j = i << 2;
            color[j++] = r1;
            color[j++] = g1;
            color[j++] = b1;
            color[j] = a1;
        }
        int[] index = new int[limit << 1];
        for (int i = 0; i < limit; i++) {
            int j = i << 1;
            index[j++] = i;
            index[j] = i + 1;
        }
        graphics.getTextureManager().unbind(graphics);
        VAO vao = VAOUtility.createVCI(vertex, color, index, RenderType.LINES);
        vao.render(graphics, shader);
        vao.markAsDisposed();
    }

    public void addStamp(double value) {
        float[] data = this.data;
        if (i < data.length) {
            data[i++] = (float) (1.0 - FastMath.pow(value, 0.25));
        }
        if (i >= data.length) {
            i = 0;
        }
    }
}
