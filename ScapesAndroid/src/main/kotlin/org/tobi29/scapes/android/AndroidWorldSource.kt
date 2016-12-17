package org.tobi29.scapes.android

import android.content.Context
import org.tobi29.scapes.engine.android.sqlite.AndroidSQLite
import org.tobi29.scapes.engine.android.sqlite.AndroidSQLiteOpenHelper
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.graphics.encodePNG
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathResource
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.newPanorama
import org.tobi29.scapes.server.format.sql.SQLWorldFormat

class AndroidWorldSource(context: Context, private val path: FilePath) : WorldSource {
    private val database: AndroidSQLite

    init {
        createDirectories(path)
        val helper = AndroidSQLiteOpenHelper(context, path.resolve("Data.db"))
        val db = helper.writableDatabase
        db.setForeignKeyConstraintsEnabled(true)
        database = AndroidSQLite(db)
    }

    override fun init(seed: Long,
                      plugins: List<FilePath>) {
        SQLWorldFormat.initDatabase(database, seed)
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
