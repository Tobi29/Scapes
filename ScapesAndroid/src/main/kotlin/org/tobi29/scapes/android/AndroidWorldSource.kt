package org.tobi29.scapes.android

import android.content.Context
import org.tobi29.scapes.engine.android.sqlite.AndroidSQLite
import org.tobi29.scapes.engine.android.sqlite.AndroidSQLiteOpenHelper
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathResource
import org.tobi29.scapes.engine.utils.io.filesystem.createDirectories
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.sql.SQLWorldFormat

class AndroidWorldSource(context: Context, private val path: FilePath) : WorldSource {
    private val database: AndroidSQLite

    init {
        createDirectories(path)
        val helper = AndroidSQLiteOpenHelper(context, path.resolve("Data.db"))
        database = AndroidSQLite(helper.writableDatabase)
    }

    override fun init(seed: Long,
                      plugins: List<FilePath>) {
        SQLWorldFormat.initDatabase(database, seed)
    }

    override fun panorama(images: WorldSource.Panorama) {
    }

    override fun panorama(): WorldSource.Panorama? {
        return null
    }

    override fun open(server: ScapesServer): WorldFormat {
        return object : SQLWorldFormat(path, database) {
            override fun createPlugins(): Plugins {
                return Plugins(emptyList<PluginFile>(), idStorage)
            }

            override fun pluginFiles(): List<PluginFile> {
                return listOf(PluginFile(
                        ClasspathResource(javaClass.classLoader,
                                "Plugin.json")))
            }
        }
    }

    override fun close() {
        database.dispose()
    }
}
