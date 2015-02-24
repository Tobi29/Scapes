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
import org.tobi29.scapes.engine.opengl.BlendingMode;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.Arrays;

public class GuiComponentTextField extends GuiComponentTextButton {
    private final boolean major;
    private final int maxLength;
    private final GuiController.TextFieldData data;
    private boolean cursor;
    private FontRenderer.Text vaoCursor, vaoSelection;

    public GuiComponentTextField(int x, int y, int width, int height,
            int textSize, String defaultText) {
        this(x, y, width, height, textSize, defaultText, Integer.MAX_VALUE,
                false);
    }

    public GuiComponentTextField(int x, int y, int width, int height,
            int textSize, String defaultText, boolean hiddenText) {
        this(x, y, width, height, textSize, defaultText, Integer.MAX_VALUE,
                hiddenText, false);
    }

    public GuiComponentTextField(int x, int y, int width, int height,
            int textSize, String defaultText, boolean hiddenText,
            boolean major) {
        this(x, y, width, height, textSize, defaultText, Integer.MAX_VALUE,
                hiddenText, major);
    }

    public GuiComponentTextField(int x, int y, int width, int height,
            int textSize, String defaultText, int maxLength) {
        this(x, y, width, height, textSize, defaultText, maxLength, false);
    }

    public GuiComponentTextField(int x, int y, int width, int height,
            int textSize, String defaultText, int maxLength,
            boolean hiddenText) {
        this(x, y, width, height, textSize, defaultText, maxLength, hiddenText,
                false);
    }

    public GuiComponentTextField(int x, int y, int width, int height,
            int textSize, String defaultText, int maxLength, boolean hiddenText,
            boolean major) {
        super(x, y, width, height, textSize, defaultText);
        this.major = major;
        this.maxLength = maxLength;
        data = new GuiController.TextFieldData(defaultText,
                defaultText.length(), -1, 0);
        if (hiddenText) {
            setTextFilter(text -> {
                char[] array = new char[text.length()];
                Arrays.fill(array, '*');
                return new String(array);
            });
        } else {
            setTextFilter(text -> text);
        }
    }

    @Override
    public void update(double mouseX, double mouseY, boolean mouseInside,
            ScapesEngine engine) {
        super.update(mouseX, mouseY, mouseInside, engine);
        getGui().ifPresent(gui -> {
            if (gui.getLastClicked() == this || major) {
                if (engine.getGuiController().processTextField(data, false)) {
                    if (data.text.length() > maxLength) {
                        data.text.delete(maxLength, data.text.length());
                        data.cursor = FastMath.min(data.cursor, maxLength);
                    }
                    setText(data.text.toString());
                }
            } else {
                data.selectionStart = -1;
                data.cursor = data.text.length();
            }
        });
    }

    @Override
    public void renderComponent(GraphicsSystem graphics, Shader shader,
            FontRenderer font, double delta) {
        super.renderComponent(graphics, shader, font, delta);
        if (cursor) {
            vaoCursor.render(graphics, shader);
        }
        graphics.getTextureManager().unbind(graphics);
        OpenGL openGL = graphics.getOpenGL();
        openGL.setBlending(BlendingMode.INVERT);
        vaoSelection.render(graphics, shader, false);
        openGL.setBlending(BlendingMode.NORMAL);
        getGui().ifPresent(gui -> {
            if (gui.getLastClicked() == this || major) {
                cursor = System.currentTimeMillis() / 600 % 2 == 0;
            } else if (cursor) {
                cursor = false;
            }
        });
    }

    @Override
    protected void updateText(String text) {
        super.updateText(text);
        if (vaoText != null) {
            int maxLengthFont = vaoText.getLength();
            if (data.text.length() > maxLengthFont) {
                data.text = data.text.delete(maxLengthFont, data.text.length());
                data.cursor = FastMath.min(data.cursor, maxLengthFont);
            }
        }
        if (font != null) {
            vaoCursor = font.render(text.substring(0, data.cursor) + '|',
                    textX - textSize * 0.1f, textY - textSize * 0.1f, textSize,
                    textSize * 1.2f, textSize, Float.MAX_VALUE, 1.0f, 1.0f,
                    1.0f, 1.0f, data.cursor, data.cursor + 1, false);
            vaoSelection = font.render(text, textX, textY, textSize, textSize,
                    textSize, width - textX, 1.0f, 1.0f, 1.0f, 1.0f,
                    data.selectionStart, data.selectionEnd, true);
        }
    }
}
