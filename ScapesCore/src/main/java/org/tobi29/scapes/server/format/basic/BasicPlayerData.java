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
package org.tobi29.scapes.server.format.basic;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.PlayerEntry;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.format.PlayerData;
import org.tobi29.scapes.server.format.PlayerStatistics;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class BasicPlayerData implements PlayerData {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(BasicPlayerData.class);
    private final FilePath path;

    public BasicPlayerData(FilePath path) throws IOException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                    new RuntimePermission("scapes.playerData"));
        }
        this.path = path;
        FileUtil.createDirectories(path);
    }

    @Override
    public synchronized PlayerEntry player(String id) {
        TagStructure tagStructure = load(id);
        return new PlayerEntry() {
            @Override
            public Optional<MobPlayerServer> createEntity(
                    PlayerConnection player, Optional<WorldServer> spawnWorld) {
                ScapesServer server = player.server().server();
                if (!spawnWorld.isPresent()) {
                    spawnWorld = server.world(tagStructure.getString("World"));
                }
                if (!spawnWorld.isPresent()) {
                    spawnWorld = server.defaultWorld();
                }
                if (!spawnWorld.isPresent()) {
                    return Optional.empty();
                }
                WorldServer world = spawnWorld.get();
                MobPlayerServer entity = world.plugins().worldType()
                        .newPlayer(world,
                                new Vector3d(0.5, 0.5, 1.0).plus(world.spawn()),
                                Vector3d.ZERO, 0.0, 0.0, player.nickname(),
                                player.skin().checksum(), player);
                Optional<TagStructure> entityTag;
                if (tagStructure.has("Entity")) {
                    entityTag =
                            Optional.of(tagStructure.getStructure("Entity"));
                } else {
                    entityTag = Optional.empty();
                }
                if (entityTag.isPresent()) {
                    entity.read(entityTag.get());
                } else {
                    entity.onSpawn();
                }
                return Optional.of(entity);
            }

            @Override
            public int permissions() {
                return tagStructure.getInteger("Permissions");
            }

            @Override
            public PlayerStatistics statistics(GameRegistry registry) {
                PlayerStatistics statistics = new PlayerStatistics();
                statistics.load(registry, tagStructure.getList("Statistics"));
                return statistics;
            }
        };
    }

    @Override
    public synchronized void save(String id, MobPlayerServer entity,
            int permissions, PlayerStatistics statistics) {
        WorldServer world = entity.world();
        TagStructure tagStructure = new TagStructure();
        tagStructure.setStructure("Entity", entity.write(false));
        tagStructure.setString("World", world.id());
        tagStructure.setList("Statistics", statistics.save());
        tagStructure.setInteger("Permissions", permissions);
        save(tagStructure, id);
    }

    @Override
    public synchronized void add(String id) {
        save(load(id), id);
    }

    @SuppressWarnings({"ReturnOfNull", "CallToNativeMethodWhileLocked"})
    @Override
    public synchronized void remove(String id) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            try {
                FileUtil.deleteIfExists(path.resolve(id + ".stag"));
            } catch (IOException e) {
                LOGGER.error("Error writing player data: {}", e.toString());
            }
            return null;
        });
    }

    @Override
    public boolean playerExists(String id) {
        return AccessController.doPrivileged(
                (PrivilegedAction<Boolean>) () -> FileUtil
                        .exists(path.resolve(id + ".stag")));
    }

    @SuppressWarnings("CallToNativeMethodWhileLocked")
    private synchronized TagStructure load(String id) {
        return AccessController
                .doPrivileged((PrivilegedAction<TagStructure>) () -> {
                    try {
                        FilePath file = path.resolve(id + ".stag");
                        if (FileUtil.exists(file)) {
                            return FileUtil
                                    .readReturn(file, TagStructureBinary::read);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error reading player data: {}",
                                e.toString());
                    }
                    return new TagStructure();
                });
    }

    @SuppressWarnings({"ReturnOfNull", "CallToNativeMethodWhileLocked"})
    private synchronized void save(TagStructure tagStructure, String id) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            try {
                FilePath file = path.resolve(id + ".stag");
                FileUtil.write(file, stream -> TagStructureBinary
                        .write(tagStructure, stream));
            } catch (IOException e) {
                LOGGER.error("Error writing player data: {}", e.toString());
            }
            return null;
        });
    }
}
