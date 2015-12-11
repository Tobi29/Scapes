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
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureFile;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.task.Joiner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GuiScreenshots extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiScreenshots.class);
    private final GameState state;
    private final GuiComponentScrollPaneViewport scrollPane;
    private final Joiner joiner;

    public GuiScreenshots(GameState state, Gui previous, GuiStyle style) {
        super(state, "Screenshots", previous, style);
        this.state = state;
        scrollPane = pane.addVert(16, 5,
                p -> new GuiComponentScrollPane(p, 368, 390, 70)).viewport();
        joiner = state.engine().taskExecutor().runTask(joiner -> {
            try {
                FilePath path = state.engine().home().resolve("screenshots");
                List<FilePath> files =
                        FileUtil.listRecursive(path, FileUtil::isRegularFile,
                                FileUtil::isNotHidden);
                Collections.sort(files);
                for (int i = 0; i < files.size() && !joiner.marked(); i++) {
                    FilePath file = files.get(i);
                    scrollPane.addVert(0, 0, p -> new Element(p, file, this));
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read screenshots: {}", e.toString());
            }
        }, "Load-Screenshots");

        back.onClickLeft(event -> joiner.join());
    }

    private class Element extends GuiComponentPane {
        @SuppressWarnings("unchecked")
        public Element(GuiLayoutData parent, FilePath path,
                GuiScreenshots gui) {
            super(parent, 378, 70);
            Texture textureLoad = state.engine().graphics().textures().empty();
            try {
                textureLoad = FileUtil.readReturn(path,
                        stream -> new TextureFile(stream, 0));
            } catch (IOException e) {
                LOGGER.warn("Failed to load screenshot: {}", e.toString());
            }
            Texture texture = textureLoad;
            GuiComponentIcon icon =
                    add(15, 20, p -> new GuiComponentIcon(p, 40, 30, texture));
            icon.onClickLeft(event -> {
                state.engine().guiStack().add("10-Menu",
                        new GuiScreenshot(state, gui, texture, gui.style()));
            });
            GuiComponentTextButton label =
                    add(70, 20, p -> button(p, 100, "Save"));
            label.onClickLeft(event -> {
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
            GuiComponentTextButton edit =
                    add(180, 20, p -> button(p, 100, "Delete"));
            edit.onClickLeft(event -> {
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