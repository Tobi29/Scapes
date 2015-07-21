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

package org.tobi29.scapes.server.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.Crashable;
import org.tobi29.scapes.engine.utils.io.filesystem.CrashReportFile;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureJSON;
import org.tobi29.scapes.server.ControlPanel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.controlpanel.ServerInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ScapesStandaloneServer
        implements Crashable, Command.Executor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesStandaloneServer.class);
    private static final Runtime RUNTIME = Runtime.getRuntime();
    protected final Path path;
    protected ScapesServer server;
    private final Thread shutdownHook = new Thread(() -> {
        try {
            stopServer();
        } catch (IOException e) {
            LOGGER.error("Failed to terminate server: {}", e.toString());
        }
    });

    protected ScapesStandaloneServer(Path path) {
        this.path = path;
    }

    public abstract int run() throws IOException;

    protected void start() throws IOException {
        start(Collections.emptyList());
    }

    protected void start(Collection<ControlPanel> initialControlPanels)
            throws IOException {
        RUNTIME.addShutdownHook(shutdownHook);
        TagStructure tagStructure;
        tagStructure = loadConfig(path.resolve("Server.json"));
        TagStructure serverTag = tagStructure.getStructure("Server");
        ServerInfo serverInfo =
                new ServerInfo(serverTag.getString("ServerName"),
                        path.resolve(serverTag.getString("ServerIcon")));
        server =
                new ScapesServer(path.resolve("data"), tagStructure, serverInfo,
                        this, initialControlPanels);
        ServerConnection connection = server.connection();
        connection.setAllowsCreation(
                tagStructure.getBoolean("AllowAccountCreation"));
        server.connection().start(tagStructure.getInteger("ServerPort"));
    }

    private ScapesServer.ShutdownReason stopServer() throws IOException {
        if (server == null) {
            return ScapesServer.ShutdownReason.STOP;
        }
        server.worldFormat().save();
        ScapesServer server = this.server;
        this.server = null;
        return server.shutdownReason();
    }

    protected ScapesServer.ShutdownReason stop() throws IOException {
        ScapesServer.ShutdownReason shutdownReason = stopServer();
        RUNTIME.removeShutdownHook(shutdownHook);
        return shutdownReason;
    }

    protected TagStructure loadConfig(Path path) throws IOException {
        if (Files.exists(path)) {
            return FileUtil.readReturn(path, TagStructureJSON::read);
        }
        TagStructure tagStructure = new TagStructure();
        tagStructure.setInteger("ServerPort", 12345);
        tagStructure.setBoolean("AllowAccountCreation", true);
        TagStructure serverTag = tagStructure.getStructure("Server");
        serverTag.setString("ServerName", "My Superb Server");
        serverTag.setString("ServerIcon", "ServerIcon.png");
        serverTag.setInteger("MaxLoadingRadius", 256);
        serverTag.setString("ControlPanelPassword", "");
        TagStructure socketTag = serverTag.getStructure("Socket");
        socketTag.setInteger("MaxPlayers", 20);
        socketTag.setInteger("WorkerCount", 2);
        socketTag.setInteger("RSASize", 2048);
        FileUtil.write(path,
                streamOut -> TagStructureJSON.write(tagStructure, streamOut));
        return tagStructure;
    }

    @Override
    @SuppressWarnings("CallToSystemExit")
    public void crash(Throwable e) {
        LOGGER.error("Stopping due to a crash", e);
        Map<String, String> debugValues = new ConcurrentHashMap<>();
        try {
            CrashReportFile.writeCrashReport(e, CrashReportFile.file(path),
                    "ScapesServer", debugValues);
        } catch (IOException e1) {
            LOGGER.warn("Failed to write crash report: {}", e1.toString());
        }
        System.exit(1);
    }

    @Override
    public String name() {
        return "Server";
    }
}
