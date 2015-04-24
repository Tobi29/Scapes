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
import org.tobi29.scapes.engine.ScapesEngineException;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.*;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;
import org.tobi29.scapes.plugins.PluginFile;

import java.io.IOException;
import java.util.zip.ZipFile;

public class GuiPlugins extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiPlugins.class);
    private final Directory directory;
    private final GuiComponentScrollPaneList scrollPane;

    public GuiPlugins(GameState state, Gui previous) {
        super(state, "Plugins", previous);
        try {
            directory =
                    state.getEngine().getFiles().getDirectory("File:plugins");
        } catch (IOException e) {
            throw new ScapesEngineException("Failed to load plugins", e);
        }
        scrollPane = new GuiComponentScrollPaneList(16, 80, 368, 250, 70);
        GuiComponentTextButton add =
                new GuiComponentTextButton(112, 370, 176, 30, 18, "Add");
        add.addLeftClick(event -> {
            try {
                directory.importFromUser(new PlatformDialogs.Extension[]{
                                new PlatformDialogs.Extension("*.jar",
                                        "Jar Archive")}, "Import plugin", true,
                        state.getEngine().getGraphics().getContainer());
                updatePlugins();
            } catch (IOException e) {
                LOGGER.warn("Failed to import plugin: {}", e.toString());
            }
        });
        pane.add(scrollPane);
        pane.add(add);
        updatePlugins();
    }

    private void updatePlugins() {
        try {
            scrollPane.removeAll();
            for (File plugin : directory.listFilesRecursive(
                    file -> file.getName().endsWith(".jar"))) {
                scrollPane.add(new Element(plugin));
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read plugins: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        private Texture texture;

        public Element(File file) throws IOException {
            super(0, 0, 378, 70);
            PluginFile plugin = new PluginFile(file);
            try (ZipFile zip = file.readZIP()) {
                texture = new TextureFile(
                        zip.getInputStream(zip.getEntry("Icon.png")), 0,
                        TextureFilter.LINEAR, TextureFilter.LINEAR,
                        TextureWrap.CLAMP, TextureWrap.CLAMP);
            } catch (IOException e) {
                texture = new TextureCustom(1, 1);
            }
            GuiComponentTextButton label =
                    new GuiComponentTextButton(70, 20, 170, 30, 18,
                            plugin.getName());
            GuiComponentTextButton delete =
                    new GuiComponentTextButton(250, 20, 100, 30, 18, "Delete");
            delete.addLeftClick(event -> {
                try {
                    file.delete();
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete plugin: {}", e.toString());
                }
            });
            add(new GuiComponentIcon(15, 15, 40, 40, texture));
            add(label);
            add(delete);
        }
    }
}
