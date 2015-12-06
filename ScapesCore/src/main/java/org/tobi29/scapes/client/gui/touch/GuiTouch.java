package org.tobi29.scapes.client.gui.touch;

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

public class GuiTouch extends GuiState {
    public GuiTouch(GameState state, GuiStyle style, GuiAlignment alignment) {
        super(state, style, alignment);
    }

    protected GuiComponentTextButton button(GuiLayoutData parent, int width,
            String text) {
        return button(parent, width, 60, 36, text);
    }

    protected GuiComponentTextButton button(GuiLayoutData parent, int width,
            int height, int textSize, String text) {
        return new GuiComponentTextButton(parent, width, height, textSize,
                text);
    }
}
