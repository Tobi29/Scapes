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
import org.tobi29.scapes.engine.utils.io.BufferedReadChannelStream
import org.tobi29.scapes.engine.utils.io.Channels
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.ReadSource
import org.tobi29.scapes.engine.utils.io.checksum
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.tag.json.readJSON
import java.lang.reflect.InvocationTargetException
import java.util.zip.ZipFile

class PluginFile {
    private val path: FilePath?
    private val checksum: Checksum
    val id: String
    val name: String
    val parent: String
    val mainClass: String
    val version: Version
    val scapesVersion: Version

    constructor(path: FilePath) {
        this.path = path
        checksum = read(path) { checksum(it) }
        try {
            val pluginMap = ZipFile(path.toFile()).use { zip ->
                readJSON(
                        BufferedReadChannelStream(Channels.newChannel(
                                zip.getInputStream(zip.getEntry(
                                        "scapes/plugin/Plugin.json")))))
            }
            id = pluginMap["ID"].toString()
            name = pluginMap["Name"].toString()
            parent = pluginMap["Parent"].toString()
            version = versionParse(pluginMap["Version"].toString())
            scapesVersion = versionParse(
                    pluginMap["ScapesVersion"].toString())
            mainClass = pluginMap["MainClass"].toString()
        } catch (e: VersionException) {
            throw IOException(e.message, e)
        }
    }

    constructor(metaData: ReadSource) {
        val pluginMap = metaData.read(::readJSON)
        try {
            path = null
            checksum = Checksum(Algorithm.UNKNOWN, EMPTY_BYTE)
            id = pluginMap["ID"].toString()
            name = pluginMap["Name"].toString()
            parent = pluginMap["Parent"].toString()
            version = versionParse(pluginMap["Version"].toString())
            scapesVersion = versionParse(
                    pluginMap["ScapesVersion"].toString())
            mainClass = pluginMap["MainClass"].toString()
        } catch (e: VersionException) {
            throw IOException(e.message, e)
        }

    }

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
    }
}
