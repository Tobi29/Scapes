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
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class GuiPlaylists extends GuiMenu {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiPlaylists.class);
    private final List<Element> elements = new ArrayList<>();
    private final GuiComponentScrollPaneViewport scrollPane;
    private final GameState state;
    private String playlist;

    @SuppressWarnings("unchecked")
    public GuiPlaylists(GameState state, Gui previous) {
        super(state, "Playlists", previous);
        this.state = state;
        GuiComponentTextButton day =
                new GuiComponentTextButton(pane, 16, 80, 80, 30, 18, "Day");
        GuiComponentTextButton night =
                new GuiComponentTextButton(pane, 106, 80, 80, 30, 18, "Night");
        GuiComponentTextButton battle =
                new GuiComponentTextButton(pane, 196, 80, 80, 30, 18, "Battle");
        scrollPane = new GuiComponentScrollPaneList(pane, 16, 120, 368, 250, 20)
                .viewport();
        GuiComponentTextButton add =
                new GuiComponentTextButton(pane, 112, 410, 176, 30, 18, "Add");
        updateTitles("day");

        day.addLeftClick(event -> updateTitles("day"));
        night.addLeftClick(event -> updateTitles("night"));
        battle.addLeftClick(event -> updateTitles("battle"));
        add.addLeftClick(event -> {
            try {
                Path directory = state.engine().home().resolve("playlists")
                        .resolve(playlist);
                Path[] imports = state.engine().container().openFileDialog(
                        new Pair[]{new Pair<>("*.*", "All Files"),
                                new Pair<>("*.ogg", "ogg-Vorbis File"),
                                new Pair<>("*.mp3", "MP3 File"),
                                new Pair<>("*.wav", "Wave File")},
                        "Import music", true);
                for (Path copy : imports) {
                    Files.copy(copy, directory.resolve(copy.getFileName()));
                }
                updateTitles(playlist);
            } catch (IOException e) {
                LOGGER.warn("Failed to import music: {}", e.toString());
            }
        });
    }

    private void updateTitles(String playlist) {
        elements.forEach(scrollPane::remove);
        elements.clear();
        this.playlist = playlist;
        try {
            Path path = state.engine().home().resolve("playlists")
                    .resolve(playlist);
            List<Path> titles = new ArrayList<>();
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) {
                    titles.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            for (Path title : titles) {
                String fileName = title.getFileName().toString();
                Element element = new Element(scrollPane,
                        fileName.substring(0, fileName.lastIndexOf('.')),
                        title);
                elements.add(element);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load playlist: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        public Element(GuiComponent parent, String title, Path path) {
            super(parent, 0, 0, 378, 20);
            GuiComponentTextButton play =
                    new GuiComponentTextButton(this, 15, 2, 35, 15, 12, "Play");
            play.addLeftClick(event -> {
                GuiNotification message =
                        new GuiNotification(state.engine().globalGUI(), 500, 0,
                                290, 60, GuiAlignment.RIGHT, 3.0);
                new GuiComponentIcon(message, 10, 10, 40, 40,
                        state.engine().graphics().textures()
                                .get("Scapes:image/gui/Playlist"));
                new GuiComponentText(message, 60, 23, 420, 16, title);
                state.engine().sounds()
                        .playMusic(FileUtil.read(path), 1.0f, 1.0f);
            });
            new GuiComponentTextButton(this, 60, 2, 220, 15, 12, title);
            GuiComponentTextButton delete =
                    new GuiComponentTextButton(this, 290, 2, 60, 15, 12,
                            "Delete");
            delete.addLeftClick(event -> {
                try {
                    Files.delete(path);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete music: {}", e.toString());
                }
            });
        }
    }
}
