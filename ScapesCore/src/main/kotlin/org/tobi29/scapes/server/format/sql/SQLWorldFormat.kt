/*
 * Copyright 2012-2018 Tobi29
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

package org.tobi29.scapes.server.format.sql

import kotlinx.coroutines.experimental.Job
import org.tobi29.io.*
import org.tobi29.io.filesystem.FilePath
import org.tobi29.io.tag.MutableTagMap
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.binary.readBinary
import org.tobi29.io.tag.binary.writeBinary
import org.tobi29.io.tag.toMutTag
import org.tobi29.io.tag.toTag
import org.tobi29.logging.KLogging
import org.tobi29.math.threadLocalRandom
import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.plugins.spi.PluginReference
import org.tobi29.scapes.plugins.spi.refer
import org.tobi29.scapes.plugins.spi.supports
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.PlayerData
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.sql.*
import org.tobi29.utils.VersionException
import org.tobi29.utils.versionParse

class SQLWorldFormat(
    private val path: FilePath,
    database: SQLDatabase,
    private val useForeignKeys: Boolean = true
) : WorldFormat {
    override val plugins: Plugins
    override val playerData: PlayerData
    override val seed: Long
    private val idStorage: MutableTagMap

    // MetaData
    private val getMetaData = database.compileQuery(
        "MetaData", arrayOf("Value"), "Name"
    )
    private val replaceMetaData = database.compileReplace(
        "MetaData", "Name", "Value"
    )

    // Plugins
    private val getPlugins = database.compileQuery(
        "Plugins", arrayOf("ID", "Version"), *emptyArray<String>()
    )
    private val replacePlugins = database.compileReplace(
        "Plugins", "ID", "Version"
    )

    // Data
    private val getData = database.compileQuery(
        "Data", arrayOf("Value"), "Name"
    )
    private val replaceData = database.compileReplace(
        "Data", "Name", "Value"
    )

    // Worlds
    private val getWorldData = database.compileQuery(
        "Worlds",
        arrayOf("Data"), "World"
    )
    private val setWorldData = database.compileUpdate(
        "Worlds",
        arrayOf("World"), "Data"
    )
    private val insertWorld = database.compileInsert(
        "Worlds", "World"
    )
    private val deleteWorld = database.compileDelete(
        "Worlds", "World"
    )

    // Chunks
    private val getChunk = database.compileQuery(
        "Chunks", arrayOf("Data"), "World", "X", "Y"
    )
    private val replaceChunk = database.compileReplace(
        "Chunks", "World", "X", "Y", "Data"
    )
    private val deleteWorldChunks = database.compileDelete(
        "Chunks", "World"
    )

    // Players
    private val getPlayer = database.compileQuery(
        "Players", arrayOf("World", "Permissions", "Entity"), "ID"
    )
    private val insertPlayer = database.compileInsert(
        "Players", "ID", "Permissions"
    )
    private val replacePlayer = database.compileReplace(
        "Players", "ID", "World", "Permissions", "Entity"
    )
    private val deletePlayer = database.compileDelete(
        "Players", "ID"
    )
    private val checkPlayer = database.compileQuery(
        "Players", arrayOf("1"), "ID"
    )
    private val setWorldPlayers = database.compileUpdate(
        "Players", arrayOf("World"), "World"
    )

    private val stream = MemoryViewStreamDefault()

    init {
        playerData = SQLPlayerData(
            getPlayer, insertPlayer, replacePlayer,
            deletePlayer, checkPlayer
        )
        checkDatabase(database)
        var rows = getMetaData("Seed")
        if (!rows.isEmpty()) {
            val row = rows[0]
            try {
                seed = row[0].toString().toLong()
            } catch (e: NumberFormatException) {
                throw IOException("Corrupt seed record: " + e.message)
            }
        } else {
            logger.info { "No seed in database, adding a random one in." }
            seed = threadLocalRandom().nextLong()
            replaceMetaData(arrayOf("Seed", seed))
        }
        rows = getData("IDs")
        idStorage = if (!rows.isEmpty()) {
            val row = rows[0]
            if (row[0] is ByteArray) {
                val array = row[0] as ByteArray
                readBinary(MemoryViewReadableStream(array.viewBE))
            } else {
                TagMap()
            }
        } else {
            TagMap()
        }.toMutTag()
        val resolvedPlugins = plugins().map { reference ->
            Plugins.available().asSequence()
                .filter { it.first.supports(reference) }
                .maxBy { it.first.version }
                    ?: throw IOException("Missing plugin required by save: $reference")
        }
        plugins = Plugins(resolvedPlugins, idStorage)
        replacePlugins(
            *resolvedPlugins.map { (plugin, _) ->
                arrayOf(plugin.id, "${plugin.version}")
            }.toTypedArray()
        )
    }

    @Synchronized
    override fun registerWorld(
        server: ScapesServer,
        environmentSupplier: Function1<WorldServer, EnvironmentServer>,
        name: String,
        seed: Long
    ): WorldServer {
        val format = SQLTerrainInfiniteFormat(
            getChunk.supply(name),
            replaceChunk.supply(name)
        )
        val world = WorldServer(
            this, name, seed, server.connection,
            server.taskExecutor + Job(server.taskExecutor[Job]), {
                TerrainInfiniteServer(
                    it, 512, format, it.environment.generator(),
                    arrayOf(it.environment.populator()), it.air
                )
            }, environmentSupplier
        )
        val rows = getWorldData(name)
        if (!rows.isEmpty()) {
            val row = rows[0]
            if (row[0] is ByteArray) {
                val array = row[0] as ByteArray
                world.read(readBinary(MemoryViewReadableStream(array.viewBE)))
            }
        } else {
            insertWorld(arrayOf(name))
        }
        return world
    }

    @Synchronized
    override fun removeWorld(world: WorldServer) {
        try {
            world.toTag().writeBinary(stream, 1)
            stream.flip()
            val array = ByteArray(stream.remaining())
            stream.get(array.view)
            stream.reset()
            setWorldData(arrayOf(world.id), array)
        } catch (e: IOException) {
            logger.error { "Failed to save world info: $e" }
        }
    }

    @Synchronized
    override fun deleteWorld(name: String): Boolean {
        try {
            deleteWorld.invoke(name)
            if (!useForeignKeys) {
                deleteWorldChunks(name)
                setWorldPlayers(arrayOf(name), null)
            }
        } catch (e: IOException) {
            logger.error { "Error whilst deleting world: $e" }
            return false
        }
        return true
    }

    @Synchronized
    override fun dispose() {
        idStorage.toTag().writeBinary(stream, 1.toByte())
        stream.flip()
        val array = ByteArray(stream.remaining())
        stream.get(array.view)
        stream.reset()
        replaceData(arrayOf("IDs", array))
    }

    private fun plugins(): List<PluginReference> =
        getPlugins().map {
            val id = it[0].toString()
            val version = try {
                versionParse(it[1].toString())
            } catch (e: VersionException) {
                throw IOException(e)
            }
            PluginReference(id, version)
        }

    companion object : KLogging() {
        fun checkDatabase(database: SQLDatabase) {
            database.createTable(
                "Data", arrayOf("Name"),
                SQLColumn("Name", SQLType.VARCHAR, "255"),
                SQLColumn("Value", SQLType.LONGBLOB)
            )
            database.createTable(
                "MetaData", arrayOf("Name"),
                SQLColumn("Name", SQLType.VARCHAR, "255"),
                SQLColumn("Value", SQLType.VARCHAR, "255")
            )
            database.createTable(
                "Plugins", arrayOf("ID", "Version"),
                SQLColumn("ID", SQLType.VARCHAR, "255"),
                SQLColumn("Version", SQLType.VARCHAR, "255")
            )
            database.createTable(
                "Worlds", arrayOf("World"),
                SQLColumn("World", SQLType.VARCHAR, "255"),
                SQLColumn("Data", SQLType.LONGBLOB)
            )
            database.createTable(
                "Players", arrayOf("ID"),
                SQLColumn("ID", SQLType.CHAR, "40"),
                SQLColumn(
                    "World", SQLType.VARCHAR, "255",
                    SQLForeignKey(
                        "Worlds", "World",
                        SQLReferentialAction.SET_NULL
                    )
                ),
                SQLColumn("Permissions", SQLType.INT, notNull = true),
                SQLColumn("Entity", SQLType.LONGBLOB)
            )
            database.createTable(
                "Chunks", arrayOf("World", "X", "Y"),
                SQLColumn(
                    "World", SQLType.VARCHAR, "255",
                    SQLForeignKey(
                        "Worlds", "World",
                        SQLReferentialAction.CASCADE
                    )
                ),
                SQLColumn("X", SQLType.INT),
                SQLColumn("Y", SQLType.INT),
                SQLColumn("Data", SQLType.LONGBLOB, notNull = true)
            )
        }

        fun initDatabase(
            database: SQLDatabase,
            seed: Long,
            plugins: List<PluginReference> = Plugins.available().map { it.first.refer() }
        ) {
            checkDatabase(database)
            database.compileReplace("MetaData", "Name", "Value")(
                arrayOf("Seed", seed)
            )
            database.compileReplace("Plugins", "ID", "Version")(
                *plugins.map {
                    arrayOf(it.id, it.version.toString())
                }.toTypedArray()
            )
        }
    }
}
