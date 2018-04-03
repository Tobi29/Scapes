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

import org.tobi29.io.FileSystemContainer
import org.tobi29.io.IOException
import org.tobi29.io.tag.MutableTagMap
import org.tobi29.logging.KLogging
import org.tobi29.scapes.block.*
import org.tobi29.scapes.inventory.ItemType
import org.tobi29.scapes.packets.*
import org.tobi29.scapes.plugins.spi.PluginHandle
import org.tobi29.scapes.plugins.spi.PluginProvider
import org.tobi29.stdex.readOnly
import org.tobi29.utils.spiLoad
import kotlin.reflect.KClass

class Plugins(
    files: List<PluginHandle>,
    idStorage: MutableTagMap
) {
    val files = files.readOnly()
    @PublishedApi
    internal val _plugins: Map<KClass<out Plugin>, Plugin>
    val plugins: Collection<Plugin> get() = _plugins.values
    val dimensions: List<Dimension>
    val worldType: WorldType
    val registry: Registries
    lateinit var air: BlockAir
        private set
    lateinit var materialResolver: Map<String, ItemType>
        private set
    private var init = false

    init {
        val plugins = HashMap<KClass<out Plugin>, Plugin>()
        val dimensions = ArrayList<Dimension>()
        var worldType: WorldType? = null
        for (handle in files) {
            val plugin = handle.second()
            plugins[plugin::class] = plugin
            if (plugin is Dimension) {
                dimensions.add(plugin)
            }
            if (plugin is WorldType) {
                if (worldType != null) {
                    throw IOException("Found 2nd world type: $plugin")
                }
                worldType = plugin
            }
        }
        if (worldType == null) throw IOException("No world type found")
        this._plugins = plugins.readOnly()
        this.dimensions = dimensions.readOnly()
        this.worldType = worldType
        registry = Registries(idStorage)
    }

    fun init() {
        if (!init) {
            registry.registryTypes({ registry ->
                registry.add("Core", "ItemType", 0, Short.MAX_VALUE.toInt())
                registry.add("Core", "Entity", 0, Int.MAX_VALUE)
                registry.add("Core", "Environment", 0, Int.MAX_VALUE)
                registry.add("Core", "Packet", 0, Short.MAX_VALUE.toInt())
                registry.add("Core", "Update", 0, Short.MAX_VALUE.toInt())
                plugins.forEach { it.registryType(registry) }
            })
            init(registry)
            registry.get<ItemType>("Core", "ItemType").run {
                air = reg("core.block.Air", 0) {
                    BlockAir(MaterialType(this@Plugins, it, "core.block.Air"))
                }
            }
            plugins.forEach { it.register(this) }
            plugins.forEach { it.init(registry) }
            registry.lock()
            materialResolver = assembleMaterialResolver(
                registry.get<ItemType>("Core", "ItemType")
            )
            init = true
        }
    }

    inline fun <reified P : Plugin> plugin(): P =
        _plugins[P::class] as? P
                ?: throw IllegalArgumentException("Unknown plugin: ${P::class}")

    companion object : KLogging() {
        private val spiPlugins =
            spiLoad(spiLoad<PluginProvider>()) { e ->
                logger.warn { "Unable to load plugin provider: $e" }
            }.flatMap { it.plugins }.toList()

        fun available(): List<PluginHandle> = spiPlugins

        fun setupAssets(files: FileSystemContainer) {
            available().forEach {
                files.registerFileSystem(it.first.id, it.first.assetRoot)
            }
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
                            "This packet should never be received"
                        )
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
                sequenceOf(
                    Pair(nameID, material),
                    Pair(
                        nameID.substring(
                            nameID.lastIndexOf('.') + 1,
                            nameID.length
                        ), material
                    )
                )
            }.toMap()
    }
}

fun <T : ItemType> Registries.Registry<ItemType>.reg(
    name: String,
    plugins: Plugins,
    block: (MaterialType) -> T
) = reg(name) { block(MaterialType(plugins, it, name)) }
