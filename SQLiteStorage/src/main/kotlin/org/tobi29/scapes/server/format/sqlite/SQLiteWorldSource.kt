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

package org.tobi29.scapes.server.format.sqlite

import kotlinx.coroutines.experimental.runBlocking
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import org.tobi29.scapes.engine.sql.sqlite.SQLiteDatabase
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.graphics.encodePNG
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.newPanorama
import org.tobi29.scapes.server.format.sql.SQLWorldFormat
import java.sql.Connection
import java.sql.SQLException

class SQLiteWorldSource(private val path: FilePath,
                        private val connection: Connection) : WorldSource {
    private val database = SQLiteDatabase(connection)

    constructor(path: FilePath) : this(path, openSave(path))

    override fun init(seed: Long,
                      plugins: List<FilePath>) {
        SQLWorldFormat.initDatabase(database, seed)
        val pluginsDir = path.resolve("plugins")
        createDirectories(pluginsDir)
        for (plugin in plugins) {
            copy(plugin,
                    pluginsDir.resolve(plugin.fileName ?: path("Plugin.jar")))
        }
    }

    override fun panorama(images: WorldSource.Panorama) {
        images.elements.indices.forEach {
            val image = images.elements[it]
            write(path.resolve("Panorama$it.png")) {
                encodePNG(image, it, 9, false)
            }
        }
    }

    override fun panorama(): WorldSource.Panorama? {
        return newPanorama {
            val background = path.resolve("Panorama$it.png")
            if (exists(background)) {
                read(background) { runBlocking { decodePNG(it) } }
            } else {
                return null
            }
        }
    }

    override fun open(server: ScapesServer): WorldFormat {
        return SQLWorldFormat(path, database)
    }

    override fun close() {
        try {
            connection.close()
        } catch (e: SQLException) {
            throw IOException(e)
        }
    }

    companion object {
        fun openSave(path: FilePath): Connection {
            createDirectories(path)
            return openDatabase(path.resolve("Data.db"))
        }

        fun openDatabase(path: FilePath): Connection {
            return openDatabase("jdbc:sqlite:${path.toAbsolutePath().toUri()}")
        }

        fun openDatabase(url: String): Connection {
            try {
                val config = SQLiteConfig()
                config.enforceForeignKeys(true)
                config.setJournalMode(SQLiteConfig.JournalMode.WAL)
                config.setLockingMode(SQLiteConfig.LockingMode.NORMAL)
                val source = SQLiteDataSource(config)
                source.url = url
                return source.connection
            } catch (e: SQLException) {
                throw IOException(e)
            }
        }
    }
}
