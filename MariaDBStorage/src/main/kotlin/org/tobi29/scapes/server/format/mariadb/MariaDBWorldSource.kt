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
package org.tobi29.scapes.server.format.mariadb

import org.mariadb.jdbc.MariaDbDataSource
import org.tobi29.scapes.engine.sql.mysql.MySQLDatabase
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.copy
import org.tobi29.scapes.engine.utils.io.filesystem.createDirectories
import org.tobi29.scapes.engine.utils.IOException
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.sql.SQLWorldFormat
import java.sql.Connection
import java.sql.SQLException

class MariaDBWorldSource(private val path: FilePath,
                         private val connection: Connection) : WorldSource {
    private val database: MySQLDatabase

    constructor(path: FilePath,
                url: String,
                user: String,
                password: String) : this(path,
            openDatabase(url, user, password))

    init {
        database = MySQLDatabase(connection)
    }

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
    }

    override fun panorama(): WorldSource.Panorama? {
        return null
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
        fun openDatabase(url: String,
                         user: String,
                         password: String): Connection {
            try {
                val source = MariaDbDataSource(url)
                return source.getConnection(user, password)
            } catch (e: SQLException) {
                throw IOException(e)
            }
        }
    }
}
