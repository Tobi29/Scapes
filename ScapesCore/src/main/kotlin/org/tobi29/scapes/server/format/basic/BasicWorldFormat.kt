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

package org.tobi29.scapes.server.format.basic

import mu.KLogging
import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.IDStorage
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.PlayerData
import org.tobi29.scapes.server.format.WorldFormat
import java.io.IOException
import java.security.AccessController
import java.security.PrivilegedAction
import java.security.PrivilegedActionException
import java.util.*

class BasicWorldFormat(private val path: FilePath,
                       map: MutableTagMap) : WorldFormat {
    private val regionPath: FilePath
    private val idStorage: IDStorage
    private val plugins: Plugins
    private val playerData: PlayerData
    private val worldsTagStructure: MutableTagMap
    private val seed: Long

    init {
        val security = System.getSecurityManager()
        security?.checkPermission(
                RuntimePermission("scapes.worldFormat"))
        seed = map["Seed"]?.toLong() ?: 0
        idStorage = IDStorage(map.mapMut("IDs"))
        worldsTagStructure = map.mapMut("Worlds")
        regionPath = path.resolve("region")
        playerData = BasicPlayerData(path.resolve("players"))
        plugins = createPlugins()
    }

    private fun createPlugins(): Plugins {
        return Plugins(pluginFiles(), idStorage)
    }

    override fun idStorage(): IDStorage {
        return idStorage
    }

    override fun playerData(): PlayerData {
        return playerData
    }

    override fun seed(): Long {
        return seed
    }

    override fun plugins(): Plugins {
        return plugins
    }

    @Synchronized override fun registerWorld(server: ScapesServer,
                                             environmentSupplier: Function1<WorldServer, EnvironmentServer>,
                                             name: String,
                                             seed: Long): WorldServer {
        val format = AccessController.doPrivileged(
                PrivilegedAction {
                    try {
                        val worldDirectory = regionPath.resolve(
                                name.toLowerCase())
                        createDirectories(worldDirectory)
                        return@PrivilegedAction BasicTerrainInfiniteFormat(
                                worldDirectory)
                    } catch (e: IOException) {
                        logger.error { "Error whilst creating world: $e" }
                    }
                    null
                }) ?: throw IOException("Unable to create world")
        val world = WorldServer(this, name, seed, server.connection,
                TaskExecutor(server.taskExecutor(), name),
                {
                    TerrainInfiniteServer(it, 512, format,
                            it.environment.generator(),
                            arrayOf(it.environment.populator()), it.air)
                }, environmentSupplier)
        worldsTagStructure[name]?.toMap()?.let { world.read(it) }
        return world
    }

    @Synchronized override fun removeWorld(world: WorldServer) {
        worldsTagStructure[world.id] = TagMap { world.write(this) }
    }

    @Synchronized override fun deleteWorld(name: String): Boolean {
        val worldDirectory = regionPath.resolve(name.toLowerCase())
        if (exists(worldDirectory)) {
            try {
                AccessController.doPrivileged(PrivilegedAction {
                    deleteDir(worldDirectory)
                })
            } catch (e: PrivilegedActionException) {
                logger.error { "Error whilst deleting world: $e" }
                return false
            }

        }
        return true
    }

    override fun dispose() {
        plugins.dispose()
    }

    private fun pluginFiles(): List<PluginFile> {
        val path = this.path.resolve("plugins")
        val files = listRecursive(path,
                { isRegularFile(it) && isNotHidden(it) })
        val plugins = ArrayList<PluginFile>(files.size)
        for (file in files) {
            plugins.add(PluginFile(file))
        }
        return plugins
    }

    companion object : KLogging()
}
