/*
 * Copyright 2012-2016 Tobi29
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

import mu.KLogging
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.server.SSLHandle
import org.tobi29.scapes.engine.server.SSLProvider
import org.tobi29.scapes.engine.utils.Crashable
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.ListenerOwnerHandle
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getInt
import org.tobi29.scapes.engine.utils.io.tag.json.TagStructureJSON
import org.tobi29.scapes.engine.utils.io.tag.setInt
import org.tobi29.scapes.engine.utils.io.use
import org.tobi29.scapes.engine.utils.task.Joiner
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.spi.WorldSourceProvider
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class ScapesStandaloneServer(protected val config: FilePath) : Crashable {
    protected val taskExecutor = TaskExecutor(this, "Server-Shell")
    private val joinable = Joiner.BasicJoinable()
    private val shutdownHook = Thread { joinable.joiner.join() }
    protected lateinit var server: ScapesServer

    protected abstract fun init(executor: Executor): () -> Unit

    @Throws(IOException::class)
    fun run(path: FilePath) {
        RUNTIME.addShutdownHook(shutdownHook)
        try {
            while (!joinable.marked) {
                val tagStructure = loadConfig(config.resolve("Server.json"))
                val keyManagerConfig = tagStructure.structure("KeyManager")
                val keyManagerProvider = loadKeyManager(
                        keyManagerConfig.getString("ID") ?: "")
                val ssl = SSLProvider.sslHandle(
                        keyManagerProvider[config, keyManagerConfig])
                val worldSourceConfig = tagStructure.structure("WorldSource")
                val worldSourceProvider = loadWorldSource(
                        worldSourceConfig.getString("ID") ?: "")
                worldSourceProvider[path, worldSourceConfig, taskExecutor].use { source ->
                    val loop = start(source, tagStructure, ssl)
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

    @Throws(IOException::class)
    protected fun start(source: WorldSource,
                        tagStructure: TagStructure,
                        ssl: SSLHandle): () -> Unit {
        val serverTag = tagStructure.structure("Server")
        val serverInfo = ServerInfo(serverTag.getString("ServerName") ?: "",
                config.resolve(serverTag.getString("ServerIcon") ?: ""))
        server = ScapesServer(source, tagStructure, serverInfo, ssl, this)
        val connection = server.connection
        val executor = object : Executor {
            override val events = EventDispatcher()
            override val listenerOwner = ListenerOwnerHandle { !server.stopped }

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
                tagStructure.getBoolean("AllowAccountCreation") ?: false)
        server.connection.start(tagStructure.getInt("ServerPort") ?: -1)
        return init(executor)
    }

    @Throws(IOException::class)
    private fun loadConfig(path: FilePath): TagStructure {
        if (exists(path)) {
            return read(path, { TagStructureJSON.read(it) })
        }
        val tagStructure = TagStructure()
        tagStructure.setInt("ServerPort", 12345)
        tagStructure.setBoolean("AllowAccountCreation", true)
        val serverTag = tagStructure.structure("Server")
        serverTag.setString("ServerName", "My Superb Server")
        serverTag.setString("ServerIcon", "ServerIcon.png")
        serverTag.setInt("MaxLoadingRadius", 256)
        val socketTag = serverTag.structure("Socket")
        socketTag.setInt("MaxPlayers", 20)
        socketTag.setString("ControlPassword", "")
        socketTag.setInt("WorkerCount", 2)
        val sourceTag = tagStructure.structure("WorldSource")
        sourceTag.setString("ID", "Basic")
        val keyTag = tagStructure.structure("KeyManager")
        keyTag.setString("ID", "Dummy")
        write(path) { streamOut ->
            TagStructureJSON.write(tagStructure, streamOut)
        }
        return tagStructure
    }

    override fun crash(e: Throwable) {
        logger.error(e) { "Stopping due to a crash" }
        val debugValues = ConcurrentHashMap<String, String>()
        try {
            writeCrashReport(e, file(config), "ScapesServer", debugValues)
        } catch (e1: IOException) {
            logger.warn { "Failed to write crash report: $e1" }
        }
        System.exit(1)
    }

    companion object : KLogging() {
        private val RUNTIME = Runtime.getRuntime()

        @Throws(IOException::class)
        private fun loadWorldSource(id: String): WorldSourceProvider {
            for (provider in ServiceLoader.load(
                    WorldSourceProvider::class.java)) {
                try {
                    if (provider.available() && id == provider.configID()) {
                        logger.debug { "Loaded world source: ${provider.javaClass.name}" }
                        return provider
                    }
                } catch (e: ServiceConfigurationError) {
                    logger.warn { "Unable to load world source provider: $e" }
                }

            }
            throw IOException("No world source found for: " + id)
        }

        @Throws(IOException::class)
        private fun loadKeyManager(id: String): KeyManagerProvider {
            for (provider in ServiceLoader.load(
                    KeyManagerProvider::class.java)) {
                try {
                    if (provider.available() && id == provider.configID()) {
                        logger.debug { "Loaded key manager: ${provider.javaClass.name}" }
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
