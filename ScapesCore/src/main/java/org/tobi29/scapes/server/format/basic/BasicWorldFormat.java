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
import java8.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.PlayerData;
import org.tobi29.scapes.server.format.TerrainInfiniteFormat;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

public class BasicWorldFormat implements WorldFormat {
    protected static final Logger LOGGER =
            LoggerFactory.getLogger(BasicWorldFormat.class);
    protected final FilePath path, regionPath;
    protected final IDStorage idStorage;
    protected final Plugins plugins;
    protected final PlayerData playerData;
    protected final TagStructure worldsTagStructure;
    protected final long seed;

    public BasicWorldFormat(FilePath path, TagStructure tagStructure)
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
        plugins = createPlugins();
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

    @SuppressWarnings("CallToNativeMethodWhileLocked")
    @Override
    public synchronized WorldServer registerWorld(ScapesServer server,
            Function<WorldServer, EnvironmentServer> environmentSupplier,
            String name, long seed) throws IOException {
        Optional<TerrainInfiniteFormat> format = AccessController.doPrivileged(
                (PrivilegedAction<Optional<TerrainInfiniteFormat>>) () -> {
                    try {
                        FilePath worldDirectory =
                                regionPath.resolve(name.toLowerCase());
                        FileUtil.createDirectories(worldDirectory);
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

    @SuppressWarnings({"ReturnOfNull", "CallToNativeMethodWhileLocked"})
    @Override
    public synchronized boolean deleteWorld(String name) {
        FilePath worldDirectory = regionPath.resolve(name.toLowerCase());
        if (FileUtil.exists(worldDirectory)) {
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
