package org.tobi29.scapes.client.gui.touch;

import org.tobi29.scapes.client.gui.GuiComponentLogo;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

public class GuiTouchMainMenu extends GuiTouch {
    public GuiTouchMainMenu(GameState state, SceneMenu scene, GuiStyle style) {
        super(state, style);
        GuiComponentPane pane =
                addHori(0, 0, 288, -1, GuiComponentVisiblePane::new);
        GuiComponentGroup space = spacer();
        pane.addVert(0, 10, 5, 20, -1, 160,
                p -> new GuiComponentLogo(p, 160, 36));
        GuiComponentTextButton singlePlayer = space.addVert(10, 10, 320, 80,
                p -> button(p, 48, "Singleplayer"));
        GuiComponentTextButton multiPlayer = space.addVert(10, 10, 320, 80,
                p -> button(p, 48, "Multiplayer"));
        GuiComponentTextButton options =
                space.addVert(10, 10, 320, 80, p -> button(p, 48, "Options"));
        GuiComponentTextButton playlists =
                space.addVert(10, 10, 320, 80, p -> button(p, 48, "Playlists"));

        selection(singlePlayer);
        selection(multiPlayer);
        selection(options);
        selection(playlists);

        singlePlayer.on(GuiEvent.CLICK_LEFT, event -> state.engine().guiStack()
                .swap(this, new GuiTouchSaveSelect(state, this, scene, style)));
        multiPlayer.on(GuiEvent.CLICK_LEFT, event -> state.engine().guiStack()
                .swap(this, new GuiTouchServerSelect(state, this, style)));
        options.on(GuiEvent.CLICK_LEFT, event -> state.engine().guiStack()
                .add("10-Menu", new GuiTouchOptions(state, this, style)));
        playlists.on(GuiEvent.CLICK_LEFT, event -> state.engine().guiStack()
                .add("10-Menu", new GuiTouchPlaylists(state, this, style)));
    }
}