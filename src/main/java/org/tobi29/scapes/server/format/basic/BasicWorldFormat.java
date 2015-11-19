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
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.PlayerData;
import org.tobi29.scapes.server.format.TerrainInfiniteFormat;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class BasicWorldFormat implements WorldFormat {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(BasicWorldFormat.class);
    private final Path path, regionPath;
    private final IDStorage idStorage;
    private final Plugins plugins;
    private final PlayerData playerData;
    private final TagStructure worldsTagStructure;
    private final long seed;

    public BasicWorldFormat(Path path, TagStructure tagStructure)
            throws IOException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                    new RuntimePermission("scapes.worldFormat"));
        }
        this.path = path;
        seed = tagStructure.getLong("Seed");
        idStorage = new IDStorage(tagStructure.getStructure("IDs"));
        worldsTagStructure = tagStructure.getStructure("Worlds");
        regionPath = path.resolve("region");
        playerData = new BasicPlayerData(path.resolve("players"));
        plugins = new Plugins(pluginFiles(), idStorage);
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
        Optional<TerrainInfiniteFormat> format = AccessController.doPrivileged(
                (PrivilegedAction<Optional<TerrainInfiniteFormat>>) () -> {
                    try {
                        Path worldDirectory =
                                regionPath.resolve(name.toLowerCase());
                        Files.createDirectories(worldDirectory);
                        return Optional.of(new BasicTerrainInfiniteFormat(
                                worldDirectory));
                    } catch (IOException e) {
                        LOGGER.error("Error whilst creating world: {}",
                                e.toString());
                    }
                    return Optional.empty();
                });
        if (!format.isPresent()) {
            throw new IOException("Unable to create world");
        }
        WorldServer world =
                new WorldServer(this, name, seed, server.connection(),
                        server.taskExecutor(),
                        newWorld -> new TerrainInfiniteServer(newWorld, 512,
                                format.get(), server.taskExecutor(),
                                plugins.registry().air()), environmentSupplier);
        world.read(worldsTagStructure.getStructure(name));
        return world;
    }

    @Override
    public synchronized void removeWorld(WorldServer world) {
        worldsTagStructure.setStructure(world.id(), world.write());
    }

    @Override
    public synchronized boolean deleteWorld(String name) {
        Path worldDirectory = regionPath.resolve(name.toLowerCase());
        if (Files.exists(worldDirectory)) {
            try {
                AccessController
                        .doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                            FileUtil.deleteDir(worldDirectory);
                            return null;
                        });
            } catch (PrivilegedActionException e) {
                LOGGER.error("Error whilst deleting world: {}", e.toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public void dispose() throws IOException {
        plugins.dispose();
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
