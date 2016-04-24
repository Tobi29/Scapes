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

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.RenderType;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.VAOUtility;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

public class GuiComponentBar extends GuiComponent {
    private final Supplier supplier;
    private final VAO vao1, vao2;
    private double value;

    public GuiComponentBar(GuiLayoutData parent, int width, int height, float r,
            float g, float b, float a, Supplier supplier) {
        super(parent);
        this.supplier = supplier;
        float r2 = r * 0.5f;
        float g2 = g * 0.5f;
        float b2 = b * 0.5f;
        vao1 = VAOUtility.createVCTI(gui.style().engine(),
                new float[]{0.0f, height, 0.0f, width, height, 0.0f, 0.0f, 0.0f,
                        0.0f, width, 0.0f, 0.0f},
                new float[]{r2, g2, b2, a, r2, g2, b2, a, r, g, b, a, r, g, b,
                        a},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
        r *= 0.4f;
        g *= 0.4f;
        b *= 0.4f;
        r2 *= 0.4f;
        g2 *= 0.4f;
        b2 *= 0.4f;
        vao2 = VAOUtility.createVCTI(gui.style().engine(),
                new float[]{0.0f, height, 0.0f, width, height, 0.0f, 0.0f, 0.0f,
                        0.0f, width, 0.0f, 0.0f},
                new float[]{r2, g2, b2, a, r2, g2, b2, a, r, g, b, a, r, g, b,
                        a},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
    }

    @Override
    public void renderComponent(GL gl, Shader shader, double width,
            double height) {
        gl.textures().unbind(gl);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.scale((float) value, 1.0f, 1.0f);
        vao1.render(gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate((float) (value * width), 0.0f, 0.0f);
        matrix.scale((float) (1.0 - value), 1.0f, 1.0f);
        vao2.render(gl, shader);
        matrixStack.pop();
    }

    @Override
    public void updateComponent(ScapesEngine engine, double delta,
            Vector2 size) {
        double factor = FastMath.min(1.0, delta);
        value += (FastMath.clamp(supplier.get(), 0.0, 1.0) - value) * factor;
    }

    public interface Supplier {
        double get();
    }
}
