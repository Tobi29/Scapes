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
import org.tobi29.scapes.engine.utils.Crashable;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.Sync;
import org.tobi29.scapes.engine.utils.io.SystemOutReader;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.controlpanel.ServerInfo;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScapesServer {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesServer.class);
    private final TaskExecutor taskExecutor;
    private final ServerInfo serverInfo;
    private final ServerConnection serverConnection;
    private final WorldFormat worldFormat;
    private final CommandRegistry commandRegistry;
    private final String controlPanelPassword;
    private final int maxLoadingRadius;
    private final Joiner logJoiner, profilerJoiner;
    private final Map<String, ControlPanel> controlPanels =
            new ConcurrentHashMap<>();
    private boolean stopped;
    private ShutdownReason shutdownReason = ShutdownReason.ERROR;

    public ScapesServer(Path path, TagStructure tagStructure,
            ServerInfo serverInfo, Crashable crashHandler) throws IOException {
        this(path, tagStructure, serverInfo, crashHandler,
                Collections.emptyList());
    }

    public ScapesServer(Path path, TagStructure tagStructure,
            ServerInfo serverInfo, Crashable crashHandler,
            Collection<ControlPanel> initalControlPanels) throws IOException {
        taskExecutor = new TaskExecutor(crashHandler, "Server");
        commandRegistry = new CommandRegistry();
        initalControlPanels.forEach(controlPanel -> controlPanels
                .put(controlPanel.id(), controlPanel));
        logJoiner = taskExecutor.runTask(joiner -> {
            try (SystemOutReader logReader = new SystemOutReader()) {
                while (!joiner.marked()) {
                    Optional<String> line = logReader.readLine();
                    if (line.isPresent()) {
                        for (ControlPanel controlPanel : controlPanels
                                .values()) {
                            controlPanel.appendLog(line.get());
                        }
                    } else {
                        List<ControlPanel> closedControlPanels =
                                controlPanels.values().stream()
                                        .filter(ControlPanel::isClosed)
                                        .collect(Collectors.toList());
                        closedControlPanels.forEach(this::removeControlPanel);
                        SleepUtil.sleep(100);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error appending log to control panels: {}",
                        e.toString());
            }
        }, "Log");
        TagStructure serverTag = tagStructure.getStructure("Server");
        maxLoadingRadius = serverTag.getInteger("MaxLoadingRadius");
        controlPanelPassword = serverTag.getString("ControlPanelPassword");
        this.serverInfo = serverInfo;
        serverConnection = new ServerConnection(this, taskExecutor,
                serverTag.getStructure("Socket"));
        worldFormat = new WorldFormat(this, path);
        ScapesServerCommands.register(commandRegistry, this);
        profilerJoiner = taskExecutor.runTask(joiner -> {
            Runtime runtime = Runtime.getRuntime();
            Sync sync = new Sync(5.0, 1000000, false, "Server-Profiler");
            sync.init();
            while (!joiner.marked()) {
                long ram = runtime.totalMemory() - runtime.freeMemory();
                Collection<WorldServer> worlds = worldFormat.worlds();
                Map<String, Double> tps =
                        new ConcurrentHashMap<>(worlds.size());
                worlds.forEach(world -> {
                    Sync worldSync = world.sync();
                    tps.put(world.name(),
                            (double) worldSync.diff() / worldSync.maxDiff());
                });
                controlPanels.values().forEach(controlPanel -> controlPanel
                        .sendProfilerResults(ram, tps));
                sync.cap();
            }
        }, "Profiler");
    }

    public ShutdownReason shutdownReason() {
        return shutdownReason;
    }

    public void addControlPanel(ControlPanel controlPanel) {
        ControlPanel oldControlPanel =
                controlPanels.put(controlPanel.id(), controlPanel);
        if (oldControlPanel != null) {
            oldControlPanel.replaced();
        }
        serverConnection.updateControlPanelPlayers();
        worldFormat.updateControlPanelWorlds();
    }

    public void removeControlPanel(ControlPanel controlPanel) {
        controlPanels.remove(controlPanel.id(), controlPanel);
        serverConnection.updateControlPanelPlayers();
    }

    public Stream<ControlPanel> controlPanels() {
        return controlPanels.values().stream();
    }

    public ServerConnection connection() {
        return serverConnection;
    }

    public WorldFormat worldFormat() {
        return worldFormat;
    }

    public ServerInfo serverInfo() {
        return serverInfo;
    }

    public int maxLoadingRadius() {
        return maxLoadingRadius;
    }

    public String controlPanelPassword() {
        return controlPanelPassword;
    }

    public TaskExecutor taskExecutor() {
        return taskExecutor;
    }

    public CommandRegistry commandRegistry() {
        return commandRegistry;
    }

    public void scheduleStop(ShutdownReason shutdownReason) {
        Thread thread = new Thread(() -> stop(shutdownReason));
        thread.setName("Server-Shutdown");
        thread.start();
    }

    public void stop(ShutdownReason shutdownReason) {
        this.shutdownReason = shutdownReason;
        worldFormat.worldNames().forEach(worldFormat::removeWorld);
        profilerJoiner.join();
        serverConnection.stop();
        logJoiner.join();
        stopped = true;
        taskExecutor.shutdown();
        LOGGER.info("Stopped server");
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
