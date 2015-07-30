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
    private final float r, g, b, a;
    private int i;
    private float[] data;

    public GuiComponentGraph(GuiComponent parent, int x, int y, int width,
            int height, float r, float g, float b, float a) {
        super(parent, x, y, width, height);
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        data = new float[width];
    }

    @Override
    public void renderComponent(GL gl, Shader shader, FontRenderer font,
            double delta) {
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
            color[j++] = r;
            color[j++] = g;
            color[j++] = b;
            color[j] = a;
        }
        int[] index = new int[limit << 1];
        for (int i = 0; i < limit; i++) {
            int j = i << 1;
            index[j++] = i;
            index[j] = i + 1;
        }
        gl.textures().unbind(gl);
        VAO vao = VAOUtility.createVCI(vertex, color, index, RenderType.LINES);
        vao.render(gl, shader);
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
