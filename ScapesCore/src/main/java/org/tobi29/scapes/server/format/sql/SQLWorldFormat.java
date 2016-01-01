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
package org.tobi29.scapes.server.format.sql;

import java8.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer;
import org.tobi29.scapes.engine.sql.SQLColumn;
import org.tobi29.scapes.engine.sql.SQLDatabase;
import org.tobi29.scapes.engine.sql.SQLQuery;
import org.tobi29.scapes.engine.sql.SQLType;
import org.tobi29.scapes.engine.utils.io.ByteBufferStream;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.PlayerData;
import org.tobi29.scapes.server.format.TerrainInfiniteFormat;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class SQLWorldFormat implements WorldFormat {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SQLWorldFormat.class);
    private static final Pattern SPACE = Pattern.compile(" ");
    protected final IDStorage idStorage;
    protected final TagStructure idTagStructure;
    protected final FilePath path;
    protected final SQLDatabase database;
    protected final SQLQuery getMetaData, getData, getWorldData;
    protected final Plugins plugins;
    protected final PlayerData playerData;
    protected final long seed;
    private final ByteBufferStream stream = new ByteBufferStream();

    public SQLWorldFormat(FilePath path, SQLDatabase database)
            throws IOException {
        this.path = path;
        this.database = database;
        playerData = new SQLPlayerData(this.database);
        getMetaData = this.database
                .compileQuery("MetaData", new String[]{"Value"}, "Name");
        getData = this.database
                .compileQuery("Data", new String[]{"Value"}, "Name");
        getWorldData = this.database
                .compileQuery("Worlds", new String[]{"Data"}, "World");
        checkDatabase(database);
        List<Object[]> rows = getMetaData.run("Seed");
        if (!rows.isEmpty()) {
            Object[] row = rows.get(0);
            try {
                seed = Long.parseLong(row[0].toString());
            } catch (NumberFormatException e) {
                throw new IOException("Corrupt seed record: " + e.getMessage());
            }
        } else {
            LOGGER.info("No seed in database, adding a random one in.");
            seed = ThreadLocalRandom.current().nextLong();
            this.database.replace("MetaData", new String[]{"Name", "Value"},
                    new Object[]{"Seed", seed});
        }
        rows = getData.run("IDs");
        if (!rows.isEmpty()) {
            Object[] row = rows.get(0);
            if (row[0] instanceof byte[]) {
                byte[] array = (byte[]) row[0];
                idTagStructure = TagStructureBinary
                        .read(new ByteBufferStream(ByteBuffer.wrap(array)));
            } else {
                idTagStructure = new TagStructure();
            }
        } else {
            idTagStructure = new TagStructure();
        }
        idStorage = new IDStorage(idTagStructure);
        plugins = createPlugins();
    }

    public static void checkDatabase(SQLDatabase database) throws IOException {
        database.createTable("Data", "Name",
                new SQLColumn("Name", SQLType.VARCHAR, "255"),
                new SQLColumn("Value", SQLType.LONGBLOB));
        database.createTable("MetaData", "Name",
                new SQLColumn("Name", SQLType.VARCHAR, "255"),
                new SQLColumn("Value", SQLType.VARCHAR, "255"));
        database.createTable("Players", "ID",
                new SQLColumn("ID", SQLType.CHAR, "40"),
                new SQLColumn("World", SQLType.VARCHAR, "255"),
                new SQLColumn("Permissions", SQLType.INT),
                new SQLColumn("Entity", SQLType.LONGBLOB));
        database.createTable("Worlds", "World",
                new SQLColumn("World", SQLType.VARCHAR, "255"),
                new SQLColumn("Data", SQLType.LONGBLOB));
    }

    public static void initDatabase(SQLDatabase database, long seed)
            throws IOException {
        checkDatabase(database);
        database.replace("MetaData", new String[]{"Name", "Value"},
                new Object[]{"Seed", seed});
    }

    protected Plugins createPlugins() throws IOException {
        return new Plugins(pluginFiles(), idStorage);
    }

    @Override
    public IDStorage idStorage() {
        return idStorage;
    }

    @Override
    public PlayerData playerData() {
        return playerData;
    }

    @Override
    public long seed() {
        return seed;
    }

    @Override
    public Plugins plugins() {
        return plugins;
    }

    @Override
    public synchronized WorldServer registerWorld(ScapesServer server,
            Function<WorldServer, EnvironmentServer> environmentSupplier,
            String name, long seed) throws IOException {
        String table = SPACE.matcher(name).replaceAll("");
        database.createTable(table, "Pos", new SQLColumn("Pos", SQLType.BIGINT),
                new SQLColumn("Data", SQLType.LONGBLOB));
        TerrainInfiniteFormat format =
                new SQLTerrainInfiniteFormat(database, table);
        WorldServer world =
                new WorldServer(this, name, seed, server.connection(),
                        server.taskExecutor(),
                        newWorld -> new TerrainInfiniteServer(newWorld, 512,
                                format, server.taskExecutor(),
                                plugins.registry().air()), environmentSupplier);
        List<Object[]> rows = getWorldData.run(name);
        if (!rows.isEmpty()) {
            Object[] row = rows.get(0);
            if (row[0] instanceof byte[]) {
                byte[] array = (byte[]) row[0];
                TagStructure tagStructure = TagStructureBinary
                        .read(new ByteBufferStream(ByteBuffer.wrap(array)));
                world.read(tagStructure);
            } else {
                world.read(new TagStructure());
            }
        } else {
            world.read(new TagStructure());
        }
        return world;
    }

    @Override
    public synchronized void removeWorld(WorldServer world) {
        try {
            TagStructureBinary.write(world.write(), stream, (byte) 1);
            stream.buffer().flip();
            byte[] array = new byte[stream.buffer().remaining()];
            stream.buffer().get(array);
            stream.buffer().clear();
            database.replace("Worlds", new String[]{"World", "Data"},
                    new Object[]{world.id(), array});
        } catch (IOException e) {
            LOGGER.error("Failed to save world info: {}", e.toString());
        }
    }

    @Override
    public synchronized boolean deleteWorld(String name) {
        try {
            String table = SPACE.matcher(name).replaceAll("");
            database.dropTable(table);
        } catch (IOException e) {
            LOGGER.error("Error whilst deleting world: {}", e.toString());
            return false;
        }
        return true;
    }

    @Override
    public synchronized void dispose() throws IOException {
        plugins.dispose();
        TagStructureBinary.write(idTagStructure, stream, (byte) 1);
        stream.buffer().flip();
        byte[] array = new byte[stream.buffer().remaining()];
        stream.buffer().get(array);
        stream.buffer().clear();
        database.replace("Data", new String[]{"Name", "Value"},
                new Object[]{"IDs", array});
    }

    protected List<PluginFile> pluginFiles() throws IOException {
        FilePath path = this.path.resolve("plugins");
        List<FilePath> files =
                FileUtil.listRecursive(path, FileUtil::isRegularFile,
                        FileUtil::isNotHidden);
        List<PluginFile> plugins = new ArrayList<>(files.size());
        for (FilePath file : files) {
            plugins.add(new PluginFile(file));
        }
        return plugins;
    }
}
