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

package org.tobi29.scapes.server.format.sqljet

import org.tmatesoft.sqljet.core.SqlJetException
import org.tmatesoft.sqljet.core.table.SqlJetDb
import org.tobi29.scapes.engine.sql.sqljet.SQLJetDatabase
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.graphics.encodePNG
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.newPanorama
import org.tobi29.scapes.server.format.sql.SQLWorldFormat
import java.io.File
import java.sql.SQLException

class SQLJetWorldSource(private val path: FilePath,
                        private val connection: SqlJetDb) : WorldSource {
    private val database = SQLJetDatabase(connection)

    constructor(path: FilePath) : this(path, openSave(path))

    override fun init(seed: Long,
                      plugins: List<FilePath>) {
        SQLWorldFormat.initDatabase(database, seed)
        val pluginsDir = path.resolve("plugins")
        createDirectories(pluginsDir)
        for (plugin in plugins) {
            copy(plugin, pluginsDir.resolve(plugin.fileName))
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
                read(background) { decodePNG(it) }
            } else {
                return null
            }
        }
    }

    override fun open(server: ScapesServer): WorldFormat {
        return SQLWorldFormat(path, database, false)
    }

    override fun close() {
        try {
            connection.close()
        } catch (e: SQLException) {
            throw IOException(e)
        }
    }

    companion object {
        fun openSave(path: FilePath): SqlJetDb {
            createDirectories(path)
            return openDatabase(path.resolve("Data.db"))
        }

        fun openDatabase(path: FilePath): SqlJetDb {
            try {
                val connection = SqlJetDb.open(File(path.toUri()), true)
                return connection
            } catch (e: SqlJetException) {
                throw IOException(e)
            }
        }
    }
}
