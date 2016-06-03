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
import org.tobi29.scapes.engine.gui.GuiRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.vao.Mesh;
import org.tobi29.scapes.engine.opengl.vao.VAO;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

public class GuiComponentBusy extends GuiComponentHeavy {
    private float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;
    private VAO vao;
    private double value;

    public GuiComponentBusy(GuiLayoutData parent) {
        super(parent);
    }

    public void setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        dirty();
    }

    @Override
    public void renderComponent(GL gl, Shader shader, Vector2 size,
            double delta) {
        value += delta * 300.0;
        while (value > 180.0) {
            value -= 360.0;
        }
        gl.textures().unbind(gl);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(size.floatX() * 0.5f, size.floatY() * 0.5f, 0.0f);
        matrix.rotate((float) value, 0.0f, 0.0f, 1.0f);
        vao.render(gl, shader);
        matrixStack.pop();
    }

    @Override
    protected void updateMesh(GuiRenderer renderer, Vector2 size) {
        Mesh mesh = new Mesh();
        double w2 = size.doubleX() * 0.5;
        double h2 = size.doubleY() * 0.5;
        double w3 = FastMath.max(w2 - 3.0, 0.0);
        double h3 = FastMath.max(h2 - 3.0, 0.0);
        Vector2 pixelSize = renderer.pixelSize();
        double w1 = w2 + pixelSize.doubleX();
        double h1 = h2 + pixelSize.doubleY();
        double w4 = w3 - pixelSize.doubleX();
        double h4 = h3 - pixelSize.doubleY();
        int section = 5;
        renderPart(mesh, 40, 140, section, w1, h1, w2, h2, w3, h3, w4, h4);
        renderPart(mesh, 220, 320, section, w1, h1, w2, h2, w3, h3, w4, h4);
        vao = mesh.finish(gui.style().engine());
    }

    private void renderPart(Mesh mesh, int start, int end, int section,
            double w1, double h1, double w2, double h2, double w3, double h3,
            double w4, double h4) {
        double cos = FastMath.cos(start * FastMath.DEG_2_RAD);
        double sin = FastMath.sin(start * FastMath.DEG_2_RAD);
        mesh.color(r, g, b, 0.0f);
        for (int dir = start + section; dir <= end; dir += section) {
            double ncos = FastMath.cos(dir * FastMath.DEG_2_RAD);
            double nsin = FastMath.sin(dir * FastMath.DEG_2_RAD);
            mesh.vertex((float) (ncos * w1), (float) (nsin * h1), 0.0f);
            mesh.vertex((float) (cos * w1), (float) (sin * h1), 0.0f);
            mesh.color(r, g, b, a);
            mesh.vertex((float) (cos * w2), (float) (sin * h2), 0.0f);
            mesh.vertex((float) (ncos * w2), (float) (nsin * h2), 0.0f);

            mesh.vertex((float) (ncos * w2), (float) (nsin * h2), 0.0f);
            mesh.vertex((float) (cos * w2), (float) (sin * h2), 0.0f);
            mesh.vertex((float) (cos * w3), (float) (sin * h3), 0.0f);
            mesh.vertex((float) (ncos * w3), (float) (nsin * h3), 0.0f);

            mesh.vertex((float) (ncos * w3), (float) (nsin * h3), 0.0f);
            mesh.vertex((float) (cos * w3), (float) (sin * h3), 0.0f);
            mesh.color(r, g, b, 0.0f);
            mesh.vertex((float) (cos * w4), (float) (sin * h4), 0.0f);
            mesh.vertex((float) (ncos * w4), (float) (nsin * h4), 0.0f);
            cos = ncos;
            sin = nsin;
        }
    }
}
