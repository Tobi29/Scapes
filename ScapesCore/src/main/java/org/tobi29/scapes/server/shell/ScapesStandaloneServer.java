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

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.server.SSLHandle;
import org.tobi29.scapes.engine.server.SSLProvider;
import org.tobi29.scapes.engine.server.ServerInfo;
import org.tobi29.scapes.engine.utils.Crashable;
import org.tobi29.scapes.engine.utils.io.filesystem.CrashReportFile;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.json.TagStructureJSON;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.spi.WorldSourceProvider;
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider;

import java.io.IOException;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ScapesStandaloneServer
        implements Crashable, Command.Executor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesStandaloneServer.class);
    private static final Runtime RUNTIME = Runtime.getRuntime();
    protected final FilePath config;
    protected final TaskExecutor taskExecutor =
            new TaskExecutor(this, "Server-Shell");
    private final Joiner.Joinable joinable = new Joiner.Joinable();
    private final Thread shutdownHook =
            new Thread(() -> joinable.joiner().join());
    protected ScapesServer server;

    protected ScapesStandaloneServer(FilePath config) {
        this.config = config;
    }

    private static WorldSourceProvider loadWorldSource(String id)
            throws IOException {
        for (WorldSourceProvider provider : ServiceLoader
                .load(WorldSourceProvider.class)) {
            try {
                if (provider.available() && id.equals(provider.configID())) {
                    LOGGER.debug("Loaded world source: {}",
                            provider.getClass().getName());
                    return provider;
                }
            } catch (ServiceConfigurationError e) {
                LOGGER.warn("Unable to load world source provider: {}",
                        e.toString());
            }
        }
        throw new IOException("No world source found for: " + id);
    }

    private static KeyManagerProvider loadKeyManager(String id)
            throws IOException {
        for (KeyManagerProvider provider : ServiceLoader
                .load(KeyManagerProvider.class)) {
            try {
                if (provider.available() && id.equals(provider.configID())) {
                    LOGGER.debug("Loaded key manager: {}",
                            provider.getClass().getName());
                    return provider;
                }
            } catch (ServiceConfigurationError e) {
                LOGGER.warn("Unable to load key manager provider: {}",
                        e.toString());
            }
        }
        throw new IOException("No key manager found for: " + id);
    }

    protected abstract Runnable loop();

    public void run(FilePath path) throws IOException {
        RUNTIME.addShutdownHook(shutdownHook);
        try {
            while (!joinable.marked()) {
                TagStructure tagStructure =
                        loadConfig(config.resolve("Server.json"));
                TagStructure keyManagerConfig =
                        tagStructure.getStructure("KeyManager");
                KeyManagerProvider keyManagerProvider =
                        loadKeyManager(keyManagerConfig.getString("ID"));
                SSLHandle ssl = SSLProvider.sslHandle(
                        keyManagerProvider.get(config, keyManagerConfig));
                TagStructure worldSourceConfig =
                        tagStructure.getStructure("WorldSource");
                WorldSourceProvider worldSourceProvider =
                        loadWorldSource(worldSourceConfig.getString("ID"));
                try (WorldSource source = worldSourceProvider
                        .get(path, worldSourceConfig, taskExecutor)) {
                    start(source, tagStructure, ssl);
                    Runnable loop = loop();
                    while (!server.shouldStop()) {
                        loop.run();
                        joinable.sleep(100);
                        if (joinable.marked()) {
                            server.scheduleStop(
                                    ScapesServer.ShutdownReason.STOP);
                        }
                    }
                    server.stop();
                }
                if (server.shutdownReason() !=
                        ScapesServer.ShutdownReason.RELOAD) {
                    break;
                }
            }
            taskExecutor.shutdown();
        } finally {
            try {
                RUNTIME.removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
            }
            joinable.join();
        }
    }

    protected void start(WorldSource source, TagStructure tagStructure,
            SSLHandle ssl) throws IOException {
        TagStructure serverTag = tagStructure.getStructure("Server");
        ServerInfo serverInfo =
                new ServerInfo(serverTag.getString("ServerName"),
                        config.resolve(serverTag.getString("ServerIcon")));
        server = new ScapesServer(source, tagStructure, serverInfo, ssl, this);
        ServerConnection connection = server.connection();
        connection.addExecutor(this);
        connection.setAllowsCreation(
                tagStructure.getBoolean("AllowAccountCreation"));
        server.connection().start(tagStructure.getInteger("ServerPort"));
    }

    private TagStructure loadConfig(FilePath path) throws IOException {
        if (FileUtil.exists(path)) {
            return FileUtil.readReturn(path, TagStructureJSON::read);
        }
        TagStructure tagStructure = new TagStructure();
        tagStructure.setInteger("ServerPort", 12345);
        tagStructure.setBoolean("AllowAccountCreation", true);
        TagStructure serverTag = tagStructure.getStructure("Server");
        serverTag.setString("ServerName", "My Superb Server");
        serverTag.setString("ServerIcon", "ServerIcon.png");
        serverTag.setInteger("MaxLoadingRadius", 256);
        TagStructure socketTag = serverTag.getStructure("Socket");
        socketTag.setInteger("MaxPlayers", 20);
        socketTag.setString("ControlPassword", "");
        socketTag.setInteger("WorkerCount", 2);
        TagStructure sourceTag = tagStructure.getStructure("WorldSource");
        sourceTag.setString("ID", "Basic");
        TagStructure keyTag = tagStructure.getStructure("KeyManager");
        keyTag.setString("ID", "Dummy");
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
            CrashReportFile.writeCrashReport(e, CrashReportFile.file(config),
                    "ScapesServer", debugValues);
        } catch (IOException e1) {
            LOGGER.warn("Failed to write crash report: {}", e1.toString());
        }
        System.exit(1);
    }

    @Override
    public Optional<String> playerName() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "Server";
    }

    @Override
    public int permissionLevel() {
        return 10;
    }
}
