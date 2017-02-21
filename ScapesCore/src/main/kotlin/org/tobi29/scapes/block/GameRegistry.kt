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

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.entity.client.EntityBlockBreakClient
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.MobFlyingBlockClient
import org.tobi29.scapes.entity.client.MobItemClient
import org.tobi29.scapes.entity.server.EntityBlockBreakServer
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobFlyingBlockServer
import org.tobi29.scapes.entity.server.MobItemServer
import org.tobi29.scapes.packets.*
import org.tobi29.scapes.plugins.WorldType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GameRegistry(private val idStorage: MutableTagMap) {
    private val registries = ConcurrentHashMap<Pair<String, String>, Registry<*>>()
    private val supplierRegistries = ConcurrentHashMap<Pair<String, String>, SupplierRegistry<*, *>>()
    private val asymSupplierRegistries = ConcurrentHashMap<Pair<String, String>, AsymSupplierRegistry<*, *, *, *>>()
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

    fun <D, E : Any> getSupplier(module: String,
                                 type: String): SupplierRegistry<D, E> {
        if (!lockedTypes) {
            throw IllegalStateException("Early initializing not finished")
        }
        @Suppress("UNCHECKED_CAST")
        return supplierRegistries[Pair(module, type)] as SupplierRegistry<D, E>
    }

    fun <D, E : Any, F, G : Any> getAsymSupplier(
            module: String,
            type: String): AsymSupplierRegistry<D, E, F, G> {
        if (!lockedTypes) {
            throw IllegalStateException("Early initializing not finished")
        }
        @Suppress("UNCHECKED_CAST")
        return asymSupplierRegistries[Pair(module,
                type)] as AsymSupplierRegistry<D, E, F, G>
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
        private val ids = ConcurrentHashMap<E, Int>()
        private val values = ArrayList<E?>()

        fun reg(element: E,
                name: String): Int {
            if (locked) {
                throw IllegalStateException("Initializing already ended")
            }
            val id = idStorage.getID(module, type, name, min, max)
            objects.put(id, element)
            ids.put(element, id)
            while (values.size <= id) {
                values.add(null)
            }
            values[id] = element
            return id
        }

        fun values(): List<E?> {
            return values
        }

        operator fun get(id: Int): E {
            val `object` = objects[id] ?: throw IllegalArgumentException(
                    "Invalid id")
            return `object`
        }

        operator fun get(instance: E): Int {
            return ids[instance] ?: throw IllegalArgumentException(
                    "${instance::class.java}")
        }
    }

    inner class SupplierRegistry<D, E : Any>(private val module: String,
                                             private val type: String,
                                             private val min: Int,
                                             private val max: Int) {
        private val objects = ConcurrentHashMap<Int, (D) -> E>()
        private val ids = ConcurrentHashMap<Class<out E>, Int>()
        private val values = ArrayList<((D) -> E)?>()

        fun reg(element: (D) -> E,
                clazz: Class<out E>,
                name: String): Int {
            if (locked) {
                throw IllegalStateException("Initializing already ended")
            }
            val id = idStorage.getID(module, type, name, min, max)
            objects.put(id, element)
            ids.put(clazz, id)
            while (values.size <= id) {
                values.add(null)
            }
            values[id] = element
            return id
        }

        fun values(): List<((D) -> E)?> {
            return values
        }

        operator fun get(id: Int): (D) -> E {
            val `object` = objects[id] ?: throw IllegalArgumentException(
                    "Invalid id")
            return `object`
        }

        fun id(instance: E): Int {
            return ids[instance::class.java] ?: throw IllegalArgumentException(
                    "${instance::class.java}")
        }
    }

    inner class AsymSupplierRegistry<D, E : Any, F, G : Any>(private val module: String,
                                                             private val type: String,
                                                             private val min: Int,
                                                             private val max: Int) {
        private val objects = ConcurrentHashMap<Int, (D) -> E>()
        private val objects2 = ConcurrentHashMap<Int, (F) -> G>()
        private val ids = ConcurrentHashMap<Class<out E>, Int>()
        private val values = ArrayList<((D) -> E)?>()

        fun reg(element: (D) -> E,
                element2: (F) -> G,
                clazz: Class<out E>,
                name: String): Int {
            if (locked) {
                throw IllegalStateException("Initializing already ended")
            }
            val id = idStorage.getID(module, type, name, min, max)
            objects.put(id, element)
            objects2.put(id, element2)
            ids.put(clazz, id)
            while (values.size <= id) {
                values.add(null)
            }
            values[id] = element
            return id
        }

        fun values(): List<((D) -> E)?> {
            return values
        }

        fun get1(id: Int): (D) -> E {
            return objects[id] ?: throw IllegalArgumentException("Invalid id")
        }

        fun get2(id: Int): (F) -> G {
            return objects2[id] ?: throw IllegalArgumentException("Invalid id")
        }

        fun id(instance: E): Int {
            return ids[instance::class.java] ?: throw IllegalArgumentException(
                    "${instance::class.java}")
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

        @Synchronized fun addSupplier(module: String,
                                      type: String,
                                      min: Int,
                                      max: Int) {
            if (lockedTypes) {
                throw IllegalStateException(
                        "Early initializing already ended")
            }
            val pair = Pair(module, type)
            var registry: SupplierRegistry<*, *>? = supplierRegistries[pair]
            if (registry == null) {
                registry = SupplierRegistry<Any, Any>(module, type, min, max)
                supplierRegistries.put(pair, registry)
            }
        }

        @Synchronized fun addAsymSupplier(module: String,
                                          type: String,
                                          min: Int,
                                          max: Int) {
            if (lockedTypes) {
                throw IllegalStateException(
                        "Early initializing already ended")
            }
            val pair = Pair(module, type)
            var registry: AsymSupplierRegistry<*, *, *, *>? = asymSupplierRegistries[pair]
            if (registry == null) {
                registry = AsymSupplierRegistry<Any, Any, Any, Any>(module,
                        type, min, max)
                asymSupplierRegistries.put(pair, registry)
            }
        }
    }
}

fun GameRegistry.init(worldType: WorldType) {
    val er = getAsymSupplier<WorldServer, EntityServer, WorldClient, EntityClient>(
            "Core", "Entity")
    val pr = getSupplier<GameRegistry, PacketAbstract>("Core", "Packet")
    val ur = getSupplier<GameRegistry, Update>("Core", "Update")
    er.reg({ throw UnsupportedOperationException() }, {
        worldType.playerSupplier().invoke(it)
    },
            worldType.playerClass(), "core.mob.Player")
    er.reg({
        EntityBlockBreakServer(it)
    }, { EntityBlockBreakClient(it) },
            EntityBlockBreakServer::class.java, "core.entity.BlockBreak")
    er.reg({ MobItemServer(it) },
            { MobItemClient(it) },
            MobItemServer::class.java,
            "core.mob.Item")
    er.reg({ MobFlyingBlockServer(it) }, { MobFlyingBlockClient(it) },
            MobFlyingBlockServer::class.java, "core.mob.FlyingBlock")
    pr.reg({ PacketRequestChunk() }, PacketRequestChunk::class.java,
            "core.packet.RequestChunk")
    pr.reg({ PacketRequestEntity() }, PacketRequestEntity::class.java,
            "core.packet.RequestEntity")
    pr.reg({ PacketSendChunk() }, PacketSendChunk::class.java,
            "core.packet.SendChunk")
    pr.reg({ PacketBlockChange() }, PacketBlockChange::class.java,
            "core.packet.BlockChange")
    pr.reg({ PacketBlockChangeAir() }, PacketBlockChangeAir::class.java,
            "core.packet.BlockChangeAir")
    pr.reg({ PacketEntityAdd() }, PacketEntityAdd::class.java,
            "core.packet.EntityAdd")
    pr.reg({ PacketEntityChange() }, PacketEntityChange::class.java,
            "core.packet.EntityChange")
    pr.reg({ PacketEntityMetaData() }, PacketEntityMetaData::class.java,
            "core.packet.EntityMetaData")
    pr.reg({ PacketMobMoveRelative() }, PacketMobMoveRelative::class.java,
            "core.packet.MobMoveRelative")
    pr.reg({ PacketMobMoveAbsolute() }, PacketMobMoveAbsolute::class.java,
            "core.packet.MobMoveAbsolute")
    pr.reg({ PacketMobChangeRot() }, PacketMobChangeRot::class.java,
            "core.packet.MobChangeRot")
    pr.reg({ PacketMobChangeSpeed() }, PacketMobChangeSpeed::class.java,
            "core.packet.MobChangeSpeed")
    pr.reg({ PacketMobChangeState() }, PacketMobChangeState::class.java,
            "core.packet.MobChangeState")
    pr.reg({ PacketMobDamage() }, PacketMobDamage::class.java,
            "core.packet.MobDamage")
    pr.reg({ PacketEntityDespawn() }, PacketEntityDespawn::class.java,
            "core.packet.EntityDespawn")
    pr.reg({ PacketSoundEffect() }, PacketSoundEffect::class.java,
            "core.packet.SoundEffect")
    pr.reg({ PacketInteraction() }, PacketInteraction::class.java,
            "core.packet.Interaction")
    pr.reg({ PacketInventoryInteraction() },
            PacketInventoryInteraction::class.java,
            "core.packet.InventoryInteraction")
    pr.reg({ PacketPlayerJump() }, PacketPlayerJump::class.java,
            "core.packet.PlayerJump")
    pr.reg({ PacketOpenGui() }, PacketOpenGui::class.java,
            "core.packet.OpenGui")
    pr.reg({ PacketCloseGui() }, PacketCloseGui::class.java,
            "core.packet.CloseGui")
    pr.reg({ PacketUpdateInventory() }, PacketUpdateInventory::class.java,
            "core.packet.UpdateInventory")
    pr.reg({ PacketChat() }, PacketChat::class.java, "core.packet.Chat")
    pr.reg({ PacketItemUse() }, PacketItemUse::class.java,
            "core.packet.ItemUse")
    pr.reg({ PacketDisconnect() }, PacketDisconnect::class.java,
            "core.packet.Disconnect")
    pr.reg({ PacketSetWorld() }, PacketSetWorld::class.java,
            "core.packet.SetWorld")
    pr.reg({ PacketPingClient() }, PacketPingClient::class.java,
            "core.packet.PingClient")
    pr.reg({ PacketPingServer() }, PacketPingServer::class.java,
            "core.packet.PingServer")
    pr.reg({ PacketSkin() }, PacketSkin::class.java, "core.packet.Skin")
    ur.reg({ UpdateBlockUpdate() }, UpdateBlockUpdate::class.java,
            "core.update.BlockUpdate")
    ur.reg({ UpdateBlockUpdateUpdateTile() },
            UpdateBlockUpdateUpdateTile::class.java,
            "core.update.BlockUpdateUpdateTile")
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
