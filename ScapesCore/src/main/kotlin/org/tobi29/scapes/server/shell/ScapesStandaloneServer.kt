/*
 * Copyright 2012-2017 Tobi29
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
package org.tobi29.scapes.server.shell

import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.server.SSLHandle
import org.tobi29.scapes.engine.utils.Crashable
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.ListenerRegistrar
import org.tobi29.scapes.engine.utils.io.*
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.exists
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.filesystem.write
import org.tobi29.scapes.engine.utils.io.tag.json.readJSON
import org.tobi29.scapes.engine.utils.io.tag.json.writeJSON
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.engine.utils.task.BasicJoinable
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.spi.WorldSourceProvider
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider
import java.util.*
import kotlin.system.exitProcess

abstract class ScapesStandaloneServer(protected val config: FilePath) : Crashable {
    protected val taskExecutor = TaskExecutor(this, "Server-Shell")
    private val joinable = BasicJoinable()
    private val shutdownHook = Thread { joinable.joiner.join() }
    protected lateinit var server: ScapesServer

    protected abstract fun ListenerRegistrar.listeners()

    protected abstract fun init(executor: Executor): () -> Unit

    // TODO: @Throws(IOException::class)
    fun run(path: FilePath) {
        RUNTIME.addShutdownHook(shutdownHook)
        try {
            while (!joinable.marked) {
                val configMap = loadConfig(config.resolve("Server.json"))
                val keyManagerConfig = configMap["KeyManager"]?.toMap() ?: TagMap()
                val keyManagerProvider = loadKeyManager(
                        keyManagerConfig["ID"].toString())
                val ssl = SSLHandle(
                        keyManagerProvider[config, keyManagerConfig])
                val worldSourceConfig = configMap["WorldSource"]?.toMap() ?: TagMap()
                val worldSourceProvider = loadWorldSource(
                        worldSourceConfig["ID"].toString())
                worldSourceProvider[path, worldSourceConfig, taskExecutor].use { source ->
                    val loop = start(source, configMap, ssl)
                    while (!server.shouldStop()) {
                        loop()
                        joinable.sleep(100)
                        if (joinable.marked) {
                            server.scheduleStop(
                                    ScapesServer.ShutdownReason.STOP)
                        }
                    }
                    server.stop()
                }
                if (server.shutdownReason() !== ScapesServer.ShutdownReason.RELOAD) {
                    break
                }
            }
            taskExecutor.shutdown()
        } finally {
            try {
                RUNTIME.removeShutdownHook(shutdownHook)
            } catch (e: IllegalStateException) {
            }

            joinable.join()
        }
    }

    // TODO: @Throws(IOException::class)
    protected fun start(source: WorldSource,
                        configMap: TagMap,
                        ssl: SSLHandle): () -> Unit {
        val serverTag = configMap["Server"]?.toMap()
        val serverInfo = ServerInfo(serverTag?.get("ServerName").toString(),
                config.resolve(serverTag?.get("ServerIcon").toString()))
        server = ScapesServer(source, configMap, serverInfo, ssl, taskExecutor)
        val connection = server.connection
        val executor = object : Executor {
            override val events = EventDispatcher(
                    server.events) { listeners() }.apply { enable() }

            override fun playerName(): String? {
                return null
            }

            override fun name(): String {
                return "Server"
            }

            override fun permissionLevel(): Int {
                return 10
            }
        }
        connection.addExecutor(executor)
        connection.setAllowsCreation(
                configMap["AllowAccountCreation"]?.toBoolean() ?: false)
        server.connection.start(configMap["ServerPort"]?.toInt() ?: -1)
        return init(executor)
    }

    // TODO: @Throws(IOException::class)
    private fun loadConfig(path: FilePath): TagMap {
        if (exists(path)) {
            return read(path, ::readJSON)
        }
        val map = TagMap {
            this["ServerPort"] = 12345.toTag()
            this["AllowAccountCreation"] = true.toTag()
            this["Server"] = TagMap {
                this["ServerName"] = "My Superb Server".toTag()
                this["ServerIcon"] = "ServerIcon.png".toTag()
                this["MaxLoadingRadius"] = 288.toTag()
                this["Socket"] = TagMap {
                    this["MaxPlayers"] = 20.toTag()
                    this["WorkerCount"] = 2.toTag()
                    this["ControlPassword"] = "".toTag()
                }
            }
            this["WorldSource"] = TagMap {
                this["ID"] = "SQLite".toTag()
            }
            this["KeyManager"] = TagMap {
                this["ID"] = "Dummy".toTag()
            }
        }
        write(path) { map.writeJSON(it) }
        return map
    }

    override fun crash(e: Throwable): Nothing {
        logger.error(e) { "Stopping due to a crash" }
        try {
            crashReport(config, e)
        } catch (e1: IOException) {
            logger.warn { "Failed to write crash report: $e1" }
        }
        exitProcess(1)
    }

    companion object : KLogging() {
        private val RUNTIME = Runtime.getRuntime()

        // TODO: @Throws(IOException::class)
        private fun loadWorldSource(id: String): WorldSourceProvider {
            for (provider in ServiceLoader.load(
                    WorldSourceProvider::class.java)) {
                try {
                    if (provider.available() && id == provider.configID()) {
                        logger.debug { "Loaded world source: ${provider::class.java.name}" }
                        return provider
                    }
                } catch (e: ServiceConfigurationError) {
                    logger.warn { "Unable to load world source provider: $e" }
                }

            }
            throw IOException("No world source found for: " + id)
        }

        // TODO: @Throws(IOException::class)
        private fun loadKeyManager(id: String): KeyManagerProvider {
            for (provider in ServiceLoader.load(
                    KeyManagerProvider::class.java)) {
                try {
                    if (provider.available() && id == provider.configID()) {
                        logger.debug { "Loaded key manager: ${provider::class.java.name}" }
                        return provider
                    }
                } catch (e: ServiceConfigurationError) {
                    logger.warn { "Unable to load key manager provider: $e" }
                }

            }
            throw IOException("No key manager found for: $id")
        }
    }
}

private val FORMATTER = DateTimeFormatter.ISO_DATE_TIME
private val FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

private fun crashReport(path: FilePath,
                        e: Throwable) {
    val time = OffsetDateTime.now()
    val crashReportFile = path.resolve(
            crashReportName(time.format(FILE_FORMATTER)))
    write(crashReportFile) {
        it.writeCrashReport(e, "Scapes Server",
                crashReportSectionStacktrace(e),
                crashReportSectionActiveThreads(),
                crashReportSectionSystemProperties(),
                crashReportSectionTime(
                        LocalDateTime.from(time).format(FORMATTER)))
    }
}
