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

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.tobi29.logging.KLogging
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.server.SSLHandle
import org.tobi29.utils.EventDispatcher
import org.tobi29.utils.ListenerRegistrar
import org.tobi29.io.IOException
import org.tobi29.io.filesystem.FilePath
import org.tobi29.io.filesystem.exists
import org.tobi29.io.filesystem.read
import org.tobi29.io.filesystem.write
import org.tobi29.io.tag.json.readJSON
import org.tobi29.io.tag.json.writeJSON
import org.tobi29.io.use
import org.tobi29.utils.sleep
import org.tobi29.utils.sleepNanos
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.spi.WorldSourceProvider
import org.tobi29.scapes.server.ssl.spi.KeyManagerProvider
import org.tobi29.io.tag.*
import org.tobi29.stdex.atomic.AtomicBoolean
import java.util.*
import kotlin.collections.set

abstract class ScapesStandaloneServer(
        protected val taskExecutor: CoroutineDispatcher,
        protected val config: FilePath
) {
    protected lateinit var server: ScapesServer

    protected abstract fun ListenerRegistrar.listeners()

    protected abstract fun init(executor: Executor): () -> Unit

    // TODO: @Throws(IOException::class)
    fun run(path: FilePath) {
        val stopped = AtomicBoolean(false)
        val running = AtomicBoolean(true)
        val shutdownHook = Thread {
            running.set(false)
            while (!stopped.get()) {
                sleepNanos(1000L)
            }
        }
        RUNTIME.addShutdownHook(shutdownHook)
        try {
            while (running.get()) {
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
                    while (running.get() && !server.shouldStop()) {
                        loop()
                        sleep(100L)
                    }
                    if (!running.get()) {
                        server.scheduleStop(ScapesServer.ShutdownReason.STOP)
                    }
                    server.stop()
                }
                if (server.shutdownReason() != ScapesServer.ShutdownReason.RELOAD) {
                    break
                }
            }
        } finally {
            stopped.set(true)
            try {
                RUNTIME.removeShutdownHook(shutdownHook)
            } catch (e: IllegalStateException) {
            }
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
