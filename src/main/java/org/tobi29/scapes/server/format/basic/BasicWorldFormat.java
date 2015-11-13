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
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.plugins.Dimension;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.PlayerData;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class BasicWorldFormat implements WorldFormat {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(BasicWorldFormat.class);
    private final ScapesServer server;
    private final IDStorage idStorage = new IDStorage();
    private final Path path, regionPath;
    private final Plugins plugins;
    private final PlayerData playerData;
    private final TagStructure worldsTagStructure;
    private final Map<String, WorldServer> worlds = new ConcurrentHashMap<>();
    private final WorldServer defaultWorld;
    private final long seed;

    public BasicWorldFormat(ScapesServer server, Path path) throws IOException {
        this.server = server;
        this.path = path;
        regionPath = path.resolve("region");
        playerData = new BasicPlayerData(path.resolve("players"));
        TagStructure tagStructure =
                FileUtil.readReturn(path.resolve("Data.stag"),
                        TagStructureBinary::read);
        idStorage.load(tagStructure.getStructure("IDs"));
        seed = tagStructure.getLong("Seed");
        worldsTagStructure = tagStructure.getStructure("Worlds");
        plugins = new Plugins(pluginFiles(), idStorage, path);
        plugins.init();
        Iterator<Dimension> iterator = plugins.dimensions().iterator();
        while (iterator.hasNext()) {
            registerWorld(iterator.next());
        }
        defaultWorld = worlds.get(plugins.worldType().id());
        plugins.plugins().forEach(plugin -> plugin.initServer(server));
        plugins.plugins().forEach(plugin -> plugin.initServerEnd(server));
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

    public Optional<WorldServer> world(String name) {
        return Optional.ofNullable(worlds.get(name));
    }

    @Override
    public WorldServer defaultWorld() {
        return defaultWorld;
    }

    @Override
    public Collection<String> worldNames() {
        return worlds.keySet();
    }

    @Override
    public synchronized WorldServer registerWorld(Dimension dimension)
            throws IOException {
        return registerWorld(dimension::createEnvironment, dimension.id(),
                seed);
    }

    @Override
    public synchronized WorldServer registerWorld(
            Function<WorldServer, EnvironmentServer> environmentSupplier,
            String name, long seed) throws IOException {
        removeWorld(name);
        Path worldDirectory = regionPath.resolve(name.toLowerCase());
        Files.createDirectories(worldDirectory);
        WorldServer world =
                new WorldServer(this, name, seed, server.connection(),
                        server.taskExecutor(),
                        newWorld -> new TerrainInfiniteServer(newWorld, 512,
                                worldDirectory, server.taskExecutor(),
                                plugins.registry().air()), environmentSupplier);
        LOGGER.info("Adding world: {}", name);
        world.read(worldsTagStructure.getStructure(name));
        world.calculateSpawn();
        worlds.put(name, world);
        world.start();
        return world;
    }

    @Override
    public synchronized boolean removeWorld(String name) {
        WorldServer world = worlds.remove(name);
        if (world == null) {
            return false;
        }
        LOGGER.info("Removing world: {}", name);
        world.stop(defaultWorld);
        world.dispose();
        worldsTagStructure.setStructure(world.id(), world.write());
        return true;
    }

    @Override
    public synchronized void deleteWorld(String name) throws IOException {
        removeWorld(name);
        Path worldDirectory = regionPath.resolve(name.toLowerCase());
        if (Files.exists(worldDirectory)) {
            FileUtil.deleteDir(worldDirectory);
        }
    }

    @Override
    public void save() throws IOException {
        TagStructure tagStructure = new TagStructure();
        tagStructure.setLong("LastPlayed", System.currentTimeMillis());
        tagStructure.setLong("Seed", seed);
        tagStructure.setStructure("IDs", idStorage.save());
        tagStructure.setStructure("Worlds", worldsTagStructure);
        FileUtil.write(path.resolve("Data.stag"),
                streamOut -> TagStructureBinary.write(tagStructure, streamOut));
    }

    @Override
    public void savePanorama(Image[] images) throws IOException {
        if (images.length != 6) {
            throw new IllegalArgumentException("6 panorama images required");
        }
        for (int i = 0; i < 6; i++) {
            int j = i;
            FileUtil.write(path.resolve("Panorama" + i + ".png"),
                    streamOut -> PNG.encode(images[j], streamOut, 9, false));
        }
    }

    private List<PluginFile> pluginFiles() throws IOException {
        List<PluginFile> plugins = new ArrayList<>();
        Path path = this.path.resolve("plugins");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && !Files.isHidden(file)) {
                    plugins.add(new PluginFile(file));
                }
            }
        }
        return plugins;
    }
}
