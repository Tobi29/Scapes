package org.tobi29.scapes.client.gui.touch;

import java8.util.function.Function;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

public class GuiTouch extends GuiState {
    public GuiTouch(GameState state, GuiStyle style) {
        super(state, style);
    }

    protected GuiComponentGroupSlab row(GuiContainerRow pane) {
        return pane.addVert(102, 0, -1, 80, GuiComponentGroupSlab::new);
    }

    protected <T extends GuiComponent> T row(GuiContainerRow pane,
            Function<GuiLayoutDataVertical, T> component) {
        return pane.addVert(112, 10, -1, 60, component);
    }

    protected <T extends GuiComponent> T rowCenter(GuiContainerRow pane,
            Function<GuiLayoutDataVertical, T> component) {
        return pane.addVert(301, 10, -1, 60, component);
    }

    protected GuiComponentTextButton button(GuiLayoutData parent, String text) {
        return button(parent, 36, text);
    }

    protected GuiComponentTextButton button(GuiLayoutData parent, int textSize,
            String text) {
        return new GuiComponentTextButton(parent, textSize, text);
    }

    protected GuiComponentSlider slider(GuiLayoutData parent, String text,
            double value) {
        return new GuiComponentSlider(parent, 36, text, value);
    }

    protected GuiComponentSlider slider(GuiLayoutData parent, String text,
            double value, GuiComponentSlider.TextFilter filter) {
        return new GuiComponentSlider(parent, 36, text, value, filter);
    }
}
