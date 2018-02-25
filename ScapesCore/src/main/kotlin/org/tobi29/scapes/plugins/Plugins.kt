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

import org.tobi29.logging.KLogging
import org.tobi29.scapes.block.*
import org.tobi29.io.FileSystemContainer
import org.tobi29.io.IOException
import org.tobi29.io.classpath.ClasspathPath
import org.tobi29.io.filesystem.FilePath
import org.tobi29.io.filesystem.isNotHidden
import org.tobi29.io.filesystem.isRegularFile
import org.tobi29.io.filesystem.listRecursive
import org.tobi29.scapes.inventory.ItemType
import org.tobi29.scapes.packets.*
import org.tobi29.io.tag.MutableTagMap
import org.tobi29.stdex.readOnly
import java.net.URLClassLoader

class Plugins(files: List<PluginFile>,
              idStorage: MutableTagMap) {
    val files = files.readOnly()
    private val pluginsMut = ArrayList<Plugin>()
    val plugins = pluginsMut.readOnly()
    private val dimensionsMut = ArrayList<Dimension>()
    val dimensions = dimensionsMut.readOnly()
    val registry: Registries
    lateinit var air: BlockAir
        private set
    lateinit var materialResolver: Map<String, ItemType>
        private set
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
            val file = PluginFile.load(
                    ClasspathPath(classLoader, "scapes/plugin/Plugin.json"))
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
        registry = Registries(idStorage)
    }

    // TODO: @Throws(IOException::class)
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
                registry.add("Core", "ItemType", 0, Short.MAX_VALUE.toInt())
                registry.add("Core", "Entity", 0, Int.MAX_VALUE)
                registry.add("Core", "Environment", 0, Int.MAX_VALUE)
                registry.add("Core", "Packet", 0, Short.MAX_VALUE.toInt())
                registry.add("Core", "Update", 0, Short.MAX_VALUE.toInt())
                pluginsMut.forEach { it.registryType(registry) }
            })
            init(registry)
            registry.get<ItemType>("Core", "ItemType").run {
                air = reg("core.block.Air", 0) {
                    BlockAir(MaterialType(this@Plugins, it, "core.block.Air"))
                }
            }
            pluginsMut.forEach { it.register(this) }
            pluginsMut.forEach { it.init(registry) }
            registry.lock()
            materialResolver = assembleMaterialResolver(
                    registry.get<ItemType>("Core", "ItemType"))
            init = true
        }
    }

    companion object : KLogging() {
        // TODO: @Throws(IOException::class)
        suspend fun installed(path: FilePath): List<PluginFile> {
            val paths = ArrayList<FilePath>()
            listRecursive(path) {
                filter {
                    isRegularFile(it) && isNotHidden(it)
                }.forEach { paths.add(it) }
            }
            return paths.mapNotNull {
                try {
                    PluginFile.loadFile(it)
                } catch (e: IOException) {
                    logger.warn { "Failed to read plugins: $e" }
                    null
                }
            } + embedded()
        }

        // TODO: @Throws(IOException::class)
        suspend fun embedded(): List<PluginFile> {
            val embedded = ClasspathPath(Plugins::class.java.classLoader,
                    "scapes/plugin/Plugin.json")
            val list = ArrayList<PluginFile>()

            try {
                list.add(PluginFile.load(embedded))
            } catch (e: IOException) {
            }
            return list.readOnly()
        }

        fun init(registry: Registries) {
            registry.get<UpdateType>("Core", "Update").run {
                reg("core.update.BlockUpdate") {
                    UpdateType(it, ::UpdateBlockUpdate)
                }
                reg("core.update.BlockUpdateUpdateTile") {
                    UpdateType(it, ::UpdateBlockUpdateUpdateTile)
                }
            }

            registry.get<PacketType>("Core", "Packet").run {
                reg("core.packet.RequestChunk") {
                    PacketType(it, ::PacketRequestChunk)
                }
                reg("core.packet.RequestEntity") {
                    PacketType(it, ::PacketRequestEntity)
                }
                reg("core.packet.SendChunk") {
                    PacketType(it, ::PacketSendChunk)
                }
                reg("core.packet.BlockChange") {
                    PacketType(it, ::PacketBlockChange)
                }
                reg("core.packet.BlockChangeAir") {
                    PacketType(it, ::PacketBlockChangeAir)
                }
                reg("core.packet.EntityAdd") {
                    PacketType(it, ::PacketEntityAdd)
                }
                reg("core.packet.EntityChange") {
                    PacketType(it, ::PacketEntityChange)
                }
                reg("core.packet.EntityComponentData") {
                    PacketType(it, ::PacketEntityComponentData)
                }
                reg("core.packet.EntityMetaData") {
                    PacketType(it, ::PacketEntityMetaData)
                }
                reg("core.packet.MobMoveRelative") {
                    PacketType(it, ::PacketMobMoveRelative)
                }
                reg("core.packet.MobMoveAbsolute") {
                    PacketType(it, ::PacketMobMoveAbsolute)
                }
                reg("core.packet.MobChangeRot") {
                    PacketType(it, ::PacketMobChangeRot)
                }
                reg("core.packet.MobChangeSpeed") {
                    PacketType(it, ::PacketMobChangeSpeed)
                }
                reg("core.packet.MobChangeState") {
                    PacketType(it, ::PacketMobChangeState)
                }
                reg("core.packet.MobDamage") {
                    PacketType(it, ::PacketMobDamage)
                }
                reg("core.packet.EntityDespawn") {
                    PacketType(it, ::PacketEntityDespawn)
                }
                reg("core.packet.SoundEffect") {
                    PacketType(it, ::PacketSoundEffect)
                }
                reg("core.packet.Interaction") {
                    PacketType(it, ::PacketInteraction)
                }
                reg("core.packet.InventoryInteraction") {
                    PacketType(it, ::PacketInventoryInteraction)
                }
                reg("core.packet.PlayerJump") {
                    PacketType(it, ::PacketPlayerJump)
                }
                reg("core.packet.OpenGui") {
                    PacketType(it, ::PacketOpenGui)
                }
                reg("core.packet.CloseGui") {
                    PacketType(it, ::PacketCloseGui)
                }
                reg("core.packet.UpdateInventory") {
                    PacketType(it, ::PacketUpdateInventory)
                }
                reg("core.packet.Chat") {
                    PacketType(it, ::PacketChat)
                }
                reg("core.packet.ItemUse") {
                    PacketType(it, ::PacketItemUse)
                }
                reg("core.packet.Disconnect") {
                    PacketType(it, ::PacketDisconnect)
                }
                reg("core.packet.DisconnectSelf") {
                    PacketType(it, {
                        throw IOException(
                                "This packet should never be received")
                    })
                }
                reg("core.packet.SetWorld") {
                    PacketType(it, ::PacketSetWorld)
                }
                reg("core.packet.PingClient") {
                    PacketType(it, ::PacketPingClient)
                }
                reg("core.packet.PingServer") {
                    PacketType(it, ::PacketPingServer)
                }
                reg("core.packet.Skin") {
                    PacketType(it, ::PacketSkin)
                }
            }
        }

        fun assembleMaterialResolver(registry: Registries.Registry<ItemType>) =
                registry.values.asSequence().filterNotNull().flatMap { material ->
                    val nameID = material.nameID
                    sequenceOf(Pair(nameID, material),
                            Pair(nameID.substring(nameID.lastIndexOf('.') + 1,
                                    nameID.length), material))
                }.toMap()
    }
}

fun <T : ItemType> Registries.Registry<ItemType>.reg(
        name: String,
        plugins: Plugins,
        block: (MaterialType) -> T
) = reg(name) { block(MaterialType(plugins, it, name)) }
