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

package org.tobi29.scapes.server.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldEnvironment;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.plugins.Dimension;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldFormat {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WorldFormat.class);
    private static final String FILENAME_EXTENSION = ".spkg";
    private final ScapesServer server;
    private final IDStorage idStorage = new IDStorage();
    private final Directory directory;
    private final Plugins plugins;
    private final PlayerData playerData;
    private final TagStructure worldsTagStructure;
    private final Map<String, WorldServer> worlds = new ConcurrentHashMap<>();
    private final long seed;

    public WorldFormat(ScapesServer server, Directory directory)
            throws IOException {
        this.server = server;
        this.directory = directory;
        playerData = new PlayerData(server, directory.get("players"));
        TagStructure tagStructure = directory.getResource("Data.stag")
                .readAndReturn(TagStructureBinary::read);
        idStorage.load(tagStructure.getStructure("IDs"));
        plugins = new Plugins(pluginFiles(), idStorage, directory);
        plugins.init();
        plugins.initServer(server);
        seed = tagStructure.getLong("Seed");
        worldsTagStructure = tagStructure.getStructure("Worlds");
        for (Dimension dimension : plugins.getDimensions()) {
            registerWorld(dimension);
        }
    }

    public static String getFilenameExtension() {
        return FILENAME_EXTENSION;
    }

    public IDStorage getIDStorage() {
        return idStorage;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public long getSeed() {
        return seed;
    }

    public Plugins getPlugins() {
        return plugins;
    }

    public WorldServer getWorld(String name) {
        return worlds.get(name);
    }

    public Collection<String> getWorldNames() {
        return worlds.keySet();
    }

    public void updateControlPanelWorlds() {
        Collection<String> worlds = this.worlds.keySet();
        String[] worldsArray = new String[worlds.size()];
        worlds.toArray(worldsArray);
        server.getControlPanels().forEach(
                controlPanel -> controlPanel.updateWorlds(worldsArray));
    }

    public synchronized WorldServer registerWorld(Dimension dimension) throws IOException {
        return registerWorld(dimension, dimension.getID());
    }

    public synchronized WorldServer registerWorld(Dimension dimension, String name)
            throws IOException {
        Directory worldDirectory =
                directory.get("region/" + name.toLowerCase());
        WorldServer world = new WorldServer(this, name, dimension.getID(),
                server.getConnection(), server.getTaskExecutor(),
                newWorld -> new TerrainInfiniteServer(newWorld, 512,
                        worldDirectory, server.getTaskExecutor(),
                        plugins.getRegistry().getAir()));
        WorldEnvironment environment = dimension.createEnvironment(world);
        LOGGER.info("Adding world: {} (Environment:{})", name, environment);
        world.init(environment);
        world.read(worldsTagStructure.getStructure(dimension.getID()));
        world.calculateSpawn();
        worlds.put(dimension.getID(), world);
        world.start();
        updateControlPanelWorlds();
        return world;
    }

    public synchronized void removeWorld(String name) {
        WorldServer world = worlds.remove(name);
        if (world == null) {
            throw new IllegalArgumentException("Unknown world");
        }
        LOGGER.info("Removing world: {}", name);
        world.stop();
        world.dispose();
        worldsTagStructure.setStructure(world.getName(), world.write());
        updateControlPanelWorlds();
    }

    public void save() throws IOException {
        TagStructure tagStructure = new TagStructure();
        tagStructure.setLong("LastPlayed", System.currentTimeMillis());
        tagStructure.setLong("Seed", seed);
        tagStructure.setStructure("IDs", idStorage.save());
        tagStructure.setStructure("Worlds", worldsTagStructure);
        directory.getResource("Data.stag").write(streamOut -> TagStructureBinary
                .write(tagStructure, streamOut));
        plugins.dispose();
    }

    public void savePanorama(Image[] images) throws IOException {
        if (images.length != 6) {
            throw new IllegalArgumentException("6 panorama images required");
        }
        for (int i = 0; i < 6; i++) {
            int j = i;
            directory.getResource("Panorama" + i + ".png")
                    .write(streamOut -> PNG
                            .encode(images[j], streamOut, 9, false));
        }
    }

    public Collection<WorldServer> getWorlds() {
        return worlds.values();
    }

    private List<PluginFile> pluginFiles() throws IOException {
        List<PluginFile> plugins = new ArrayList<>();
        for (File plugin : directory.get("plugins")
                .listFiles(file -> file.getName().endsWith(".jar"))) {
            plugins.add(new PluginFile(plugin));
        }
        return plugins;
    }
}
