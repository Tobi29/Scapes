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

import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class GuiHud extends Gui {
    public final GuiComponentCross cross;

    public GuiHud(MobPlayerClientMain player) {
        super(GuiAlignment.LEFT);
        add(new GuiComponentHotbar(8, 464, 560, 40, player));
        add(new GuiComponentBar(8, 426, 280, 16, 1.0f, 0.0f, 0.0f, 0.6f,
                () -> player.getLives() / player.getMaxLives()));
        cross = new GuiComponentCross(392, 248, 16, 16);
    }

    @Override
    public void render(GL gl, Shader shader,
            FontRenderer font, double delta) {
        if (visible) {
            super.render(gl, shader, font, delta);
            MatrixStack matrixStack = gl.getMatrixStack();
            Matrix matrix = matrixStack.push();
            float ratio =
                    (float) gl.getSceneHeight() / gl.getSceneWidth() * 1.5625f;
            matrix.scale(ratio, 1.0f, 1.0f);
            matrix.translate(-400.0f + 400.0f / ratio, 0.0f, 0.0f);
            cross.render(gl, shader, font, delta);
            matrixStack.pop();
        }
    }

    @Override
    public void removed() {
        super.removed();
        cross.removed();
    }
}
