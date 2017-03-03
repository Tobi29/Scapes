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
package org.tobi29.scapes.block

import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.EntityBlockBreakClient
import org.tobi29.scapes.entity.client.MobFlyingBlockClient
import org.tobi29.scapes.entity.client.MobItemClient
import org.tobi29.scapes.entity.server.EntityBlockBreakServer
import org.tobi29.scapes.entity.server.MobFlyingBlockServer
import org.tobi29.scapes.entity.server.MobItemServer
import org.tobi29.scapes.packets.*
import org.tobi29.scapes.plugins.WorldType
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GameRegistry(private val idStorage: MutableTagMap) {
    private val registries = ConcurrentHashMap<Pair<String, String>, Registry<*>>()
    private val materialNames = ConcurrentHashMap<String, Material>()
    private val air: BlockType
    private var blocks: Array<BlockType?>
    private var materials: Array<Material?>
    private var lockedTypes = false
    private var locked = false

    init {
        materials = arrayOfNulls<Material>(1)
        air = BlockAir(this)
        blocks = arrayOfNulls<BlockType>(1)
        materials[0] = air
        blocks[0] = air
        air.id = 0
    }

    fun registryTypes(consumer: (RegistryAdder) -> Unit) {
        if (lockedTypes) {
            throw IllegalStateException("Early initializing already ended")
        }
        val registry = RegistryAdder()
        consumer(registry)
        lockedTypes = true
    }

    fun writeIDStorage() = idStorage.toTag()

    fun lock() {
        locked = true
    }

    operator fun <E : Any> get(module: String,
                               type: String): Registry<E> {
        if (!lockedTypes) {
            throw IllegalStateException("Early initializing not finished")
        }
        @Suppress("UNCHECKED_CAST")
        return registries[Pair(module, type)] as Registry<E>
    }

    fun material(name: String): Material? {
        return materialNames[name]
    }

    fun material(id: Int?): Material? {
        if (id == null) {
            return null
        }
        return materials[id]
    }

    fun material(id: Int): Material? {
        return materials[id]
    }

    fun materials(): Array<Material?> {
        return materials
    }

    fun block(id: Int): BlockType? {
        return blocks[id]
    }

    fun blocks(): Array<BlockType?> {
        return blocks
    }

    fun air(): BlockType {
        return air
    }

    fun registerMaterial(material: Material) {
        if (!lockedTypes) {
            throw IllegalStateException("Early initializing not finished")
        }
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        val nameID = material.nameID()
        val blockType = material is BlockType
        val id = idStorage.getID("Core", "Material", nameID,
                if (blockType) 1 else Short.MAX_VALUE + 1,
                if (blockType) Short.MAX_VALUE.toInt() else Int.MAX_VALUE)
        material.id = id
        if (id >= materials.size) {
            val materials2 = materials
            materials = arrayOfNulls<Material>(id + 1)
            System.arraycopy(materials2, 0, materials, 0, materials2.size)
        }
        materials[id] = material
        if (material is BlockType) {
            if (id >= blocks.size) {
                val blocks2 = blocks
                blocks = arrayOfNulls<BlockType>(id + 1)
                System.arraycopy(blocks2, 0, blocks, 0, blocks2.size)
            }
            blocks[id] = material
        }
        materialNames.put(nameID, material)
        materialNames.put(
                nameID.substring(nameID.lastIndexOf('.') + 1, nameID.length),
                material)
    }

    inner class Registry<E : Any>(private val module: String,
                                  private val type: String,
                                  private val min: Int,
                                  private val max: Int) {
        private val objects = ConcurrentHashMap<Int, E>()
        private val objectsByStr = ConcurrentHashMap<String, E>()
        private val ids = ConcurrentHashMap<E, Int>()
        private val values = ArrayList<E?>()

        fun reg(name: String,
                block: (Int) -> E): E {
            if (locked) {
                throw IllegalStateException("Initializing already ended")
            }
            val id = idStorage.getID(module, type, name, min, max)
            val element = block(id)
            objects.put(id, element)
            objectsByStr.put(name, element)
            ids.put(element, id)
            while (values.size <= id) {
                values.add(null)
            }
            values[id] = element
            return element
        }

        fun values(): List<E?> {
            return values
        }

        operator fun get(id: Int): E {
            val `object` = objects[id] ?: throw IllegalArgumentException(
                    "Invalid id")
            return `object`
        }

        operator fun get(id: String): E {
            val `object` = objectsByStr[id] ?: throw IllegalArgumentException(
                    "Invalid id")
            return `object`
        }

        operator fun get(instance: E): Int {
            return ids[instance] ?: throw IllegalArgumentException("$instance")
        }
    }

    inner class RegistryAdder {
        @Synchronized fun add(module: String,
                              type: String,
                              min: Int,
                              max: Int) {
            if (lockedTypes) {
                throw IllegalStateException(
                        "Early initializing already ended")
            }
            val pair = Pair(module, type)
            var registry: Registry<*>? = registries[pair]
            if (registry == null) {
                registry = Registry<Any>(module, type, min, max)
                registries.put(pair, registry)
            }
        }
    }
}

fun GameRegistry.init(worldType: WorldType) {
    get<UpdateType>("Core", "Update").run {
        reg("core.update.BlockUpdate") {
            org.tobi29.scapes.block.UpdateType(it, ::UpdateBlockUpdate)
        }
        reg("core.update.BlockUpdateUpdateTile") {
            org.tobi29.scapes.block.UpdateType(it,
                    ::UpdateBlockUpdateUpdateTile)
        }
    }

    get<EntityType>("Core", "Entity").run {
        reg("core.mob.Player") {
            EntityType(it, { worldType.playerSupplier().invoke(it) },
                    { throw UnsupportedOperationException() })
        }
        reg("core.entity.BlockBreak") {
            EntityType(it, { EntityBlockBreakClient(it) },
                    { EntityBlockBreakServer(it) })
        }
        reg("core.mob.Item") {
            EntityType(it, { MobItemClient(it) }, { MobItemServer(it) })
        }
        reg("core.mob.FlyingBlock") {
            EntityType(it, { MobFlyingBlockClient(it) },
                    { MobFlyingBlockServer(it) })
        }
    }

    get<PacketType>("Core", "Packet").run {
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
                throw IOException("This packet should never be received")
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

private fun ReadWriteTagMutableMap.getID(module: String,
                                         type: String,
                                         name: String,
                                         min: Int = 0,
                                         max: Int = Int.MAX_VALUE): Int {
    val typeTag = mapMut(module).mapMut(type)
    typeTag[name]?.toInt()?.let { return it }
    var i = min
    while (typeTag.containsValue(i.toTag())) {
        i++
        if (i > max) {
            throw IllegalStateException(
                    "Overflowed IDs for: $module->$type")
        }
    }
    typeTag[name] = i
    return i
}
