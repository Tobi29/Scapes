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

package org.tobi29.scapes.plugins

import org.tobi29.scapes.engine.utils.*
import org.tobi29.scapes.engine.utils.io.*
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.tag.json.readJSON
import java.lang.reflect.InvocationTargetException
import java.util.zip.ZipFile

class PluginFile(
        private val path: FilePath?,
        private val checksum: Checksum,
        val id: String,
        val name: String,
        val parent: String,
        val version: Version,
        val scapesVersion: Version,
        val mainClass: String
) {
    fun version(): Version {
        return version
    }

    fun scapesVersion(): Version {
        return scapesVersion
    }

    fun file(): FilePath? {
        return path
    }

    fun id(): String {
        return id
    }

    fun name(): String {
        return name
    }

    fun parent(): String {
        return parent
    }

    fun checksum(): Checksum {
        return checksum
    }

    fun plugin(classLoader: ClassLoader): Plugin {
        try {
            return classLoader.loadClass(
                    mainClass).getConstructor().newInstance() as Plugin
        } catch (e: ClassNotFoundException) {
            throw IOException(e)
        } catch (e: InvocationTargetException) {
            throw IOException(e)
        } catch (e: InstantiationException) {
            throw IOException(e)
        } catch (e: NoSuchMethodException) {
            throw IOException(e)
        } catch (e: IllegalAccessException) {
            throw IOException(e)
        }

    }

    companion object {
        val EMPTY_BYTE = byteArrayOf()

        suspend fun loadFile(path: FilePath): PluginFile {
            val pluginMap = ZipFile(path.toFile()).use { zip ->
                readJSON(BufferedReadChannelStream(Channels.newChannel(
                        zip.getInputStream(zip.getEntry(
                                "scapes/plugin/Plugin.json"))).toChannel()))
            }
            try {
                val checksum = read(path) { checksum(it) }
                val id = pluginMap["ID"].toString()
                val name = pluginMap["Name"].toString()
                val parent = pluginMap["Parent"].toString()
                val version = versionParse(pluginMap["Version"].toString())
                val scapesVersion = versionParse(
                        pluginMap["ScapesVersion"].toString())
                val mainClass = pluginMap["MainClass"].toString()
                return PluginFile(path, checksum, id, name, parent, version,
                        scapesVersion, mainClass)
            } catch (e: VersionException) {
                throw IOException(e.message, e)
            }
        }

        suspend fun load(metaData: ReadSource): PluginFile {
            val pluginMap = metaData.readAsync { readJSON(it) }
            try {
                val path = null
                val checksum = Checksum(Algorithm.UNKNOWN, EMPTY_BYTE)
                val id = pluginMap["ID"].toString()
                val name = pluginMap["Name"].toString()
                val parent = pluginMap["Parent"].toString()
                val version = versionParse(pluginMap["Version"].toString())
                val scapesVersion = versionParse(
                        pluginMap["ScapesVersion"].toString())
                val mainClass = pluginMap["MainClass"].toString()
                return PluginFile(path, checksum, id, name, parent, version,
                        scapesVersion, mainClass)
            } catch (e: VersionException) {
                throw IOException(e.message, e)
            }
        }

        fun load(metaData: ReadSourceLocal): PluginFile {
            val pluginMap = metaData.readNow(::readJSON)
            try {
                val path = null
                val checksum = Checksum(Algorithm.UNKNOWN, EMPTY_BYTE)
                val id = pluginMap["ID"].toString()
                val name = pluginMap["Name"].toString()
                val parent = pluginMap["Parent"].toString()
                val version = versionParse(pluginMap["Version"].toString())
                val scapesVersion = versionParse(
                        pluginMap["ScapesVersion"].toString())
                val mainClass = pluginMap["MainClass"].toString()
                return PluginFile(path, checksum, id, name, parent, version,
                        scapesVersion, mainClass)
            } catch (e: VersionException) {
                throw IOException(e.message, e)
            }
        }
    }
}
