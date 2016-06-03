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

import org.tobi29.scapes.engine.gui.GuiComponentHeavy;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.vao.RenderType;
import org.tobi29.scapes.engine.opengl.vao.VAO;
import org.tobi29.scapes.engine.opengl.vao.VAOUtility;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

public class GuiComponentGraph extends GuiComponentHeavy {
    private final float[] r, g, b, a;
    private final int[] i;
    private float[][] data;

    public GuiComponentGraph(GuiLayoutData parent, int graphs, float[] r,
            float[] g, float[] b, float[] a) {
        super(parent);
        assert graphs > 0;
        assert r.length == graphs;
        assert g.length == graphs;
        assert b.length == graphs;
        assert a.length == graphs;
        data = new float[graphs][0];
        i = new int[graphs];
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public void renderComponent(GL gl, Shader shader, Vector2 size,
            double delta) {
        int w = (int) FastMath.ceil(size.doubleX());
        if (data[0].length != w) {
            data = new float[data.length][w];
        }
        float[] vertex = new float[data.length * w * 3];
        float[] color = new float[w * (data.length << 2)];
        int limit = w - 1;
        int[] index = new int[w * (limit << 1)];
        for (int i = 0; i < data.length; i++) {
            int offset = i * w;
            for (int j = 0; j < w; j++) {
                int x = j + this.i[i];
                if (x >= w) {
                    x -= w;
                }
                x = FastMath.clamp(x, 0, limit);
                int k = (offset + j) * 3;
                vertex[k++] = j;
                vertex[k++] = (float) (data[i][x] * size.doubleY());
                vertex[k] = 0.0f;
                k = offset + j << 2;
                color[k++] = r[i];
                color[k++] = g[i];
                color[k++] = b[i];
                color[k] = a[i];
            }
            for (int j = 0; j < limit; j++) {
                int k = offset + j;
                int l = k << 1;
                index[l++] = k;
                index[l] = k + 1;
            }
        }
        gl.textures().unbind(gl);
        VAO vao = VAOUtility
                .createVCI(gui.style().engine(), vertex, color, index,
                        RenderType.LINES);
        vao.render(gl, shader);
        vao.markAsDisposed();
    }

    public void addStamp(double value, int graph) {
        float[] data = this.data[graph];
        if (i[graph] < data.length) {
            data[i[graph]++] = (float) (1.0 - FastMath.pow(value, 0.25));
        }
        if (i[graph] >= data.length) {
            i[graph] = 0;
        }
    }
}
