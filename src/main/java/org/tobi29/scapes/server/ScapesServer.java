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

import org.tobi29.scapes.engine.server.ServerInfo;
import org.tobi29.scapes.engine.utils.Crashable;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.extension.ServerExtensions;
import org.tobi29.scapes.server.format.WorldFormat;
import org.tobi29.scapes.server.format.basic.BasicWorldFormat;

import java.io.IOException;
import java.nio.file.Path;

public class ScapesServer {
    private final ServerExtensions extensions;
    private final TaskExecutor taskExecutor;
    private final ServerInfo serverInfo;
    private final ServerConnection serverConnection;
    private final WorldFormat worldFormat;
    private final CommandRegistry commandRegistry;
    private final int maxLoadingRadius;
    private boolean stopped;
    private ShutdownReason shutdownReason = ShutdownReason.RUNNING;

    public ScapesServer(Path path, TagStructure tagStructure,
            ServerInfo serverInfo, Crashable crashHandler) throws IOException {
        extensions = new ServerExtensions(this);
        extensions.loadExtensions();
        taskExecutor = new TaskExecutor(crashHandler, "Server");
        commandRegistry = new CommandRegistry();
        TagStructure serverTag = tagStructure.getStructure("Server");
        maxLoadingRadius = serverTag.getInteger("MaxLoadingRadius");
        this.serverInfo = serverInfo;
        serverConnection =
                new ServerConnection(this, serverTag.getStructure("Socket"));
        worldFormat = new BasicWorldFormat(this, path);
        extensions.init();
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

    public WorldFormat worldFormat() {
        return worldFormat;
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
        worldFormat.worldNames().forEach(worldFormat::removeWorld);
        serverConnection.stop();
        taskExecutor.shutdown();
        worldFormat.save();
        worldFormat.plugins().dispose();
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
