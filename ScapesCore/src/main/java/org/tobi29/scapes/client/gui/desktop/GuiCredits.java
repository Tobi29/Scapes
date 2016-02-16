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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiComponentVisiblePane;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

import java.io.BufferedReader;
import java.io.IOException;

public class GuiCredits extends GuiDesktop {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiCredits.class);
    private final FontRenderer.Text vaoText;
    private double y = -540;

    public GuiCredits(GameState state, Gui previous, GuiStyle style) {
        super(state, style);
        spacer();
        GuiComponentVisiblePane pane =
                addHori(0, 0, 96, -1, GuiComponentVisiblePane::new);
        GuiComponentTextButton back =
                pane.addVert(13, 64, -1, 30, p -> button(p, "Back"));
        StringBuilder credits = new StringBuilder(200);
        try (BufferedReader reader = state.engine().files()
                .get("Scapes:Readme.txt").reader()) {
            String line = reader.readLine();
            while (line != null) {
                credits.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            LOGGER.error("Error reading Readme.txt: {}", e.toString());
        }
        vaoText =
                style.font().render(credits.toString(), 120, 0, 18, 1, 1, 1, 1);
        back.onClickLeft(event -> {
            state.engine().sounds().stop("music.Credits");
            state.engine().guiStack().add("10-Menu", previous);
        });
        state.engine().sounds().stop("music");
        state.engine().sounds()
                .playMusic("Scapes:sound/Credits.ogg", "music.Credits", 1.0f,
                        1.0f, true);
    }

    @Override
    public void renderComponent(GL gl, Shader shader, double width, double height) {
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(0, (float) -y, 0);
        vaoText.render(gl, shader);
        matrixStack.pop();
    }

    @Override
    public void updateComponent(ScapesEngine engine, double delta,
            Vector2 size) {
        y += 40.0 * delta;
    }
}
