/*
 * Copyright 2012-2016 Tobi29
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
package org.tobi29.scapes.server.format.sql;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.sql.SQLDatabase;
import org.tobi29.scapes.engine.sql.SQLQuery;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.ByteBufferStream;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.PlayerEntry;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.format.PlayerData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class SQLPlayerData implements PlayerData {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SQLPlayerData.class);
    private final ByteBufferStream stream = new ByteBufferStream();
    private final SQLDatabase database;
    private final SQLQuery getPlayer, checkPlayer;

    public SQLPlayerData(SQLDatabase database) {
        this.database = database;
        getPlayer = this.database.compileQuery("Players",
                new String[]{"World", "Permissions", "Entity"}, "ID");
        checkPlayer =
                this.database.compileQuery("Players", new String[]{"1"}, "ID");
    }

    @Override
    public synchronized PlayerEntry player(String id) {
        Optional<String> worldNameFetch = Optional.empty();
        int permissionsFetch = 0;
        Optional<TagStructure> entityFetch = Optional.empty();
        try {
            List<Object[]> rows = getPlayer.run(id);
            if (!rows.isEmpty()) {
                Object[] row = rows.get(0);
                worldNameFetch = Optional.ofNullable(String.valueOf(row[0]));
                try {
                    permissionsFetch = Integer.parseInt(String.valueOf(row[1]));
                } catch (NumberFormatException e) {
                    LOGGER.error("Corrupt player permission record: {}",
                            e.toString());
                }
                if (row[2] instanceof byte[]) {
                    byte[] array = (byte[]) row[2];
                    TagStructure tagStructure = TagStructureBinary
                            .read(new ByteBufferStream(ByteBuffer.wrap(array)));
                    entityFetch = Optional.of(tagStructure);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load player: {}", e.toString());
        }
        Optional<String> worldName = worldNameFetch;
        int permissions = permissionsFetch;
        Optional<TagStructure> entityTag = entityFetch;
        return new PlayerEntry() {
            @Override
            public Optional<MobPlayerServer> createEntity(
                    PlayerConnection player, Optional<WorldServer> world,
                    Optional<Vector3> pos) {
                if (!entityTag.isPresent()) {
                    return Optional.empty();
                }
                TagStructure tagStructure = entityTag.get();
                ScapesServer server = player.server().server();
                if (!world.isPresent() && worldName.isPresent()) {
                    world = server.world(worldName.get());
                }
                if (!world.isPresent()) {
                    world = server.defaultWorld();
                }
                if (!world.isPresent()) {
                    return Optional.empty();
                }
                WorldServer spawnWorld = world.get();
                Vector3 spawnPos = new Vector3d(0.5, 0.5, 1.0)
                        .plus(pos.orElseGet(spawnWorld::spawn));
                MobPlayerServer entity = spawnWorld.plugins().worldType()
                        .newPlayer(spawnWorld, spawnPos, Vector3d.ZERO, 0.0,
                                0.0, player.name(), player.skin().checksum(),
                                player);
                pos.ifPresent(
                        forcePos -> tagStructure.setMultiTag("Pos", forcePos));
                entity.read(tagStructure);
                return Optional.of(entity);
            }

            @Override
            public int permissions() {
                return permissions;
            }
        };
    }

    @Override
    public synchronized void save(String id, MobPlayerServer entity,
            int permissions) {
        WorldServer world = entity.world();
        try {
            TagStructureBinary.write(entity.write(false), stream, (byte) 1);
            stream.buffer().flip();
            byte[] array = new byte[stream.buffer().remaining()];
            stream.buffer().get(array);
            stream.buffer().clear();
            database.replace("Players",
                    new String[]{"ID", "World", "Permissions", "Entity"},
                    new Object[]{id, world.id(), permissions, array});
        } catch (IOException e) {
            LOGGER.error("Failed to save player: {}", e.toString());
        }
    }

    @Override
    public synchronized void add(String id) {
        try {
            database.insert("Players", new String[]{"ID", "Permissions"},
                    new Object[]{id, 0});
        } catch (IOException e) {
            LOGGER.error("Failed to delete player: {}", e.toString());
        }
    }

    @Override
    public synchronized void remove(String id) {
        try {
            database.delete("Players", new Pair<>("ID", id));
        } catch (IOException e) {
            LOGGER.error("Failed to delete player: {}", e.toString());
        }
    }

    @Override
    public boolean playerExists(String id) {
        try {
            return !checkPlayer.run(id).isEmpty();
        } catch (IOException e) {
            LOGGER.error("Failed to load player: {}", e.toString());
        }
        return false;
    }
}
