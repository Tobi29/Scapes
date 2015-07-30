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
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.*;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.plugins.PluginFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

public class GuiPlugins extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiPlugins.class);
    private final Path path;
    private final GuiComponentScrollPaneViewport scrollPane;

    @SuppressWarnings("unchecked")
    public GuiPlugins(GameState state, Gui previous) {
        super(state, "Plugins", previous);
        path = state.engine().home().resolve("plugins");
        scrollPane = new GuiComponentScrollPaneList(pane, 16, 80, 368, 290, 70)
                .viewport();
        GuiComponentTextButton add =
                new GuiComponentTextButton(pane, 112, 410, 176, 30, 18, "Add");
        add.addLeftClick(event -> {
            try {
                Path[] imports = state.engine().container().openFileDialog(
                        new Pair[]{new Pair<>("*.jar", "Jar Archive")},
                        "Import plugin", true);
                for (Path copy : imports) {
                    Files.copy(copy, path.resolve(copy.getFileName()));
                }
                updatePlugins();
            } catch (IOException e) {
                LOGGER.warn("Failed to import plugin: {}", e.toString());
            }
        });
        updatePlugins();
    }

    private void updatePlugins() {
        try {
            scrollPane.removeAll();
            for (Path file : Files.newDirectoryStream(path)) {
                if (Files.isRegularFile(file) && !Files.isHidden(file)) {
                    new Element(scrollPane, file);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read plugins: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        private Texture texture;

        public Element(GuiComponent parent, Path path) throws IOException {
            super(parent, 0, 0, 378, 70);
            PluginFile plugin = new PluginFile(path);
            try (ZipFile zip = new ZipFile(path.toFile())) {
                texture = new TextureFile(
                        zip.getInputStream(zip.getEntry("Icon.png")), 0,
                        TextureFilter.LINEAR, TextureFilter.LINEAR,
                        TextureWrap.CLAMP, TextureWrap.CLAMP);
            } catch (IOException e) {
                texture = new TextureCustom(1, 1);
            }
            new GuiComponentTextButton(this, 70, 20, 180, 30, 18,
                    plugin.name());
            GuiComponentTextButton delete =
                    new GuiComponentTextButton(this, 260, 20, 80, 30, 18,
                            "Delete");
            delete.addLeftClick(event -> {
                try {
                    Files.delete(path);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete plugin: {}", e.toString());
                }
            });
            new GuiComponentIcon(this, 15, 15, 40, 40, texture);
        }
    }
}
