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

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiNotificationSimple;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Playlist {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(Playlist.class);
    private final ScapesEngine engine;
    private Music currentMusic;
    private double musicWait = 5.0;

    public Playlist(ScapesEngine engine) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("scapes.playlist"));
        }
        this.engine = engine;
    }

    public void update(MobPlayerClientMain player, double delta) {
        if (!engine.sounds().isPlaying("music")) {
            if (musicWait >= 0.0) {
                musicWait -= delta;
            } else {
                Music music;
                if (player.world().environment()
                        .sunLightReduction(FastMath.floor(player.x()),
                                FastMath.floor(player.y())) > 8) {
                    music = Music.NIGHT;
                } else {
                    music = Music.DAY;
                }
                playMusic(music);
                Random random = ThreadLocalRandom.current();
                musicWait = random.nextDouble() * 4.0 + 4.0;
            }
        }
    }

    private void playMusic(Music music) {
        currentMusic = music;
        if (engine.config().volume("music") <= 0.0) {
            return;
        }
        Optional<FilePath> title = playMusic(
                engine.home().resolve("playlists").resolve(music.dirName()));
        if (title.isPresent()) {
            String fileName = title.get().getFileName().toString();
            int index = fileName.lastIndexOf('.');
            String name;
            if (index == -1) {
                name = fileName;
            } else {
                name = fileName.substring(0, index);
            }
            engine.notifications().add(p -> new GuiNotificationSimple(p,
                    engine.graphics().textures()
                            .get("Scapes:image/gui/Playlist"), name));
        }
    }

    private Optional<FilePath> playMusic(FilePath path) {
        return AccessController
                .doPrivileged((PrivilegedAction<Optional<FilePath>>) () -> {
                    try {
                        List<FilePath> files = FileUtil.listRecursive(path,
                                FileUtil::isRegularFile, FileUtil::isNotHidden);
                        if (!files.isEmpty()) {
                            Random random = ThreadLocalRandom.current();
                            FilePath title =
                                    files.get(random.nextInt(files.size()));
                            engine.sounds().stop("music");
                            engine.sounds().playMusic(FileUtil.read(title),
                                    "music.Playlist", 1.0f, 1.0f, false);
                            return Optional.of(title);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to play music: {}", e.toString());
                    }
                    return Optional.empty();
                });
    }

    public void setMusic(Music music) {
        if (music != currentMusic) {
            playMusic(music);
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

        public String dirName() {
            return name;
        }
    }
}
