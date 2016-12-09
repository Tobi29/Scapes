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

import java8.util.stream.Stream
import mu.KLogging
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.chunk.IDStorage
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPath
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathResource
import org.tobi29.scapes.engine.utils.stream
import java.io.IOException
import java.net.URLClassLoader
import java.util.*

class Plugins @Throws(IOException::class)
constructor(private val files: List<PluginFile>, idStorage: IDStorage) {
    private val plugins = ArrayList<Plugin>()
    private val dimensions = ArrayList<Dimension>()
    private val registry: GameRegistry
    private val classLoader: URLClassLoader?
    private var worldType: WorldType? = null
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
        if (worldType == null) {
            throw IOException("No world type found")
        }
        registry = GameRegistry(idStorage)
    }

    @Throws(IOException::class)
    private fun load(plugin: Plugin) {
        plugins.add(plugin)
        if (plugin is Dimension) {
            dimensions.add(plugin)
        }
        if (plugin is WorldType) {
            if (worldType != null) {
                throw IOException("Found 2nd world type: " + plugin)
            }
            worldType = plugin
        }
    }

    fun dispose() {
        plugins.clear()
        dimensions.clear()
        worldType = null
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

    fun fileCount(): Int {
        return files.size
    }

    fun files(): Stream<PluginFile> {
        return files.stream()
    }

    fun file(i: Int): PluginFile {
        return files[i]
    }

    fun plugins(): Stream<Plugin> {
        return plugins.stream()
    }

    fun dimensions(): Stream<Dimension> {
        return dimensions.stream()
    }

    fun worldType(): WorldType {
        return worldType ?: throw IllegalStateException("No world type loaded")
    }

    fun plugin(name: String): Plugin {
        for (plugin in plugins) {
            if (plugin.id() == name) {
                return plugin
            }
        }
        throw IllegalArgumentException("Unknown plugin")
    }

    fun addFileSystems(files: FileSystemContainer) {
        for (plugin in plugins) {
            files.registerFileSystem(plugin.id(),
                    ClasspathPath(plugin.javaClass.classLoader,
                            plugin.assetRoot()))
        }
    }

    fun removeFileSystems(files: FileSystemContainer) {
        for (plugin in plugins) {
            files.removeFileSystem(plugin.id())
        }
    }

    fun init() {
        if (!init) {
            registry.registryTypes({ registry ->
                plugins.forEach { it.registryType(registry) }
            })
            registry.init(worldType())
            plugins.forEach { it.register(registry) }
            plugins.forEach { it.init(registry) }
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
