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

package org.tobi29.scapes.server.format.sql

import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer
import org.tobi29.scapes.engine.sql.*
import org.tobi29.scapes.engine.utils.IOException
import org.tobi29.scapes.engine.utils.io.ByteBuffer
import org.tobi29.scapes.engine.utils.io.ByteBufferStream
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.isNotHidden
import org.tobi29.scapes.engine.utils.io.filesystem.isRegularFile
import org.tobi29.scapes.engine.utils.io.filesystem.listRecursive
import org.tobi29.scapes.engine.utils.io.tag.binary.readBinary
import org.tobi29.scapes.engine.utils.io.tag.binary.writeBinary
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toMutTag
import org.tobi29.scapes.engine.utils.tag.toTag
import org.tobi29.scapes.engine.utils.threadLocalRandom
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.PlayerData
import org.tobi29.scapes.server.format.WorldFormat

open class SQLWorldFormat(protected val path: FilePath,
                          database: SQLDatabase,
                          private val useForeignKeys: Boolean = true) : WorldFormat {
    override val plugins: Plugins
    override val playerData: PlayerData
    override val seed: Long
    protected val idStorage: MutableTagMap

    // MetaData
    protected val getMetaData = database.compileQuery("MetaData",
            arrayOf("Value"), "Name")
    protected val replaceMetaData = database.compileReplace("MetaData", "Name",
            "Value")

    // Data
    protected val getData = database.compileQuery("Data", arrayOf("Value"),
            "Name")
    protected val replaceData = database.compileReplace("Data", "Name", "Value")

    // Worlds
    protected val getWorldData = database.compileQuery("Worlds",
            arrayOf("Data"), "World")
    protected val setWorldData = database.compileUpdate("Worlds",
            arrayOf("World"), "Data")
    protected val insertWorld = database.compileInsert("Worlds", "World")
    protected val deleteWorld = database.compileDelete("Worlds", "World")

    // Chunks
    protected val getChunk = database.compileQuery("Chunks", arrayOf("Data"),
            "World", "X", "Y")
    protected val replaceChunk = database.compileReplace("Chunks", "World", "X",
            "Y", "Data")
    protected val deleteWorldChunks = database.compileDelete("Chunks", "World")

    // Players
    protected val getPlayer = database.compileQuery("Players",
            arrayOf("World", "Permissions", "Entity"), "ID")
    protected val insertPlayer = database.compileInsert("Players",
            "ID", "Permissions")
    protected val replacePlayer = database.compileReplace("Players", "ID",
            "World", "Permissions", "Entity")
    protected val deletePlayer = database.compileDelete("Players", "ID")
    protected val checkPlayer = database.compileQuery("Players",
            arrayOf("1"), "ID")
    protected val setWorldPlayers = database.compileUpdate("Players",
            arrayOf("World"), "World")

    private val stream = ByteBufferStream()

    init {
        playerData = SQLPlayerData(getPlayer, insertPlayer, replacePlayer,
                deletePlayer, checkPlayer)
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
                readBinary(ByteBufferStream(ByteBuffer.wrap(array)))
            } else {
                TagMap()
            }
        } else {
            TagMap()
        }.toMutTag()
        plugins = createPlugins()
    }

    open protected fun createPlugins(): Plugins {
        return Plugins(pluginFiles(), idStorage)
    }

    @Synchronized override fun registerWorld(server: ScapesServer,
                                             environmentSupplier: Function1<WorldServer, EnvironmentServer>,
                                             name: String,
                                             seed: Long): WorldServer {
        val format = SQLTerrainInfiniteFormat(getChunk.supply(name),
                replaceChunk.supply(name))
        val world = WorldServer(this, name, seed, server.connection, {
            TerrainInfiniteServer(it, 512, format, it.environment.generator(),
                    arrayOf(it.environment.populator()), it.air)
        }, environmentSupplier)
        val rows = getWorldData(name)
        if (!rows.isEmpty()) {
            val row = rows[0]
            if (row[0] is ByteArray) {
                val array = row[0] as ByteArray
                world.read(readBinary(ByteBufferStream(ByteBuffer.wrap(array))))
            }
        } else {
            insertWorld(arrayOf(name))
        }
        return world
    }

    @Synchronized override fun removeWorld(world: WorldServer) {
        try {
            world.toTag().writeBinary(stream, 1)
            stream.buffer().flip()
            val array = ByteArray(stream.buffer().remaining())
            stream.buffer().get(array)
            stream.buffer().clear()
            setWorldData(arrayOf(world.id), array)
        } catch (e: IOException) {
            logger.error { "Failed to save world info: $e" }
        }
    }

    @Synchronized override fun deleteWorld(name: String): Boolean {
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

    @Synchronized override fun dispose() {
        plugins.dispose()
        idStorage.toTag().writeBinary(stream, 1.toByte())
        stream.buffer().flip()
        val array = ByteArray(stream.buffer().remaining())
        stream.buffer().get(array)
        stream.buffer().clear()
        replaceData(arrayOf("IDs", array))
    }

    open protected fun pluginFiles(): List<PluginFile> {
        val path = this.path.resolve("plugins")
        val files = listRecursive(path,
                { isRegularFile(it) && isNotHidden(it) })
        val plugins = ArrayList<PluginFile>(files.size)
        for (file in files) {
            plugins.add(PluginFile(file))
        }
        return plugins
    }

    companion object : KLogging() {
        fun checkDatabase(database: SQLDatabase) {
            database.createTable("Data", arrayOf("Name"),
                    SQLColumn("Name", SQLType.VARCHAR, "255"),
                    SQLColumn("Value", SQLType.LONGBLOB))
            database.createTable("MetaData", arrayOf("Name"),
                    SQLColumn("Name", SQLType.VARCHAR, "255"),
                    SQLColumn("Value", SQLType.VARCHAR, "255"))
            database.createTable("Worlds", arrayOf("World"),
                    SQLColumn("World", SQLType.VARCHAR, "255"),
                    SQLColumn("Data", SQLType.LONGBLOB))
            database.createTable("Players", arrayOf("ID"),
                    SQLColumn("ID", SQLType.CHAR, "40"),
                    SQLColumn("World", SQLType.VARCHAR, "255",
                            SQLForeignKey("Worlds", "World",
                                    SQLReferentialAction.SET_NULL)),
                    SQLColumn("Permissions", SQLType.INT, notNull = true),
                    SQLColumn("Entity", SQLType.LONGBLOB))
            database.createTable("Chunks", arrayOf("World", "X", "Y"),
                    SQLColumn("World", SQLType.VARCHAR, "255",
                            SQLForeignKey("Worlds", "World",
                                    SQLReferentialAction.CASCADE)),
                    SQLColumn("X", SQLType.INT),
                    SQLColumn("Y", SQLType.INT),
                    SQLColumn("Data", SQLType.LONGBLOB, notNull = true))
        }

        fun initDatabase(database: SQLDatabase,
                         seed: Long) {
            checkDatabase(database)
            database.compileReplace("MetaData", "Name", "Value")(
                    arrayOf("Seed", seed))
        }
    }
}
