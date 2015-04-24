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
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiSaveSelect extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiSaveSelect.class);
    private static final String NO_WORLD_TYPE =
            "No plugin found that that can\n" + "be used to create a save.";
    private final GuiComponentScrollPaneList scrollPane;

    public GuiSaveSelect(GameState state, Gui previous) {
        super(state, "Singleplayer", previous);
        scrollPane = new GuiComponentScrollPaneList(16, 80, 368, 290, 70);
        GuiComponentTextButton create =
                new GuiComponentTextButton(112, 410, 176, 30, 18, "Create");
        create.addLeftClick(event -> {
            try {
                Directory directory = state.getEngine().getFiles()
                        .getDirectory("File:plugins");
                List<File> files = directory.listFilesRecursive(
                        file -> file.getName().endsWith(".jar"));
                List<PluginFile> worldTypes = new ArrayList<>();
                List<PluginFile> plugins = new ArrayList<>();
                for (File file : files) {
                    PluginFile plugin = new PluginFile(file);
                    if ("WorldType".equals(plugin.getParent())) {
                        worldTypes.add(plugin);
                    }
                }
                state.remove(this);
                if (worldTypes.isEmpty()) {
                    state.add(new GuiMessage(state, this, "Error",
                            NO_WORLD_TYPE));
                } else {
                    state.add(new GuiCreateWorld(state, this, worldTypes,
                            plugins));
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read plugins: {}", e.toString());
            }
        });
        pane.add(scrollPane);
        pane.add(create);
        updateSaves();
    }

    public void updateSaves() {
        try {
            Directory directory =
                    state.getEngine().getFiles().getDirectory("File:saves");
            scrollPane.removeAll();
            for (Directory save : directory.listDirectories(dir -> dir.getName()
                    .endsWith(WorldFormat.getFilenameExtension()))) {
                Element element = null;
                try {
                    element = new Element(save);
                    scrollPane.add(element);
                } catch (IOException e) {
                    LOGGER.error("Error reading save info: {}", e.toString());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read saves: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        private final Texture texture;

        public Element(Directory directory) throws IOException {
            super(0, 0, 378, 70);
            String filename = directory.getName();
            GuiComponentTextButton label =
                    new GuiComponentTextButton(70, 20, 200, 30, 18,
                            filename.substring(0, filename.lastIndexOf('.')));
            label.addLeftClick(event -> state.getEngine().setState(
                    new GameStateLoadSP(directory, state.getEngine(),
                            (SceneMenu) state.getScene())));
            label.addHover(event -> {
                try {
                    ((SceneMenu) state.getScene()).changeBackground(directory);
                } catch (IOException e) {
                }
            });
            GuiComponentTextButton delete =
                    new GuiComponentTextButton(280, 20, 60, 30, 18, "Delete");
            delete.addLeftClick(event -> {
                try {
                    directory.delete();
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete save: {}", e.toString());
                }
            });
            File thumbnailFile = directory.getResource("Panorama0.png");
            if (thumbnailFile.exists()) {
                texture = new TextureFile(thumbnailFile.read(), 0,
                        TextureFilter.LINEAR, TextureFilter.LINEAR,
                        TextureWrap.CLAMP, TextureWrap.CLAMP);
            } else {
                texture = new TextureCustom(1, 1);
            }
            add(new GuiComponentIcon(15, 15, 40, 40, texture));
            add(label);
            add(delete);
        }
    }
}
