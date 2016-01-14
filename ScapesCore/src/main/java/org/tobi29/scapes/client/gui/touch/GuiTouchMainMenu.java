package org.tobi29.scapes.client.gui.touch;

import org.tobi29.scapes.client.gui.GuiComponentLogo;
import org.tobi29.scapes.client.gui.desktop.GuiServerSelect;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

public class GuiTouchMainMenu extends GuiTouch {
    public GuiTouchMainMenu(GameState state, SceneMenu scene, GuiStyle style) {
        super(state, style);
        GuiComponentPane pane =
                addHori(0, 0, 288, -1, GuiComponentVisiblePane::new);
        GuiComponentPane space = spacer();
        pane.addVert(0, 10, 5, 20, -1, 160,
                p -> new GuiComponentLogo(p, 160, 36));
        GuiComponentTextButton singlePlayer = space.addVert(176, 10, -1, 80,
                p -> button(p, 48, "Singleplayer"));
        GuiComponentTextButton multiPlayer = space.addVert(176, 10, -1, 80,
                p -> button(p, 48, "Multiplayer"));
        GuiComponentTextButton options =
                space.addVert(176, 10, -1, 80, p -> button(p, 48, "Options"));
        GuiComponentTextButton playlists =
                space.addVert(176, 10, -1, 80, p -> button(p, 48, "Playlists"));

        singlePlayer.onClickLeft(event -> {
            state.engine().guiStack().add("10-Menu",
                    new GuiTouchSaveSelect(state, this, scene, style));
        });
        multiPlayer.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiServerSelect(state, this, style));
        });
        options.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiTouchOptions(state, this, style));
        });
        playlists.onClickLeft(event -> {
            state.engine().guiStack()
                    .add("10-Menu", new GuiTouchPlaylists(state, this, style));
        });
    }
}