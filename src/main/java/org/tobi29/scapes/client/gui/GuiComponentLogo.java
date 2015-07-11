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
import org.tobi29.scapes.engine.gui.GuiComponentEvent;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.shader.Shader;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GuiComponentLogo extends GuiComponent {
    private static final String[] SPLASHES =
            {"Hi!", "Shut up!", "<3", ";)", "9001", "Minecraft",
                    "Please\nstand back!", "You\nsuck!", "Crap!",
                    "Holy\nsheet!", "<- Pickaxe", "Whatever", "Minceraft",
                    "OpenGL", "OpenAL", "Sepacs", "<Insert bad\npun here>",
                    "Orange\nSmoke\nBlue\nCup", "Prepare!", "Hm...",
                    "Fatal\nerror!", "java.util.\nRandom\nfor ya",
                    "Java:\n" + System.getProperty("java.version"),
                    "Hello,\n" + System.getProperty("user.name")};
    private final int textX, textY;
    private final VAO vao;
    private String text;
    private FontRenderer.Text vaoScapes, vaoSplash;
    private FontRenderer font;

    public GuiComponentLogo(int x, int y, int width, int height) {
        super(x, y, width, height);
        text = getSplash();
        textX = x + height - 8;
        textY = y + 4;
        vao = VAOUtility.createVTI(
                new float[]{x, y + height, 0.0f, x + height, y + height, 0.0f,
                        x, y, 0.0f, x + height, y, 0.0f},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
    }

    private static String getSplash() {
        String text;
        LocalDate date = LocalDate.now();
        if (date.getDayOfMonth() == 1 && date.getMonth() == Month.APRIL) {
            text = "COMIC\nSANS!!!";
        } else if (date.getDayOfMonth() == 29 &&
                date.getMonth() == Month.FEBRUARY) {
            text = "Best day\never!";
        } else if (date.getDayOfMonth() == 24 &&
                date.getMonth() == Month.DECEMBER) {
            text = "Merry\nChristmas!";
        } else if (date.getDayOfMonth() == 1 &&
                date.getMonth() == Month.JANUARY) {
            text = "Happy new\nyear!";
        } else if (date.getDayOfMonth() == 3 &&
                date.getMonth() == Month.MARCH) {
            char[] array = new char[1024];
            Arrays.fill(array, ' ');
            text = new String(array) + "Aspect ratio!?!";
        } else {
            Random random = ThreadLocalRandom.current();
            text = SPLASHES[random.nextInt(SPLASHES.length)];
        }
        return text;
    }

    @Override
    public void clickLeft(GuiComponentEvent event, ScapesEngine engine) {
        engine.sounds().playSound("Engine:sound/Click.ogg", 1.0f, 1.0f);
        super.clickLeft(event, engine);
        text = getSplash();
        updateText();
    }

    @Override
    public void renderComponent(GL gl, Shader shader,
            FontRenderer font, double delta) {
        gl.getTextureManager().bind("Scapes:image/Icon", gl);
        OpenGL openGL = gl.getOpenGL();
        openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f, 1.0f);
        vao.render(gl, shader);
        if (this.font != font) {
            this.font = font;
            updateText();
        }
        vaoScapes.render(gl, shader);
        vaoSplash.render(gl, shader);
    }

    private void updateText() {
        if (font != null) {
            vaoScapes =
                    font.render("Scapes", textX, textY, 18, 1.0f, 1.0f, 1.0f,
                            1);
            vaoSplash =
                    font.render(text, textX, textY + 22, 12, 1.0f, 1.0f, 0.0f,
                            1);
        }
    }
}
