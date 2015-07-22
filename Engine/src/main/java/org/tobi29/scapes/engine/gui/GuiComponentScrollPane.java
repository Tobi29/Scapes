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

package org.tobi29.scapes.engine.gui;

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiComponentScrollPane extends GuiComponentVisiblePane {
    protected final int scrollStep;
    protected final GuiComponentSliderVert slider;
    private int maxY;
    private double scroll;

    public GuiComponentScrollPane(int x, int y, int width, int height,
            int scrollStep) {
        super(x, y, width, height);
        this.scrollStep = scrollStep;
        slider = new GuiComponentSliderVert(width - 10, 0, 10, height, 0);
        slider.parent = this;
    }

    @Override
    public void removed() {
        super.removed();
        slider.removed();
    }

    @Override
    public void render(GL gl, Shader shader, FontRenderer font, double delta) {
        if (visible) {
            MatrixStack matrixStack = gl.matrixStack();
            Matrix matrix = matrixStack.push();
            transform(matrix);
            renderComponent(gl, shader, font, delta);
            matrix = matrixStack.push();
            matrix.translate(0.0f, (float) -scroll, 0.0f);
            int yy = y;
            GuiComponent other = this;
            while (true) {
                if (other.parent == null) {
                    break;
                }
                other = other.parent;
                yy += other.y;
            }
            gl.enableScissor(0, yy, 800, height);
            components.stream()
                    .filter(component -> component.y() - scroll > -scrollStep &&
                            component.y() - scroll < height).forEach(
                    component -> component.render(gl, shader, font, delta));
            gl.disableScissor();
            matrixStack.pop();
            renderOverlay(gl, shader, font);
            slider.render(gl, shader, font, delta);
            matrixStack.pop();
        }
    }

    @Override
    public void update(double mouseX, double mouseY, boolean mouseInside,
            ScapesEngine engine) {
        super.update(mouseX, mouseY, mouseInside, engine);
        double lScroll = scroll;
        boolean inside = mouseInside && checkInside(mouseX, mouseY);
        if (inside) {
            GuiController guiController = engine.guiController();
            scroll -= guiController.scroll() * scrollStep;
        }
        scroll = FastMath.clamp(scroll, 0,
                Math.max(0, maxY + scrollStep - height));
        if (scroll != lScroll) {
            slider.value = scroll / Math.max(0, maxY + scrollStep - height);
        } else {
            scroll = (int) (slider.value *
                    Math.max(0, maxY + scrollStep - height));
        }
        double mouseXX = mouseX - x;
        double mouseYY = mouseY - y;
        slider.update(mouseXX, mouseYY, inside, engine);
    }

    @Override
    protected void updateChild(GuiComponent component, double mouseX,
            double mouseY, boolean inside, ScapesEngine engine) {
        double y = component.y() - scroll;
        component.update(mouseX, mouseY + scroll,
                inside && y > -scrollStep && y < height, engine);
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
        maxY += scrollStep;
        if (maxY <= 0) {
            slider.setSliderHeight(height);
        } else {
            slider.setSliderHeight(
                    FastMath.min(height * height / maxY, height));
        }
    }
}
