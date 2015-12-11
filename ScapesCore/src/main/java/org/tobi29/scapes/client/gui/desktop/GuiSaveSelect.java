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

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.Debug;
import org.tobi29.scapes.client.SaveStorage;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.states.GameStateLoadSP;
import org.tobi29.scapes.client.states.GameStateLoadSocketSP;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureCustom;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiSaveSelect extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiSaveSelect.class);
    private static final String NO_WORLD_TYPE =
            "No plugin found that that can\n" + "be used to create a save.";
    private final SaveStorage saves;
    private final SceneMenu scene;
    private final GuiComponentScrollPaneViewport scrollPane;

    public GuiSaveSelect(GameState state, Gui previous, SceneMenu scene,
            GuiStyle style) {
        super(state, "Singleplayer", previous, style);
        this.scene = scene;
        ScapesClient game = (ScapesClient) state.engine().game();
        saves = game.saves();
        scrollPane = pane.addVert(16, 5,
                p -> new GuiComponentScrollPane(p, 368, 340, 70)).viewport();
        GuiComponentTextButton create =
                pane.addVert(112, 5, p -> button(p, 176, "Create"));
        create.onClickLeft(event -> {
            try {
                FilePath path = state.engine().home().resolve("plugins");
                List<FilePath> files =
                        FileUtil.listRecursive(path, FileUtil::isRegularFile,
                                FileUtil::isNotHidden);
                List<PluginFile> worldTypes = new ArrayList<>();
                List<PluginFile> plugins = new ArrayList<>();
                for (FilePath file : files) {
                    PluginFile plugin = new PluginFile(file);
                    if ("WorldType".equals(plugin.parent())) {
                        worldTypes.add(plugin);
                    }
                }
                if (worldTypes.isEmpty()) {
                    state.engine().guiStack().add("10-Menu",
                            new GuiMessage(state, this, "Error", NO_WORLD_TYPE,
                                    style));
                } else {
                    state.engine().guiStack().add("10-Menu",
                            new GuiCreateWorld(state, this, worldTypes, plugins,
                                    style));
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read plugins: {}", e.toString());
            }
        });
        updateSaves();
    }

    public void updateSaves() {
        try {
            scrollPane.removeAll();
            saves.list().sorted().forEach(file -> scrollPane
                    .addVert(0, 0, p -> new Element(p, file)));
        } catch (IOException e) {
            LOGGER.warn("Failed to read saves: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        public Element(GuiLayoutData parent, String name) {
            super(parent, 378, 70);
            GuiComponentIcon icon =
                    add(15, 15, p -> new GuiComponentIcon(p, 40, 40));
            GuiComponentTextButton label =
                    add(70, 20, p -> button(p, 180, name));
            GuiComponentTextButton delete =
                    add(260, 20, p -> button(p, 80, "Delete"));

            label.onClickLeft(event -> {
                scene.setSpeed(0.0f);
                try {
                    if (Debug.socketSingleplayer()) {
                        state.engine().setState(
                                new GameStateLoadSocketSP(saves.get(name),
                                        state.engine(), state.scene()));
                    } else {
                        state.engine().setState(
                                new GameStateLoadSP(saves.get(name),
                                        state.engine(), state.scene()));
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to open save: {}", e.toString());
                }
            });
            label.onHover(event -> {
                if (event.state() == GuiComponentHoverEvent.State.ENTER) {
                    try (WorldSource source = saves.get(name)) {
                        scene.changeBackground(source);
                    } catch (IOException e) {
                    }
                }
            });
            delete.onClickLeft(event -> {
                try {
                    saves.delete(name);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete save: {}", e.toString());
                }
            });
            try (WorldSource source = saves.get(name)) {
                Optional<Image[]> panorama = source.panorama();
                if (panorama.isPresent()) {
                    Image image = panorama.get()[0];
                    Texture texture =
                            new TextureCustom(image.width(), image.height(),
                                    image.buffer(), 4, TextureFilter.LINEAR,
                                    TextureFilter.LINEAR, TextureWrap.CLAMP,
                                    TextureWrap.CLAMP);
                    icon.setIcon(texture);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load save icon", e);
            }
        }
    }
}
