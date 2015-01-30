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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiComponentVisiblePane;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;

import java.io.BufferedReader;
import java.io.IOException;

public class GuiCredits extends Gui {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiCredits.class);
    private final GameState state;
    private final FontRenderer.Text vaoText;
    private double y = -512;

    public GuiCredits(GameState state, Gui prev) {
        super(GuiAlignment.RIGHT);
        this.state = state;
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(704, 0, 96, 512);
        GuiComponentTextButton back =
                new GuiComponentTextButton(13, 64, 70, 30, 18, "Back");
        StringBuilder credits = new StringBuilder(200);
        try (BufferedReader reader = state.getEngine().getFiles()
                .getResource("Scapes:Readme.txt").reader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                credits.append(line).append('\n');
            }
            credits.append(
                    "\n\n\n\n\n\n\n\n\n\nThanks for playing!\n\nHave a nice day!" +
                            "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                            "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                            "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                            "Patience for the win!");
        } catch (IOException e) {
            LOGGER.error("Error reading Readme.txt: {}", e.toString());
        }
        back.addLeftClick(event -> {
            state.getEngine().getSounds().stopMusic();
            state.remove(this);
            state.add(prev);
        });
        pane.add(back);
        add(pane);
        state.getEngine().getSounds()
                .playMusic("Scapes:sound/Credits.ogg", 1.0f, 1.0f);
        vaoText = state.getFont()
                .render(credits.toString(), 20, 0, 18, 1, 1, 1, 1);
    }

    @Override
    public void renderComponent(GraphicsSystem graphics, Shader shader,
            FontRenderer font) {
        y += 40 * state.getEngine().getGraphics().getSync().getSpeedFactor();
        MatrixStack matrixStack = graphics.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(0, (float) -y, 0);
        vaoText.render(graphics, shader);
        matrixStack.pop();
    }
}
