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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.connection.ServerInfo;
import org.tobi29.scapes.engine.utils.Crashable;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.Sync;
import org.tobi29.scapes.engine.utils.io.SystemOutReader;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScapesServer {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesServer.class);
    private final Directory directory;
    private final TaskExecutor taskExecutor;
    private final ServerInfo serverInfo;
    private final ServerConnection serverConnection;
    private final WorldFormat worldFormat;
    private final CommandRegistry commandRegistry;
    private final String controlPanelPassword;
    private final Joiner logJoiner, profilerJoiner;
    private final Map<String, ControlPanel> controlPanels =
            new ConcurrentHashMap<>();
    private boolean stopped;
    private ShutdownReason shutdownReason = ShutdownReason.ERROR;

    public ScapesServer(Directory directory, TagStructure tagStructure,
            ServerInfo serverInfo, Crashable crashHandler) throws IOException {
        this(directory, tagStructure, serverInfo, crashHandler,
                Collections.emptyList());
    }

    public ScapesServer(Directory directory, TagStructure tagStructure,
            ServerInfo serverInfo, Crashable crashHandler,
            Collection<ControlPanel> initalControlPanels) throws IOException {
        taskExecutor = new TaskExecutor(crashHandler, "Server");
        this.directory = directory;
        commandRegistry = new CommandRegistry();
        initalControlPanels.forEach(controlPanel -> controlPanels
                .put(controlPanel.getID(), controlPanel));
        logJoiner = taskExecutor.runTask(joiner -> {
            try (SystemOutReader logReader = new SystemOutReader()) {
                while (!joiner.marked()) {
                    String line = logReader.readLine();
                    if (line == null) {
                        List<ControlPanel> closedControlPanels =
                                controlPanels.values().stream()
                                        .filter(ControlPanel::isClosed)
                                        .collect(Collectors.toList());
                        closedControlPanels.forEach(this::removeControlPanel);
                        SleepUtil.sleep(10);
                    } else {
                        for (ControlPanel controlPanel : controlPanels
                                .values()) {
                            controlPanel.appendLog(line);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error appending log to control panels: {}",
                        e.toString());
            }
        }, "Log");
        TagStructure serverTag = tagStructure.getStructure("Server");
        controlPanelPassword = serverTag.getString("ControlPanelPassword");
        this.serverInfo = serverInfo;
        serverConnection = new ServerConnection(this, taskExecutor,
                serverTag.getStructure("Socket"));
        worldFormat = new WorldFormat(this, directory);
        ScapesServerCommands.register(commandRegistry, this);
        profilerJoiner = taskExecutor.runTask(joiner -> {
            Runtime runtime = Runtime.getRuntime();
            Sync sync = new Sync(5.0, 1000000, false, "Server-Profiler");
            sync.init();
            while (!joiner.marked()) {
                long ram = runtime.totalMemory() - runtime.freeMemory();
                Collection<WorldServer> worlds = worldFormat.getWorlds();
                Map<String, Double> tps =
                        new ConcurrentHashMap<>(worlds.size());
                worlds.forEach(world -> {
                    Sync worldSync = world.getSync();
                    tps.put(world.getName(), (double) worldSync.getDiff() /
                            worldSync.getMaxDiff());
                });
                controlPanels.values().forEach(controlPanel -> controlPanel
                        .sendProfilerResults(ram, tps));
                sync.capTPS();
            }
        }, "Profiler");
    }

    public ShutdownReason getShutdownReason() {
        return shutdownReason;
    }

    public void addControlPanel(ControlPanel controlPanel) {
        ControlPanel oldControlPanel =
                controlPanels.put(controlPanel.getID(), controlPanel);
        if (oldControlPanel != null) {
            oldControlPanel.replaced();
        }
        serverConnection.updateControlPanelPlayers();
        worldFormat.updateControlPanelWorlds();
    }

    public void removeControlPanel(ControlPanel controlPanel) {
        controlPanels.remove(controlPanel.getID(), controlPanel);
        serverConnection.updateControlPanelPlayers();
    }

    public Stream<ControlPanel> getControlPanels() {
        return controlPanels.values().stream();
    }

    public ServerConnection getConnection() {
        return serverConnection;
    }

    public WorldFormat getWorldFormat() {
        return worldFormat;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public String getControlPanelPassword() {
        return controlPanelPassword;
    }

    public void start(int port) {
        try {
            serverConnection.start(port);
        } catch (IOException e) {
            LOGGER.error("Error starting server: {}", e.toString());
        }
    }

    public Directory getDirectory() {
        return directory;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public void stop(ShutdownReason shutdownReason) {
        worldFormat.getWorldNames().forEach(worldFormat::removeWorld);
        profilerJoiner.join();
        serverConnection.stop();
        logJoiner.join();
        stopped = true;
        taskExecutor.shutdown();
        this.shutdownReason = shutdownReason;
    }

    public boolean hasStopped() {
        return stopped;
    }

    public enum ShutdownReason {
        STOP,
        RELOAD,
        ERROR
    }
}
