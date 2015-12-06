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
package org.tobi29.scapes.server;

import java8.util.Optional;
import java8.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.server.ServerInfo;
import org.tobi29.scapes.engine.utils.Crashable;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.plugins.Dimension;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.extension.ServerExtensions;
import org.tobi29.scapes.server.format.PlayerData;
import org.tobi29.scapes.server.format.PlayerStatistics;
import org.tobi29.scapes.server.format.WorldFormat;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScapesServer {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesServer.class);
    private final ServerExtensions extensions;
    private final TaskExecutor taskExecutor;
    private final ServerInfo serverInfo;
    private final ServerConnection serverConnection;
    private final WorldSource source;
    private final WorldFormat format;
    private final PlayerData playerData;
    private final Plugins plugins;
    private final CommandRegistry commandRegistry;
    private final int maxLoadingRadius;
    private final Map<String, WorldServer> worlds = new ConcurrentHashMap<>();
    private final long seed;
    private boolean stopped;
    private ShutdownReason shutdownReason = ShutdownReason.RUNNING;

    public ScapesServer(WorldSource source, TagStructure tagStructure,
            ServerInfo serverInfo, Crashable crashHandler) throws IOException {
        this.source = source;
        format = source.open(this);
        playerData = format.playerData();
        plugins = format.plugins();
        seed = format.seed();
        extensions = new ServerExtensions(this);
        extensions.loadExtensions();
        taskExecutor = new TaskExecutor(crashHandler, "Server");
        commandRegistry = new CommandRegistry();
        TagStructure serverTag = tagStructure.getStructure("Server");
        maxLoadingRadius = serverTag.getInteger("MaxLoadingRadius");
        this.serverInfo = serverInfo;
        serverConnection =
                new ServerConnection(this, serverTag.getStructure("Socket"));
        extensions.init();
        format.plugins().init();
        format.plugins().plugins().forEach(plugin -> plugin.initServer(this));
        format.plugins().dimensions().forEach(this::registerWorld);
        format.plugins().plugins()
                .forEach(plugin -> plugin.initServerEnd(this));
    }

    public ShutdownReason shutdownReason() {
        return shutdownReason;
    }

    public ServerConnection connection() {
        return serverConnection;
    }

    public ServerExtensions extensions() {
        return extensions;
    }

    public Plugins plugins() {
        return plugins;
    }

    public ServerInfo serverInfo() {
        return serverInfo;
    }

    public int maxLoadingRadius() {
        return maxLoadingRadius;
    }

    public TaskExecutor taskExecutor() {
        return taskExecutor;
    }

    public CommandRegistry commandRegistry() {
        return commandRegistry;
    }

    public Optional<WorldServer> world(String name) {
        return Optional.ofNullable(worlds.get(name));
    }

    public Optional<WorldServer> defaultWorld() {
        return Optional.ofNullable(worlds.get(plugins.worldType().id()));
    }

    public Optional<WorldServer> registerWorld(Dimension dimension) {
        return registerWorld(dimension::createEnvironment, dimension.id(),
                seed);
    }

    public synchronized Optional<WorldServer> registerWorld(
            Function<WorldServer, EnvironmentServer> environmentSupplier,
            String name, long seed) {
        removeWorld(name);
        LOGGER.info("Adding world: {}", name);
        WorldServer world = null;
        try {
            world = format.registerWorld(this, environmentSupplier, name, seed);
        } catch (IOException e) {
            LOGGER.error("Failed to register world: {}", e.toString());
            return Optional.empty();
        }
        world.calculateSpawn();
        worlds.put(name, world);
        world.start();
        return Optional.of(world);
    }

    public synchronized boolean removeWorld(String name) {
        WorldServer world = worlds.remove(name);
        if (world == null) {
            return false;
        }
        removeWorld(world);
        return true;
    }

    public synchronized void removeWorld(WorldServer world) {
        LOGGER.info("Removing world: {}", world.id());
        world.stop(defaultWorld());
        world.dispose();
        format.removeWorld(world);
    }

    public synchronized boolean deleteWorld(String name) {
        LOGGER.info("Deleting world: {}", name);
        removeWorld(name);
        return format.deleteWorld(name);
    }

    public PlayerEntry player(String id) {
        return playerData.player(id);
    }

    public void save(String id, MobPlayerServer entity, int permissions,
            PlayerStatistics statistics) {
        playerData.save(id, entity, permissions, statistics);
    }

    public void add(String id) {
        playerData.add(id);
    }

    public void remove(String id) {
        playerData.remove(id);
    }

    public boolean playerExists(String id) {
        return playerData.playerExists(id);
    }

    public void scheduleStop(ShutdownReason shutdownReason) {
        this.shutdownReason = shutdownReason;
    }

    public void stop(ShutdownReason shutdownReason) throws IOException {
        this.shutdownReason = shutdownReason;
        stop();
    }

    public synchronized void stop() throws IOException {
        if (stopped) {
            return;
        }
        assert shutdownReason != ShutdownReason.RUNNING;
        stopped = true;
        Streams.of(worlds.values()).forEach(this::removeWorld);
        serverConnection.stop();
        taskExecutor.shutdown();
        format.dispose();
        source.close();
    }

    public boolean shouldStop() {
        return shutdownReason != ShutdownReason.RUNNING;
    }

    public boolean hasStopped() {
        return stopped;
    }

    public enum ShutdownReason {
        RUNNING,
        STOP,
        RELOAD,
        ERROR
    }
}
