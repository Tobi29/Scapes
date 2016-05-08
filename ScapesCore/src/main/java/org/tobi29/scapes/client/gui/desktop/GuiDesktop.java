package org.tobi29.scapes.client.gui.desktop;

import java8.util.function.Function;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

public class GuiDesktop extends GuiState {
    public GuiDesktop(GameState state, GuiStyle style) {
        super(state, style);
    }

    protected GuiComponentGroupSlab row(GuiContainerRow pane) {
        return pane.addVert(11, 0, -1, 40, GuiComponentGroupSlab::new);
    }

    protected <T extends GuiComponent> T row(GuiContainerRow pane,
            Function<GuiLayoutDataVertical, T> component) {
        return pane.addVert(16, 5, -1, 30, component);
    }

    protected <T extends GuiComponent> T rowCenter(GuiContainerRow pane,
            Function<GuiLayoutDataVertical, T> component) {
        return pane.addVert(112, 5, 176, 30, component);
    }

    protected GuiComponentTextButton button(GuiLayoutData parent, String text) {
        return button(parent, 18, text);
    }

    protected GuiComponentTextButton button(GuiLayoutData parent, int textSize,
            String text) {
        return new GuiComponentTextButton(parent, textSize, text);
    }

    protected GuiComponentSlider slider(GuiLayoutData parent, String text,
            double value) {
        return new GuiComponentSlider(parent, 18, text, value);
    }

    protected GuiComponentSlider slider(GuiLayoutData parent, String text,
            double value, GuiComponentSlider.TextFilter filter) {
        return new GuiComponentSlider(parent, 18, text, value, filter);
    }
}
