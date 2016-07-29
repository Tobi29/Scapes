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
package org.tobi29.scapes.client.gui.desktop;

import org.tobi29.scapes.engine.gui.GuiComponentHeavy;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.gui.GuiRenderer;
import org.tobi29.scapes.engine.graphics.FontRenderer;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Matrix;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

public class GuiComponentCredits extends GuiComponentHeavy {
    private final String text;
    private float y = 160.0f;

    public GuiComponentCredits(GuiLayoutData parent, String text) {
        super(parent);
        this.text = text;
    }

    @Override
    protected void updateMesh(GuiRenderer renderer, Vector2 size) {
        FontRenderer font = gui.style().font();
        font.render(FontRenderer.to(renderer, 1.0f, 1.0f, 1.0f, 1.0f), text,
                size.floatY(), size.floatX());
    }

    @Override
    protected void transform(Matrix matrix, Vector2 size) {
        matrix.translate(0, y, 0);
    }

    @Override
    public void renderComponent(GL gl, Shader shader, Vector2 size,
            double delta) {
        y -= 40.0f * delta;
    }
}
