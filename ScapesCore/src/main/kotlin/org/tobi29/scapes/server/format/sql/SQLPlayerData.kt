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
package org.tobi29.scapes.server.format.sql

import mu.KLogging
import org.tobi29.scapes.engine.sql.SQLDatabase
import org.tobi29.scapes.engine.utils.io.ByteBufferStream
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.server.PlayerEntry
import org.tobi29.scapes.server.format.PlayerData
import java.io.IOException
import java.nio.ByteBuffer

class SQLPlayerData(database: SQLDatabase,
                    table: String) : PlayerData {
    private val stream = ByteBufferStream()
    private val getPlayer = database.compileQuery(table,
            arrayOf("World", "Permissions", "Entity"), "ID")
    private val insertPlayer = database.compileInsert(table,
            "ID", "Permissions")
    private val replacePlayer = database.compileReplace(table, "ID",
            "World", "Permissions", "Entity")
    private val deletePlayer = database.compileDelete(table, "ID")
    private val checkPlayer = database.compileQuery(table,
            arrayOf("1"), "ID")

    @Synchronized override fun player(id: String): PlayerEntry {
        var permissions = 0
        try {
            val rows = getPlayer(id)
            if (rows.isEmpty()) {
                return PlayerEntry(permissions)
            } else {
                val row = rows[0]
                val worldName = row[0].toString()
                try {
                    permissions = row[1].toString().toInt()
                } catch (e: NumberFormatException) {
                    logger.error { "Corrupt player permission record: $e" }
                }
                if (row[2] is ByteArray) {
                    val array = row[2] as ByteArray
                    val entityStructure = TagStructureBinary.read(
                            ByteBufferStream(ByteBuffer.wrap(array)))
                    return PlayerEntry(permissions, worldName,
                            entityStructure)
                } else {
                    return PlayerEntry(permissions, worldName)
                }
            }
        } catch (e: IOException) {
            logger.error { "Failed to load player: $e" }
            return PlayerEntry(permissions)
        }
    }

    @Synchronized override fun save(id: String,
                                    entity: MobPlayerServer,
                                    permissions: Int) {
        val world = entity.world
        try {
            TagStructureBinary.write(stream, entity.write(false), 1)
            stream.buffer().flip()
            val array = ByteArray(stream.buffer().remaining())
            stream.buffer().get(array)
            stream.buffer().clear()
            replacePlayer(arrayOf(id, world.id, permissions, array))
        } catch (e: IOException) {
            logger.error { "Failed to save player: $e" }
        }

    }

    @Synchronized override fun add(id: String) {
        try {
            insertPlayer(arrayOf(id, 0))
        } catch (e: IOException) {
            logger.error { "Failed to delete player: $e" }
        }
    }

    @Synchronized override fun remove(id: String) {
        try {
            deletePlayer(id)
        } catch (e: IOException) {
            logger.error { "Failed to delete player: $e" }
        }

    }

    override fun playerExists(id: String): Boolean {
        try {
            return checkPlayer(id).isNotEmpty()
        } catch (e: IOException) {
            logger.error { "Failed to load player: $e" }
        }

        return false
    }

    companion object : KLogging()
}
