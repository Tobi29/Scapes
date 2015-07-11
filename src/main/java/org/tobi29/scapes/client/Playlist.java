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

package org.tobi29.scapes.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentIcon;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiMessage;
import org.tobi29.scapes.engine.openal.SoundSystem;
import org.tobi29.scapes.engine.utils.io.FileUtil;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Playlist {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(Playlist.class);
    private final SoundSystem sounds;
    private Music currentMusic;
    private double musicWait = 5.0;

    public Playlist(SoundSystem sounds) {
        this.sounds = sounds;
    }

    public void update(MobPlayerClientMain player, double delta) {
        if (!sounds.isMusicPlaying()) {
            if (musicWait >= 0.0) {
                musicWait -= delta;
            } else {
                Music music;
                if (player.getWorld().getEnvironment()
                        .getSunLightReduction(FastMath.floor(player.getX()),
                                FastMath.floor(player.getY())) > 8) {
                    music = Music.NIGHT;
                } else {
                    music = Music.DAY;
                }
                playMusic(music, player);
                Random random = ThreadLocalRandom.current();
                musicWait = random.nextDouble() * 4.0 + 4.0;
            }
        }
    }

    private void playMusic(Music music, MobPlayerClientMain player) {
        currentMusic = music;
        if (sounds.getMusicVolume() <= 0.0) {
            return;
        }
        try {
            Path path = player.getGame().getEngine().home().resolve("playlists")
                            .resolve(music.getName());
            List<Path> titles = new ArrayList<>();
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) {
                    titles.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            if (!titles.isEmpty()) {
                Random random = ThreadLocalRandom.current();
                Path title = titles.get(random.nextInt(titles.size()));
                ScapesEngine engine = player.getGame().getEngine();
                GuiMessage message =
                        new GuiMessage(500, 0, 290, 60, GuiAlignment.RIGHT,
                                3.0);
                message.add(new GuiComponentIcon(10, 10, 40, 40,
                        engine.graphics().getTextureManager()
                                .getTexture("Scapes:image/gui/Playlist")));
                String name = title.getFileName().toString();
                name = name.substring(0, name.lastIndexOf('.'));
                message.add(new GuiComponentText(60, 23, 420, 16, name));
                engine.globalGUI().add(message);
                sounds.playMusic(FileUtil.read(title), 1.0f, 1.0f);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to play music: {}", e.toString());
        }
    }

    public void setMusic(Music music, MobPlayerClientMain player) {
        if (music != currentMusic) {
            playMusic(music, player);
            Random random = ThreadLocalRandom.current();
            musicWait = random.nextDouble() * 20.0 + 4.0;
        }
    }

    public enum Music {
        DAY("day"),
        NIGHT("night"),
        BATTLE("battle");
        private final String name;

        Music(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
