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
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPath;

import java.io.IOException;
import java.nio.file.Path;
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

    public Plugins(List<PluginFile> files, IDStorage idStorage, Path path)
            throws IOException {
        this.files = files;
        classLoader = new PluginClassLoader(files, path);
        for (PluginFile file : files) {
            Plugin plugin = file.plugin(classLoader);
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
        registry = new GameRegistry(idStorage, this);
    }

    public void dispose() {
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

    public GameRegistry registry() {
        return registry;
    }

    public int fileCount() {
        return files.size();
    }

    public Stream<PluginFile> files() {
        return files.stream();
    }

    public PluginFile file(int i) {
        return files.get(i);
    }

    public Stream<Plugin> plugins() {
        return plugins.stream();
    }

    public Stream<Dimension> dimensions() {
        return dimensions.stream();
    }

    public WorldType worldType() {
        return worldType;
    }

    public Plugin plugin(String name) {
        for (Plugin plugin : plugins) {
            if (plugin.id().equals(name)) {
                return plugin;
            }
        }
        throw new IllegalArgumentException("Unknown plugin");
    }

    public void addFileSystems(FileSystemContainer files) {
        for (Plugin plugin : plugins) {
            files.registerFileSystem(plugin.id(),
                    new ClasspathPath(classLoader, plugin.assetRoot()));
        }
    }

    public void removeFileSystems(FileSystemContainer files) {
        for (Plugin plugin : plugins) {
            files.removeFileSystem(plugin.id());
        }
    }

    public void init() {
        plugins.forEach(plugin -> plugin.init(registry));
        plugins.forEach(plugin -> plugin.initEnd(registry));
        registry.lock();
    }
}
