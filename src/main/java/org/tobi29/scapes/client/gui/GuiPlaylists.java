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
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentPane;
import org.tobi29.scapes.engine.gui.GuiComponentScrollPaneList;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.FileUtil;

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
    private final GuiComponentScrollPaneList scrollPane;
    private final GameState state;
    private String playlist;

    @SuppressWarnings("unchecked")
    public GuiPlaylists(GameState state, Gui previous) {
        super(state, "Playlists", previous);
        this.state = state;
        GuiComponentTextButton day =
                new GuiComponentTextButton(16, 80, 80, 30, 18, "Day");
        day.addLeftClick(event -> updateTitles("day"));
        GuiComponentTextButton night =
                new GuiComponentTextButton(106, 80, 80, 30, 18, "Night");
        night.addLeftClick(event -> updateTitles("night"));
        GuiComponentTextButton battle =
                new GuiComponentTextButton(196, 80, 80, 30, 18, "Battle");
        battle.addLeftClick(event -> updateTitles("battle"));
        scrollPane = new GuiComponentScrollPaneList(16, 120, 368, 210, 20);
        GuiComponentTextButton add =
                new GuiComponentTextButton(112, 370, 176, 30, 18, "Add");
        add.addLeftClick(event -> {
            try {
                Path directory =
                        state.getEngine().getHome().resolve("playlists")
                                .resolve(playlist);
                state.getEngine().getGraphics().getContainer()
                        .importFromUser(directory,
                                new Pair[]{new Pair<>("*.*", "All Files"),
                                        new Pair<>("*.ogg", "ogg-Vorbis File"),
                                        new Pair<>("*.mp3", "MP3 File"),
                                        new Pair<>("*.wav", "Wave File")},
                                "Import music", true);
                updateTitles(playlist);
            } catch (IOException e) {
                LOGGER.warn("Failed to import music: {}", e.toString());
            }
        });
        pane.add(day);
        pane.add(night);
        pane.add(battle);
        pane.add(scrollPane);
        pane.add(add);
        updateTitles("day");
    }

    private void updateTitles(String playlist) {
        elements.forEach(scrollPane::remove);
        elements.clear();
        this.playlist = playlist;
        try {
            Path path = state.getEngine().getHome().resolve("playlists")
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
                Element element = new Element(
                        fileName.substring(0, fileName.lastIndexOf('.')),
                        title);
                elements.add(element);
                scrollPane.add(element);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load playlist: {}", e.toString());
        }
    }

    private class Element extends GuiComponentPane {
        public Element(String title, Path path) {
            super(0, 0, 378, 20);
            GuiComponentTextButton play =
                    new GuiComponentTextButton(20, 2, 30, 15, 12, "Play");
            play.addLeftClick(event -> state.getEngine().getSounds()
                    .playMusic(FileUtil.read(path), 1.0f, 1.0f));
            GuiComponentTextButton label =
                    new GuiComponentTextButton(60, 2, 220, 15, 12, title);
            GuiComponentTextButton delete =
                    new GuiComponentTextButton(290, 2, 60, 15, 12, "Delete");
            delete.addLeftClick(event -> {
                try {
                    Files.delete(path);
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete music: {}", e.toString());
                }
            });
            add(play);
            add(label);
            add(delete);
        }
    }
}
