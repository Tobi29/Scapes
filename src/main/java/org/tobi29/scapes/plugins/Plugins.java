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

package org.tobi29.scapes.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPathRoot;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Plugins {
    private static final Logger LOGGER = LoggerFactory.getLogger(Plugins.class);
    private final List<PluginFile> files;
    private final List<Plugin> plugins = new ArrayList<>();
    private final List<Dimension> dimensions = new ArrayList<>();
    private final GameRegistry registry;
    private WorldType worldType;
    private PluginClassLoader classLoader;

    public Plugins(List<PluginFile> files, IDStorage idStorage)
            throws IOException {
        this(files, idStorage, null);
    }

    public Plugins(List<PluginFile> files, IDStorage idStorage,
            Directory directory) throws IOException {
        this.files = files;
        registry = new GameRegistry(idStorage);
        classLoader = new PluginClassLoader(files, directory);
        for (PluginFile file : files) {
            Plugin plugin = file.getPlugin(classLoader);
            plugins.add(plugin);
            if (plugin instanceof Dimension) {
                dimensions.add((Dimension) plugin);
            }
            if (plugin instanceof WorldType) {
                if (worldType != null) {
                    throw new IOException("Found 2nd world type: " + plugin);
                }
                worldType = (WorldType) plugin;
            }
        }
        if (worldType == null) {
            throw new IOException("No world type found");
        }
    }

    public void dispose() {
        for (Plugin plugin : plugins) {
            plugin.dispose(registry);
        }
        plugins.clear();
        dimensions.clear();
        worldType = null;
        try {
            classLoader.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close plugin classloader: {}",
                    e.toString());
        }
        classLoader = null;
    }

    public GameRegistry getRegistry() {
        return registry;
    }

    public int getFileCount() {
        return files.size();
    }

    public Stream<PluginFile> getFiles() {
        return files.stream();
    }

    public PluginFile getFile(int i) {
        return files.get(i);
    }

    public Stream<Plugin> getPlugins() {
        return plugins.stream();
    }

    public Stream<Dimension> getDimensions() {
        return dimensions.stream();
    }

    public WorldType getWorldType() {
        return worldType;
    }

    public Plugin getPlugin(String name) {
        for (Plugin plugin : plugins) {
            if (plugin.getID().equals(name)) {
                return plugin;
            }
        }
        throw new IllegalArgumentException("Unknown plugin");
    }

    public boolean checkPlugin(String name) {
        for (Plugin plugin : plugins) {
            if (plugin.getID().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void addFileSystems(FileSystemContainer files) {
        try {
            for (Plugin plugin : plugins) {
                files.registerFileSystem(plugin.getID(), plugin.getAssetRoot(),
                        ClasspathPathRoot.make(classLoader));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to add virtual filesystems for plugins: {}",
                    e.toString());
        }
    }

    public void removeFileSystems(FileSystemContainer files) {
        for (Plugin plugin : plugins) {
            files.removeFileSystem(plugin.getID());
        }
    }

    public void init() {
        for (Plugin plugin : plugins) {
            plugin.init(registry);
        }
        for (Plugin plugin : plugins) {
            plugin.initEnd(registry);
        }
        registry.lock();
    }

    public void initServer(ScapesServer server) {
        for (Plugin plugin : plugins) {
            plugin.initServer(server);
        }
    }
}
