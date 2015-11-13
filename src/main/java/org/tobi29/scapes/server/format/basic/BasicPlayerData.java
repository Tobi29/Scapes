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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.format.PlayerData;
import org.tobi29.scapes.server.format.PlayerStatistics;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class BasicPlayerData implements PlayerData {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(BasicPlayerData.class);
    private final Path path;

    public BasicPlayerData(Path path) throws IOException {
        this.path = path;
        Files.createDirectories(path);
    }

    @Override
    public synchronized PlayerData.Player player(String id) {
        TagStructure tagStructure = load(id);
        return new PlayerData.Player() {
            @Override
            public MobPlayerServer createEntity(PlayerConnection player,
                    Optional<WorldServer> overrideWorld) {
                Optional<TagStructure> entityTag;
                if (tagStructure.has("Entity")) {
                    entityTag =
                            Optional.of(tagStructure.getStructure("Entity"));
                } else {
                    entityTag = Optional.empty();
                }
                Optional<String> worldTag =
                        Optional.ofNullable(tagStructure.getString("World"));
                WorldFormat worldFormat =
                        player.server().server().worldFormat();
                WorldServer world = overrideWorld.orElseGet(() -> {
                    if (worldTag.isPresent()) {
                        return worldFormat.world(worldTag.get())
                                .orElseGet(worldFormat::defaultWorld);
                    } else {
                        return worldFormat.defaultWorld();
                    }
                });
                MobPlayerServer entity = world.plugins().worldType()
                        .newPlayer(world,
                                new Vector3d(0.5, 0.5, 1.0).plus(world.spawn()),
                                Vector3d.ZERO, 0.0, 0.0, player.nickname(),
                                player.skin().checksum(), player);
                if (entityTag.isPresent()) {
                    entity.read(entityTag.get());
                } else {
                    entity.onSpawn();
                }
                return entity;
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

    @Override
    public synchronized void remove(String id) {
        try {
            Files.deleteIfExists(path.resolve(id + ".stag"));
        } catch (IOException e) {
            LOGGER.error("Error writing player data: {}", e.toString());
        }
    }

    @Override
    public boolean playerExists(String id) {
        return Files.exists(path.resolve(id + ".stag"));
    }

    private synchronized TagStructure load(String id) {
        try {
            Path file = path.resolve(id + ".stag");
            if (Files.exists(file)) {
                return FileUtil.readReturn(file, TagStructureBinary::read);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading player data: {}", e.toString());
        }
        return new TagStructure();
    }

    private synchronized void save(TagStructure tagStructure, String id) {
        try {
            Path file = path.resolve(id + ".stag");
            FileUtil.write(file,
                    stream -> TagStructureBinary.write(tagStructure, stream));
        } catch (IOException e) {
            LOGGER.error("Error writing player data: {}", e.toString());
        }
    }
}
