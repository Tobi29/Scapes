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
import org.tobi29.scapes.client.states.GameStateLoadSP;
import org.tobi29.scapes.client.states.scenes.SceneMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.*;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GuiSaveSelect extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiSaveSelect.class);
    private static final String NO_WORLD_TYPE =
            "No plugin found that that can\n" + "be used to create a save.";
    private final SceneMenu scene;
    private final GuiComponentScrollPaneViewport scrollPane;

    public GuiSaveSelect(GameState state, Gui previous, SceneMenu scene) {
        super(state, "Singleplayer", previous);
        this.scene = scene;
        scrollPane = new GuiComponentScrollPaneList(pane, 16, 80, 368, 290, 70)
                .viewport();
        GuiComponentTextButton create =
                new GuiComponentTextButton(pane, 112, 410, 176, 30, 18,
                        "Create");
        create.addLeftClick(event -> {
            try {
                Path path = state.engine().home().resolve("plugins");
                List<Path> files = new ArrayList<>();
                for (Path file : Files.newDirectoryStream(path)) {
                    if (Files.isRegularFile(file) && !Files.isHidden(file)) {
                        files.add(file);
                    }
                }
                List<PluginFile> worldTypes = new ArrayList<>();
                List<PluginFile> plugins = new ArrayList<>();
                for (Path file : files) {
                    PluginFile plugin = new PluginFile(file);
                    if ("WorldType".equals(plugin.parent())) {
                        worldTypes.add(plugin);
                    }
                }
                state.remove(this);
                if (worldTypes.isEmpty()) {
                    state.add(new GuiMessage(state, this, "Error",
                            NO_WORLD_TYPE));
                } else {
                    state.add(
                            new GuiCreateWorld(state, this, worldTypes, plugins,
                                    state.engine().home().resolve("saves")));
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read plugins: {}", e.toString());
            }
        });
        updateSaves();
    }

    public void updateSaves() {
        try {
            Path path = state.engine().home().resolve("saves");
            scrollPane.removeAll();
            for (Path directory : Files.newDirectoryStream(path)) {
                if (Files.isDirectory(directory) &&
                        !Files.isHidden(directory) &&
                        directory.getFileName().toString()
                                .endsWith(WorldFormat.filenameExtension())) {
                    try {
                        new Element(scrollPane, directory);
                    } catch (IOException e) {
                        LOGGER.error("Error reading save info: {}",
                                e.toString());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read saves: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        private final Texture texture;

        public Element(GuiComponent parent, Path path) throws IOException {
            super(parent, 0, 0, 378, 70);
            String filename = path.getFileName().toString();
            GuiComponentTextButton label =
                    new GuiComponentTextButton(this, 70, 20, 180, 30, 18,
                            filename.substring(0, filename.lastIndexOf('.')));
            label.addLeftClick(event -> {
                scene.setSpeed(0.0f);
                state.engine().setState(
                        new GameStateLoadSP(path, state.engine(),
                                state.scene()));
            });
            label.addHover(event -> {
                if (event.state() == GuiComponentHoverEvent.State.ENTER) {
                    scene.changeBackground(path);
                }
            });
            GuiComponentTextButton delete =
                    new GuiComponentTextButton(this, 260, 20, 80, 30, 18,
                            "Delete");
            delete.addLeftClick(event -> {
                try {
                    FileUtil.deleteDir(path);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete save: {}", e.toString());
                }
            });
            Path thumbnailPath = path.resolve("Panorama0.png");
            if (Files.exists(thumbnailPath)) {
                texture = FileUtil.readReturn(thumbnailPath,
                        input -> new TextureFile(input, 0, TextureFilter.LINEAR,
                                TextureFilter.LINEAR, TextureWrap.CLAMP,
                                TextureWrap.CLAMP));
            } else {
                texture = new TextureCustom(1, 1);
            }
            new GuiComponentIcon(this, 15, 15, 40, 40, texture);
        }
    }
}
