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

package org.tobi29.scapes.plugins

import org.tobi29.scapes.engine.utils.Checksum
import org.tobi29.scapes.engine.utils.Version
import org.tobi29.scapes.engine.utils.VersionException
import org.tobi29.scapes.engine.utils.io.Algorithm
import org.tobi29.scapes.engine.utils.io.BufferedReadChannelStream
import org.tobi29.scapes.engine.utils.io.checksum
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.ReadSource
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.filesystem.zipFile
import org.tobi29.scapes.engine.utils.io.tag.json.TagStructureJSON
import org.tobi29.scapes.engine.utils.versionParse
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.channels.Channels

class PluginFile {
    private val path: FilePath?
    private val checksum: Checksum
    private val id: String
    private val name: String
    private val parent: String
    private val mainClass: String
    private val version: Version
    private val scapesVersion: Version

    constructor(path: FilePath) {
        this.path = path
        checksum = read(path) { checksum(it) }
        try {
            val tagStructure =
                    zipFile(path).use { zip ->
                        TagStructureJSON.read(
                                BufferedReadChannelStream(Channels.newChannel(
                                        zip.getInputStream(
                                                zip.getEntry("Plugin.json")))))
                    }
            id = tagStructure.getString("ID") ?: ""
            name = tagStructure.getString("Name") ?: ""
            parent = tagStructure.getString("Parent") ?: ""
            version = versionParse(tagStructure.getString("Version") ?: "0.0.0")
            scapesVersion = versionParse(
                    tagStructure.getString("ScapesVersion") ?: "0.0.0")
            mainClass = tagStructure.getString("MainClass") ?: ""
        } catch (e: VersionException) {
            throw IOException(e.message, e)
        }
    }

    constructor(metaData: ReadSource) {
        val tagStructure = metaData.read { TagStructureJSON.read(it) }
        try {
            path = null
            checksum = Checksum(Algorithm.UNKNOWN, EMPTY_BYTE)
            id = tagStructure.getString("ID") ?: ""
            name = tagStructure.getString("Name") ?: ""
            parent = tagStructure.getString("Parent") ?: ""
            version = versionParse(tagStructure.getString("Version") ?: "0.0.0")
            scapesVersion = versionParse(
                    tagStructure.getString("ScapesVersion") ?: "0.0.0")
            mainClass = tagStructure.getString("MainClass") ?: ""
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
