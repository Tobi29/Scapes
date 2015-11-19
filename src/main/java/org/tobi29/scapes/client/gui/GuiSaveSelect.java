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
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureCustom;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.basic.BasicWorldSource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuiSaveSelect extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiSaveSelect.class);
    private static final String NO_WORLD_TYPE =
            "No plugin found that that can\n" + "be used to create a save.";
    private final Path path;
    private final SceneMenu scene;
    private final GuiComponentScrollPaneViewport scrollPane;

    public GuiSaveSelect(GameState state, Gui previous, SceneMenu scene,
            GuiStyle style) {
        super(state, "Singleplayer", previous, style);
        this.scene = scene;
        path = state.engine().home().resolve("saves");
        scrollPane = pane.addVert(16, 5,
                p -> new GuiComponentScrollPane(p, 368, 290, 70)).viewport();
        GuiComponentTextButton create = pane.addVert(112, 5,
                p -> new GuiComponentTextButton(p, 176, 30, 18, "Create"));
        create.addLeftClick(event -> {
            try {
                Path path = state.engine().home().resolve("plugins");
                List<Path> files = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files
                        .newDirectoryStream(path)) {
                    for (Path file : stream) {
                        if (Files.isRegularFile(file) &&
                                !Files.isHidden(file)) {
                            files.add(file);
                        }
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
                    state.add(
                            new GuiMessage(state, this, "Error", NO_WORLD_TYPE,
                                    style));
                } else {
                    state.add(
                            new GuiCreateWorld(state, this, worldTypes, plugins,
                                    state.engine().home().resolve("saves"),
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
            List<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files
                    .newDirectoryStream(path)) {
                for (Path file : stream) {
                    if (Files.isDirectory(file) && !Files.isHidden(file)) {
                        files.add(file);
                    }
                }
            }
            files.stream().sorted().forEach(file -> scrollPane
                    .addVert(0, 0, p -> new Element(p, file)));
        } catch (IOException e) {
            LOGGER.warn("Failed to read saves: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        private final Texture texture;

        public Element(GuiLayoutData parent, Path path) {
            super(parent, 378, 70);
            String filename = String.valueOf(path.getFileName());
            GuiComponentTextButton label = add(70, 20,
                    p -> new GuiComponentTextButton(p, 180, 30, 18, filename));
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
            GuiComponentTextButton delete = add(260, 20,
                    p -> new GuiComponentTextButton(p, 80, 30, 18, "Delete"));
            delete.addLeftClick(event -> {
                try {
                    FileUtil.deleteDir(path);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete save: {}", e.toString());
                }
            });
            Texture texture;
            try (WorldSource source = new BasicWorldSource(path)) {
                Optional<Image[]> panorama = source.panorama();
                if (panorama.isPresent()) {
                    Image image = panorama.get()[0];
                    texture = new TextureCustom(image.width(), image.height(),
                            image.buffer(), 4, TextureFilter.LINEAR,
                            TextureFilter.LINEAR, TextureWrap.CLAMP,
                            TextureWrap.CLAMP);
                } else {
                    texture = new TextureCustom(1, 1);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load save icon", e);
                texture = new TextureCustom(1, 1);
            }
            this.texture = texture;
            add(15, 15, p -> new GuiComponentIcon(p, 40, 40, this.texture));
        }
    }
}
