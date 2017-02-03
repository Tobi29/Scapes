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

package org.tobi29.scapes.chunk.terrain.infinite

import org.tobi29.scapes.block.Update
import org.tobi29.scapes.block.UpdateBlockUpdate
import org.tobi29.scapes.block.UpdateBlockUpdateUpdateTile
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.GeneratorOutput
import org.tobi29.scapes.engine.utils.forEach
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.math.vector.distanceSqr
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.packets.PacketBlockChange
import org.tobi29.scapes.packets.PacketBlockChangeAir
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class TerrainInfiniteChunkServer : TerrainInfiniteChunk<EntityServer> {
    private val terrain2: TerrainInfiniteServer
    private val delayedUpdates = ArrayList<Update>()
    private var lastAccess = System.currentTimeMillis()

    constructor(pos: Vector2i,
                terrain: TerrainInfiniteServer,
                zSize: Int,
                tagStructure: TagStructure) : super(pos, terrain, zSize,
            terrain.world.registry.blocks()) {
        this.terrain2 = terrain
        profilerSection("Load") { load(tagStructure) }
        profilerSection("HeightMap") { initHeightMap() }
    }

    constructor(pos: Vector2i,
                terrain: TerrainInfiniteServer,
                zSize: Int,
                generator: ChunkGenerator,
                output: GeneratorOutput) : super(pos, terrain, zSize,
            terrain.world.registry.blocks()) {
        this.terrain2 = terrain
        profilerSection("Generate") { generate(generator, output) }
        profilerSection("Sunlight") { initSunLight() }
        profilerSection("HeightMap") { initHeightMap() }
    }

    internal fun accessed() {
        lastAccess = System.currentTimeMillis()
    }

    fun lastAccess(): Long {
        return lastAccess
    }

    fun updateServer(delta: Double) {
        entitiesMut.values.forEach { entity ->
            entity.update(delta)
            entity.updateListeners(delta)
            if (entity is MobServer) {
                entity.move(delta)
            }
            if (entity is MobLivingServer) {
                if (entity.isDead) {
                    entity.onDeath()
                    removeEntity(entity)
                    return@forEach
                } else if (entity.creatureType().doesDespawn()) {
                    val player = terrain2.world.nearestPlayer(
                            entity.getCurrentPos())
                    if (player != null) {
                        if (entity.getCurrentPos().distanceSqr(
                                player.getCurrentPos()) > 16384.0) {
                            removeEntity(entity)
                            return@forEach
                        }
                    } else {
                        removeEntity(entity)
                        return@forEach
                    }
                }
            }
            val pos = entity.getCurrentPos()
            val x = pos.intX() shr 4
            val y = pos.intY() shr 4
            if ((x != this.pos.x || y != this.pos.y) && unmapEntity(
                    entity)) {
                if (terrain.chunk(x, y, { chunk ->
                    chunk.mapEntity(entity)
                    true
                }) == null) {
                    terrain.entityRemoved(entity)
                }
            }
        }
        if (state.id >= TerrainInfiniteChunk.State.LOADED.id) {
            synchronized(delayedUpdates) {
                var i = 0
                while (i < delayedUpdates.size) {
                    val update = delayedUpdates[i]
                    if (update.isValid) {
                        if (update.delay(delta) <= 0) {
                            delayedUpdates.removeAt(i--)
                            if (update.isValidOn(
                                    typeG(update.x(), update.y(), update.z()),
                                    terrain2)) {
                                update.run(terrain2)
                            }
                        }
                        i++
                    } else {
                        delayedUpdates.removeAt(i)
                    }
                }
            }
            val random = ThreadLocalRandom.current()
            if (random.nextInt(16) == 0) {
                val x = random.nextInt(16)
                val y = random.nextInt(16)
                val z = random.nextInt(zSize)
                val block = blockL(x, y, z)
                terrain.type(block).update(terrain2, x + posBlock.x,
                        y + posBlock.y, z, terrain.data(block))
            }
        }
    }

    fun dispose(): TagStructure {
        entitiesMut.values.forEach { terrain.entityRemoved(it) }
        return save(false)
    }

    fun addDelayedUpdate(update: Update) {
        synchronized(delayedUpdates) {
            delayedUpdates.add(update)
        }
    }

    fun hasDelayedUpdate(x: Int,
                         y: Int,
                         z: Int,
                         clazz: Class<out Update>): Boolean {
        synchronized(delayedUpdates) {
            for (update in delayedUpdates) {
                if (update.x() == x && update.y() == y && update.z() == z) {
                    if (update.isValidOn(
                            typeG(update.x(), update.y(), update.z()),
                            terrain2)) {
                        if (update.javaClass == clazz) {
                            return true
                        }
                    } else {
                        update.markAsInvalid()
                    }
                }
            }
        }
        return false
    }

    override fun update(x: Int,
                        y: Int,
                        z: Int,
                        updateTile: Boolean) {
        if (updateTile) {
            addDelayedUpdate(
                    UpdateBlockUpdateUpdateTile().set(x + posBlock.x,
                            y + posBlock.y, z, 0.0))
        } else {
            addDelayedUpdate(UpdateBlockUpdate().set(x + posBlock.x,
                    y + posBlock.y, z, 0.0))
        }
        if (state.id >= TerrainInfiniteChunk.State.SENDABLE.id) {
            val block = blockL(x, y, z)
            val type = terrain.type(block)
            if (type === terrain2.air) {
                terrain2.world.send(PacketBlockChangeAir(x + posBlock.x,
                        y + posBlock.y, z))
            } else {
                val data = terrain.data(block)
                terrain2.world.send(PacketBlockChange(x + posBlock.x,
                        y + posBlock.y, z, type.id, data))
            }
        }
        if (state.id >= TerrainInfiniteChunk.State.LOADED.id) {
            terrain.lighting().updateLight(x + posBlock.x,
                    y + posBlock.y, z)
        }
    }

    override fun updateLight(x: Int,
                             y: Int,
                             z: Int) {
    }

    fun load(tagStructure: TagStructure) {
        lockWrite {
            tagStructure.getList("BlockID")?.let { bID.load(it) }
            tagStructure.getList("BlockData")?.let { bData.load(it) }
            tagStructure.getList("BlockLight")?.let { bLight.load(it) }
        }
        initHeightMap()
        val oldTick = tagStructure.getLong("Tick") ?: 0
        tagStructure.getListStructure("Entities") { tag ->
            EntityServer.make(tag.getInt("ID"),
                    terrain2.world)?.let { entity ->
                tag.getUUID("UUID")?.let { entity.setEntityID(it) }
                tag.getStructure("Data")?.let { entity.read(it) }
                addEntity(entity)
                val newTick = terrain2.world.tick
                if (newTick > oldTick) {
                    entity.tickSkip(oldTick, newTick)
                }
            }
        }
        tagStructure.getListStructure("Updates") { tag ->
            var xy = tag.getInt("PosXY") ?: 0
            if (xy < 0) {
                xy += 256
            }
            addDelayedUpdate(Update.make(terrain2.world.plugins.registry(),
                    (xy and 0xF) + posBlock.x,
                    (xy shr 4) + posBlock.y,
                    tag.getInt("PosZ") ?: 0, tag.getDouble("Delay") ?: 0.0,
                    tag.getInt("ID") ?: 0))
        }
        if (tagStructure.getBoolean("Populated") ?: false) {
            state = TerrainInfiniteChunk.State.POPULATED
        }
        tagStructure.getStructure("MetaData")?.let { metaData = it }
    }

    fun save(packet: Boolean): TagStructure {
        val tick = terrain2.world.tick
        val tagStructure = TagStructure()
        lockWrite {
            bID.compress()
            bData.compress()
            bLight.compress()
            tagStructure.setList("BlockID", bID.save())
            tagStructure.setList("BlockData", bData.save())
            tagStructure.setList("BlockLight", bLight.save())
        }
        tagStructure.setStructure("MetaData", metaData)
        if (!packet) {
            tagStructure.setLong("Tick", tick)
            val entitiesTag = ArrayList<TagStructure>()
            val registry = terrain2.world.registry
            entitiesMut.values.forEach { entity ->
                val entityTag = TagStructure()
                entityTag.setUUID("UUID", entity.getUUID())
                entityTag.setInt("ID", entity.id(registry))
                entityTag.setStructure("Data", entity.write())
                entitiesTag.add(entityTag)
            }
            tagStructure.setList("Entities", entitiesTag)
            val updatesTag = ArrayList<TagStructure>()
            synchronized(delayedUpdates) {
                delayedUpdates.forEach({ update ->
                    update.isValidOn(typeG(update.x(), update.y(), update.z()),
                            terrain2)
                }) { update ->
                    val updateTag = TagStructure()
                    updateTag.setInt("ID", update.id(registry))
                    updateTag.setDouble("Delay", update.delay())
                    updateTag.setByte("PosXY",
                            (update.x() - posBlock.x or (update.y() - posBlock.y shl 4)).toByte())
                    var xy = updateTag.getInt("PosXY") ?: 0
                    if (xy < 0) {
                        xy += 256
                    }
                    updateTag.setInt("PosZ", update.z())
                    updatesTag.add(updateTag)
                }
            }
            tagStructure.setList("Updates", updatesTag)
            tagStructure.setBoolean("Populated",
                    state.id >= TerrainInfiniteChunk.State.POPULATED.id)
        }
        return tagStructure
    }

    private fun generate(generator: ChunkGenerator,
                         output: GeneratorOutput) {
        generator.seed(posBlock.x, posBlock.y)
        lockWrite {
            for (y in 0..15) {
                val yy = posBlock.y + y
                for (x in 0..15) {
                    val xx = posBlock.x + x
                    generator.makeLand(xx, yy, 0, zSize, output)
                    for (z in 0..zSize - 1) {
                        val type = output.type[z]
                        if (type != 0) {
                            bID.setData(x, y, z, 0, type)
                            val data = output.data[z]
                            if (data != 0) {
                                bData.setData(x, y, z, 0, data)
                            }
                        }
                    }
                    output.updates.forEach { addDelayedUpdate(it) }
                    output.updates.clear()
                }
            }
        }
    }

    fun populate() {
        state = TerrainInfiniteChunk.State.POPULATING
        terrain2.queue({ handle ->
            terrain2.populators.forEach { pop ->
                pop.populate(handle, this)
            }
            updateSunLight()
            lockWrite {
                bID.compress()
                bData.compress()
                bLight.compress()
            }
            state = TerrainInfiniteChunk.State.POPULATED
        })
    }

    fun finish() {
        terrain2.queue({ handle ->
            terrain2.populators.forEach { pop ->
                pop.load(handle, this)
            }
            state = TerrainInfiniteChunk.State.BORDER
            terrain2.updateAdjacent(pos.x, pos.y)
        })
    }

    val isSendable: Boolean
        get() = state.id >= TerrainInfiniteChunk.State.SENDABLE.id

    fun shouldPopulate(): Boolean {
        return state == TerrainInfiniteChunk.State.SHOULD_POPULATE
    }

    fun shouldFinish(): Boolean {
        return state == TerrainInfiniteChunk.State.POPULATED
    }

    fun updateAdjacent() {
        if (terrain2.checkBorder(this, 1)) {
            if (state == TerrainInfiniteChunk.State.BORDER) {
                state = TerrainInfiniteChunk.State.LOADED
                terrain2.updateAdjacent(pos.x, pos.y)
            } else if (state == TerrainInfiniteChunk.State.NEW) {
                state = TerrainInfiniteChunk.State.SHOULD_POPULATE
            }
            if (terrain2.checkLoaded(this, 1)) {
                if (state == TerrainInfiniteChunk.State.LOADED) {
                    state = TerrainInfiniteChunk.State.SENDABLE
                    terrain2.updateAdjacent(pos.x, pos.y)
                }
            } else if (state == TerrainInfiniteChunk.State.SENDABLE) {
                state = TerrainInfiniteChunk.State.LOADED
                terrain2.updateAdjacent(pos.x, pos.y)
            }
        } else if (state.id >= TerrainInfiniteChunk.State.LOADED.id) {
            state = TerrainInfiniteChunk.State.BORDER
            terrain2.updateAdjacent(pos.x, pos.y)
        }
    }
}
