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

package org.tobi29.scapes.server

import mu.KLogging
import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.server.ConnectionManager
import org.tobi29.scapes.engine.server.SSLHandle
import org.tobi29.scapes.engine.utils.Crashable
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.toInt
import org.tobi29.scapes.engine.utils.io.tag.toMap
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.plugins.Dimension
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.command.CommandRegistry
import org.tobi29.scapes.server.connection.ServerConnection
import org.tobi29.scapes.server.extension.ServerExtensions
import org.tobi29.scapes.server.format.PlayerData
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.scapes.server.format.WorldSource
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class ScapesServer(source: WorldSource,
                   configMap: TagMap,
                   val serverInfo: ServerInfo,
                   ssl: SSLHandle,
                   crashHandler: Crashable) {
    val connections: ConnectionManager
    val connection: ServerConnection
    val plugins: Plugins
    val taskExecutor: TaskExecutor
    val seed: Long
    val extensions: ServerExtensions
    val events = EventDispatcher()
    private val format: WorldFormat
    private val playerData: PlayerData
    private val commandRegistry: CommandRegistry
    private val maxLoadingRadius: Int
    private val worlds = ConcurrentHashMap<String, WorldServer>()
    var stopped = false
        private set
    private var shutdownReason = ShutdownReason.RUNNING

    init {
        format = source.open(this)
        playerData = format.playerData()
        plugins = format.plugins()
        seed = format.seed()
        extensions = ServerExtensions(this)
        extensions.loadExtensions(configMap["Extension"]?.toMap())
        taskExecutor = TaskExecutor(crashHandler, "Server")
        commandRegistry = CommandRegistry()
        val serverTag = configMap["Server"]?.toMap()
        val socketTag = serverTag?.get("Socket")?.toMap() ?: TagMap()
        maxLoadingRadius = serverTag?.get("MaxLoadingRadius")?.toInt() ?: 0
        connections = ConnectionManager(taskExecutor, 10)
        connections.workers(socketTag["WorkerCount"]?.toInt() ?: 1)
        connection = ServerConnection(this, socketTag, ssl)
        extensions.init()
        format.plugins().init()
        format.plugins().plugins.forEach { it.initServer(this) }
        format.plugins().dimensions.forEach { this.registerWorld(it) }
    }

    fun shutdownReason(): ShutdownReason {
        return shutdownReason
    }

    fun maxLoadingRadius(): Int {
        return maxLoadingRadius
    }

    fun taskExecutor(): TaskExecutor {
        return taskExecutor
    }

    fun commandRegistry(): CommandRegistry {
        return commandRegistry
    }

    fun world(name: String): WorldServer? {
        return worlds[name]
    }

    fun defaultWorld(): WorldServer? {
        return worlds[plugins.worldType.id()]
    }

    fun registerWorld(dimension: Dimension): WorldServer? {
        return registerWorld({
            dimension.createEnvironment(it)
        }, dimension.id(), seed)
    }

    @Synchronized fun registerWorld(
            environmentSupplier: (WorldServer) -> EnvironmentServer,
            name: String,
            seed: Long): WorldServer? {
        removeWorld(name)
        logger.info { "Adding world: $name" }
        val world: WorldServer
        try {
            world = format.registerWorld(this, environmentSupplier, name, seed)
        } catch (e: IOException) {
            logger.error { "Failed to register world: $e" }
            return null
        }
        world.calculateSpawn()
        worlds.put(name, world)
        world.start()
        return world
    }

    @Synchronized fun removeWorld(name: String): Boolean {
        val world = worlds.remove(name) ?: return false
        stopWorld(world)
        return true
    }

    @Synchronized fun deleteWorld(name: String): Boolean {
        logger.info { "Deleting world: $name" }
        removeWorld(name)
        return format.deleteWorld(name)
    }

    fun getPlayer(id: String): PlayerEntry {
        return playerData.player(id)
    }

    fun savePlayer(id: String,
                   entity: MobPlayerServer,
                   permissions: Int) {
        playerData.save(id, entity, permissions)
    }

    fun addPlayer(id: String) {
        playerData.add(id)
    }

    fun removePlayer(id: String) {
        playerData.remove(id)
    }

    fun playerExists(id: String): Boolean {
        return playerData.playerExists(id)
    }

    fun scheduleStop(shutdownReason: ShutdownReason) {
        this.shutdownReason = shutdownReason
    }

    fun stop(shutdownReason: ShutdownReason) {
        this.shutdownReason = shutdownReason
        stop()
    }

    @Synchronized fun stop() {
        if (stopped) {
            return
        }
        assert(shutdownReason != ShutdownReason.RUNNING)
        stopped = true
        worlds.values.forEach { this.stopWorld(it) }
        connections.stop()
        taskExecutor.shutdown()
        format.dispose()
    }

    fun shouldStop(): Boolean {
        return shutdownReason != ShutdownReason.RUNNING
    }

    private fun stopWorld(world: WorldServer) {
        logger.info { "Removing world: ${world.id}" }
        world.stop(defaultWorld())
        world.dispose()
        format.removeWorld(world)
    }

    enum class ShutdownReason {
        RUNNING,
        STOP,
        RELOAD,
        ERROR
    }

    companion object : KLogging()
}
