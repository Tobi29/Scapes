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
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.graphics.Texture;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;

import java.io.IOException;
import java.util.List;

public class GuiScreenshots extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiScreenshots.class);
    private final GameState state;
    private final GuiComponentScrollPaneViewport scrollPane;

    public GuiScreenshots(GameState state, Gui previous, GuiStyle style) {
        super(state, "Screenshots", previous, style);
        this.state = state;
        scrollPane = pane.addVert(16, 5, 368, 390,
                p -> new GuiComponentScrollPane(p, 70)).viewport();
        try {
            FilePath path = state.engine().home().resolve("screenshots");
            List<FilePath> files =
                    FileUtil.listRecursive(path, FileUtil::isRegularFile,
                            FileUtil::isNotHidden);
            Streams.of(files).sorted().forEach(file -> {
                Element element = scrollPane
                        .addVert(0, 0, -1, 70, p -> new Element(p, file, this));
                state.engine().taskExecutor().runTask(() -> {
                    try {
                        Texture texture = FileUtil.readReturn(file, stream -> {
                            Image image = PNG.decode(stream,
                                    state.engine()::allocate);
                            return state.engine().graphics()
                                    .createTexture(image, 0);
                        });
                        element.icon.setIcon(texture);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to load screenshot: {}",
                                e.toString());
                    }
                }, "Load-Screenshot");
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to read screenshots: {}", e.toString());
        }
    }

    private class Element extends GuiComponentGroupSlabHeavy {
        private final GuiComponentIcon icon;

        @SuppressWarnings("unchecked")
        public Element(GuiLayoutData parent, FilePath path,
                GuiScreenshots gui) {
            super(parent);
            icon = addHori(15, 20, 40, -1, GuiComponentIcon::new);
            GuiComponentTextButton save =
                    addHori(5, 20, -1, -1, p -> button(p, "Save"));
            GuiComponentTextButton delete =
                    addHori(5, 20, 100, -1, p -> button(p, "Delete"));
            selection(save, delete);

            icon.on(GuiEvent.CLICK_LEFT, event -> {
                icon.texture().ifPresent(texture -> state.engine().guiStack()
                        .add("10-Menu", new GuiScreenshot(state, gui, texture,
                                gui.style())));
            });
            save.on(GuiEvent.CLICK_LEFT, event -> {
                try {
                    Optional<FilePath> export = state.engine().container()
                            .saveFileDialog(new Pair[]{
                                            new Pair<>("*.png", "PNG Picture")},
                                    "Export screenshot");
                    if (export.isPresent()) {
                        FileUtil.copy(path, export.get());
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to export screenshot: {}",
                            e.toString());
                }
            });
            delete.on(GuiEvent.CLICK_LEFT, event -> {
                try {
                    FileUtil.delete(path);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete screenshot: {}",
                            e.toString());
                }
            });
        }
    }
}
