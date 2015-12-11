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

import java8.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPath;
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathResource;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Plugins {
    private static final Logger LOGGER = LoggerFactory.getLogger(Plugins.class);
    private final List<PluginFile> files;
    private final List<Plugin> plugins = new ArrayList<>();
    private final List<Dimension> dimensions = new ArrayList<>();
    private final GameRegistry registry;
    private final URLClassLoader classLoader;
    private WorldType worldType;

    public Plugins(List<PluginFile> files, IDStorage idStorage,
            boolean loadClasses) throws IOException {
        if (loadClasses) {
            this.files = files;
            classLoader = new PluginClassLoader(files);
            for (PluginFile file : files) {
                load(file.plugin(classLoader));
            }
        } else {
            this.files = Collections.emptyList();
            classLoader = null;
            ClassLoader classLoader = getClass().getClassLoader();
            PluginFile file = new PluginFile(
                    new ClasspathResource(classLoader, "Plugin.json"));
            load(file.plugin(classLoader));
        }
        if (worldType == null) {
            throw new IOException("No world type found");
        }
        registry = new GameRegistry(idStorage, this);
    }

    private void load(Plugin plugin) throws IOException {
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

    public void dispose() {
        plugins.clear();
        dimensions.clear();
        worldType = null;
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close plugin classloader: {}",
                        e.toString());
            }
        }
    }

    public GameRegistry registry() {
        return registry;
    }

    public int fileCount() {
        return files.size();
    }

    public Stream<PluginFile> files() {
        return Streams.of(files);
    }

    public PluginFile file(int i) {
        return files.get(i);
    }

    public Stream<Plugin> plugins() {
        return Streams.of(plugins);
    }

    public Stream<Dimension> dimensions() {
        return Streams.of(dimensions);
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
                    new ClasspathPath(plugin.getClass().getClassLoader(),
                            plugin.assetRoot()));
        }
    }

    public void removeFileSystems(FileSystemContainer files) {
        for (Plugin plugin : plugins) {
            files.removeFileSystem(plugin.id());
        }
    }

    public void init() {
        Streams.of(plugins).forEach(plugin -> plugin.init(registry));
        Streams.of(plugins).forEach(plugin -> plugin.initEnd(registry));
        registry.lock();
    }
}