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
package org.tobi29.scapes.client.gui.touch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.input.FileType;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;

import java.io.IOException;
import java.util.List;

public class GuiTouchPlaylists extends GuiTouchMenuDouble {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiTouchPlaylists.class);
    private final GuiComponentScrollPaneViewport scrollPane;
    private final GameState state;
    private String playlist;

    public GuiTouchPlaylists(GameState state, Gui previous, GuiStyle style) {
        super(state, "Playlists", "Add", "Back", previous, style);
        this.state = state;
        GuiComponentTextButton day =
                pane.addHori(112, 10, 10, 10, p -> button(p, 232, "Day"));
        GuiComponentTextButton night =
                pane.addHori(10, 10, p -> button(p, 232, "Night"));
        GuiComponentTextButton battle =
                pane.addHori(10, 10, 112, 10, p -> button(p, 232, "Battle"));
        scrollPane = pane.addVert(112, 10,
                p -> new GuiComponentScrollPane(p, 736, 250, 60)).viewport();

        day.onClickLeft(event -> updateTitles("day"));
        night.onClickLeft(event -> updateTitles("night"));
        battle.onClickLeft(event -> updateTitles("battle"));
        save.onClickLeft(event -> {
            try {
                FilePath directory = state.engine().home().resolve("playlists")
                        .resolve(playlist);
                state.engine().container()
                        .openFileDialog(FileType.MUSIC, "Import music", true,
                                (name, input) -> {
                                    FileUtil.write(directory.resolve(name),
                                            output -> ProcessStream
                                                    .process(input,
                                                            output::put));
                                    updateTitles();
                                });
            } catch (IOException e) {
                LOGGER.warn("Failed to import music: {}", e.toString());
            }
        });

        updateTitles("day");
    }

    private void updateTitles() {
        updateTitles(playlist);
    }

    private void updateTitles(String playlist) {
        scrollPane.removeAll();
        this.playlist = playlist;
        try {
            FilePath path = state.engine().home().resolve("playlists")
                    .resolve(playlist);
            List<FilePath> files =
                    FileUtil.listRecursive(path, FileUtil::isRegularFile,
                            FileUtil::isNotHidden);
            Streams.of(files).sorted().forEach(file -> scrollPane
                    .addVert(0, 0, p -> new Element(p, file)));
        } catch (IOException e) {
            LOGGER.warn("Failed to load playlist: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        public Element(GuiLayoutData parent, FilePath path) {
            super(parent, 736, 40);
            String fileName = String.valueOf(path.getFileName());
            int index = fileName.lastIndexOf('.');
            String name;
            if (index == -1) {
                name = fileName;
            } else {
                name = fileName.substring(0, index);
            }
            GuiComponentTextButton label =
                    addHori(10, 5, p -> button(p, 576, 30, 24, name));
            GuiComponentTextButton delete =
                    addHori(10, 5, p -> button(p, 110, 30, 24, "Delete"));

            label.onClickLeft(event -> {
                GuiNotification message = new GuiNotification(660, 0, 290, 60,
                        state.engine().guiStyle(), GuiAlignment.RIGHT, 3.0);
                message.add(10, 10, p -> new GuiComponentIcon(p, 40, 40,
                        state.engine().graphics().textures()
                                .get("Scapes:image/gui/Playlist")));
                message.add(60, 23,
                        p -> new GuiComponentText(p, 420, 16, name));
                state.engine().guiStack().add("90-Notification", message);
                state.engine().sounds().stop("music");
                state.engine().sounds()
                        .playMusic(FileUtil.read(path), "music.Playlist", 1.0f,
                                1.0f, true);
            });
            delete.onClickLeft(event -> {
                try {
                    FileUtil.delete(path);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete music: {}", e.toString());
                }
            });
        }
    }
}
