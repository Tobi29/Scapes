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

import java8.util.function.DoubleSupplier;
import org.tobi29.scapes.engine.gui.GuiComponentHeavy;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.gui.GuiRenderer;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.RenderType;
import org.tobi29.scapes.engine.graphics.Model;
import org.tobi29.scapes.engine.graphics.VAOUtility;
import org.tobi29.scapes.engine.graphics.Matrix;
import org.tobi29.scapes.engine.graphics.MatrixStack;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

public class GuiComponentBar extends GuiComponentHeavy {
    private final float r, g, b, a;
    private final double updateFactor;
    private final DoubleSupplier supplier;
    private Model model1, model2;
    private double value;

    public GuiComponentBar(GuiLayoutData parent, float r, float g, float b,
            float a, DoubleSupplier supplier) {
        this(parent, r, g, b, a, 10.0, supplier);
    }

    public GuiComponentBar(GuiLayoutData parent, float r, float g, float b,
            float a, double updateFactor, DoubleSupplier supplier) {
        super(parent);
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.updateFactor = updateFactor;
        this.supplier = supplier;
    }

    @Override
    public void renderComponent(GL gl, Shader shader, Vector2 size,
            Vector2 pixelSize, double delta) {
        double factor = FastMath.min(1.0, delta * updateFactor);
        double newValue = supplier.getAsDouble();
        if (newValue == Double.NEGATIVE_INFINITY) {
            value = 0.0;
        } else if (newValue == Double.POSITIVE_INFINITY) {
            value = 1.0;
        } else {
            value += (FastMath.clamp(newValue, 0.0, 1.0) - value) * factor;
        }
        gl.textures().unbind(gl);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.scale((float) value, 1.0f, 1.0f);
        model1.render(gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate((float) (value * size.doubleX()), 0.0f, 0.0f);
        matrix.scale((float) (1.0 - value), 1.0f, 1.0f);
        model2.render(gl, shader);
        matrixStack.pop();
    }

    @Override
    protected void updateMesh(GuiRenderer renderer, Vector2 size) {
        float r1 = r;
        float g1 = g;
        float b1 = b;
        float r2 = r1 * 0.5f;
        float g2 = g1 * 0.5f;
        float b2 = b1 * 0.5f;
        model1 = VAOUtility.createVCTI(gui.style().engine(),
                new float[]{0.0f, size.floatY(), 0.0f, size.floatX(),
                        size.floatY(), 0.0f, 0.0f, 0.0f, 0.0f, size.floatX(),
                        0.0f, 0.0f},
                new float[]{r2, g2, b2, a, r2, g2, b2, a, r1, g1, b1, a, r1, g1,
                        b1, a},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
        r1 *= 0.4f;
        g1 *= 0.4f;
        b1 *= 0.4f;
        r2 *= 0.4f;
        g2 *= 0.4f;
        b2 *= 0.4f;
        model2 = VAOUtility.createVCTI(gui.style().engine(),
                new float[]{0.0f, size.floatY(), 0.0f, size.floatX(),
                        size.floatY(), 0.0f, 0.0f, 0.0f, 0.0f, size.floatX(),
                        0.0f, 0.0f},
                new float[]{r2, g2, b2, a, r2, g2, b2, a, r1, g1, b1, a, r1, g1,
                        b1, a},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
    }
}
