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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.input.FileType;
import org.tobi29.scapes.engine.opengl.texture.*;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.plugins.PluginFile;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;

public class GuiPlugins extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiPlugins.class);
    private final FilePath path;
    private final GuiComponentScrollPaneViewport scrollPane;

    public GuiPlugins(GameState state, Gui previous, GuiStyle style) {
        super(state, "Plugins", previous, style);
        path = state.engine().home().resolve("plugins");
        scrollPane = pane.addVert(16, 5,
                p -> new GuiComponentScrollPane(p, 368, 340, 70)).viewport();
        GuiComponentTextButton add =
                pane.addVert(112, 5, p -> button(p, 176, "Add"));
        add.onClickLeft(event -> {
            try {
                state.engine().container()
                        .openFileDialog(new FileType("*.jar", "Jar Archive"),
                                "Import plugin", true, (name, input) -> FileUtil
                                        .write(path.resolve(name),
                                                output -> ProcessStream
                                                        .process(input,
                                                                output::put)));
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
            List<FilePath> files =
                    FileUtil.listRecursive(path, FileUtil::isRegularFile,
                            FileUtil::isNotHidden);
            Streams.of(files).sorted().forEach(file -> scrollPane
                    .addVert(0, 0, p -> new Element(p, file)));
        } catch (IOException e) {
            LOGGER.warn("Failed to read plugins: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        public Element(GuiLayoutData parent, FilePath path) {
            super(parent, 378, 70);
            GuiComponentIcon icon =
                    add(15, 15, p -> new GuiComponentIcon(p, 40, 40));
            GuiComponentTextButton label =
                    add(70, 20, p -> button(p, 180, "Invalid plugin"));
            GuiComponentTextButton delete =
                    add(260, 20, p -> button(p, 80, "Delete"));

            delete.onClickLeft(event -> {
                try {
                    FileUtil.delete(path);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete plugin: {}", e.toString());
                }
            });
            try {
                PluginFile plugin = new PluginFile(path);
                label.setText(plugin.name());
                try (ZipFile zip = FileUtil.zipFile(path)) {
                    Texture texture = new TextureFile(
                            zip.getInputStream(zip.getEntry("Icon.png")), 0,
                            TextureFilter.LINEAR, TextureFilter.LINEAR,
                            TextureWrap.CLAMP, TextureWrap.CLAMP);
                    icon.setIcon(texture);
                }
            } catch (IOException e) {
            }
        }
    }
}
