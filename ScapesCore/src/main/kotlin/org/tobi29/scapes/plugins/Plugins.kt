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

import mu.KLogging
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.init
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPath
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathResource
import org.tobi29.scapes.engine.utils.io.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.readOnly
import java.io.IOException
import java.net.URLClassLoader
import java.util.*

class Plugins @Throws(IOException::class)
constructor(files: List<PluginFile>,
            idStorage: MutableTagMap) {
    val files = files.readOnly()
    private val pluginsMut = ArrayList<Plugin>()
    val plugins = pluginsMut.readOnly()
    private val dimensionsMut = ArrayList<Dimension>()
    val dimensions = dimensionsMut.readOnly()
    private val registry: GameRegistry
    private val classLoader: URLClassLoader?
    private var worldTypeMut: WorldType? = null
    val worldType: WorldType
        get() = worldTypeMut ?: throw IllegalStateException(
                "No world type loaded")
    private var init = false

    init {
        val paths = files.asSequence().mapNotNull { it.file() }.toList()
        if (paths.isEmpty()) {
            classLoader = null
            val classLoader = Plugins::class.java.classLoader
            val file = PluginFile(
                    ClasspathResource(classLoader, "Plugin.json"))
            load(file.plugin(classLoader))
        } else {
            classLoader = PluginClassLoader(paths)
            for (file in files) {
                load(file.plugin(classLoader))
            }
        }
        if (worldTypeMut == null) {
            throw IOException("No world type found")
        }
        registry = GameRegistry(idStorage)
    }

    @Throws(IOException::class)
    private fun load(plugin: Plugin) {
        pluginsMut.add(plugin)
        if (plugin is Dimension) {
            dimensionsMut.add(plugin)
        }
        if (plugin is WorldType) {
            if (worldTypeMut != null) {
                throw IOException("Found 2nd world type: " + plugin)
            }
            worldTypeMut = plugin
        }
    }

    fun dispose() {
        pluginsMut.clear()
        dimensionsMut.clear()
        worldTypeMut = null
        if (classLoader != null) {
            try {
                classLoader.close()
            } catch (e: IOException) {
                logger.error { "Failed to close plugin classloader: $e" }
            }
        }
    }

    fun registry(): GameRegistry {
        return registry
    }

    fun plugin(name: String): Plugin {
        for (plugin in pluginsMut) {
            if (plugin.id() == name) {
                return plugin
            }
        }
        throw IllegalArgumentException("Unknown plugin")
    }

    fun addFileSystems(files: FileSystemContainer) {
        for (plugin in pluginsMut) {
            files.registerFileSystem(plugin.id(),
                    ClasspathPath(plugin::class.java.classLoader,
                            plugin.assetRoot()))
        }
    }

    fun removeFileSystems(files: FileSystemContainer) {
        for (plugin in pluginsMut) {
            files.removeFileSystem(plugin.id())
        }
    }

    fun init() {
        if (!init) {
            registry.registryTypes({ registry ->
                registry.addAsymSupplier("Core", "Entity", 0, Int.MAX_VALUE)
                registry.addAsymSupplier("Core", "Environment", 0, Int.MAX_VALUE)
                registry.addSupplier("Core", "Packet", 0, Short.MAX_VALUE.toInt())
                registry.addSupplier("Core", "Update", 0, Short.MAX_VALUE.toInt())
                pluginsMut.forEach { it.registryType(registry) }
            })
            registry.init(worldType)
            pluginsMut.forEach { it.register(registry) }
            pluginsMut.forEach { it.init(registry) }
            registry.lock()
            init = true
        }
    }

    companion object : KLogging() {
        @Throws(IOException::class)
        fun installed(path: FilePath): List<PluginFile> {
            val files = ArrayList<PluginFile>()
            listRecursive(path) {
                filter {
                    isRegularFile(it) && isNotHidden(it)
                }.forEach { files.add(PluginFile(it)) }
            }
            files.addAll(embedded())
            return files
        }

        @Throws(IOException::class)
        fun embedded(): List<PluginFile> {
            val embedded = ClasspathResource(Plugins::class.java.classLoader,
                    "Plugin.json")
            if (embedded.exists()) {
                return listOf(PluginFile(embedded))
            }
            return emptyList()
        }
    }
}
