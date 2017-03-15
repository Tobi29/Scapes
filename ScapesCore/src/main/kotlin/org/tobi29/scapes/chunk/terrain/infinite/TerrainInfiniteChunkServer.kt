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
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.math.vector.distanceSqr
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.packets.PacketBlockChange
import org.tobi29.scapes.packets.PacketBlockChangeAir
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteBaseChunk
import org.tobi29.scapes.terrain.infinite.chunk
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class TerrainInfiniteChunkServer : TerrainInfiniteChunk<EntityServer> {
    private val terrain: TerrainInfiniteServer
    private val delayedUpdates = ArrayList<Update>()

    constructor(pos: Vector2i,
                terrain: TerrainInfiniteServer,
                zSize: Int,
                map: TagMap) : super(pos, terrain, zSize) {
        this.terrain = terrain
        profilerSection("Load") { read(map) }
        profilerSection("HeightMap") { initHeightMap() }
    }

    constructor(pos: Vector2i,
                terrain: TerrainInfiniteServer,
                zSize: Int,
                generator: ChunkGenerator,
                output: GeneratorOutput) : super(pos, terrain, zSize) {
        this.terrain = terrain
        profilerSection("Generate") { generate(generator, output) }
        profilerSection("Sunlight") { initSunLight() }
        profilerSection("HeightMap") { initHeightMap() }
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
                    val player = terrain.world.nearestPlayer(
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
                if (terrain.chunk(x, y) { chunk ->
                    chunk.mapEntity(entity)
                    true
                } == null) {
                    terrain.entityRemoved(entity)
                }
            }
        }
        if (state.id >= TerrainInfiniteBaseChunk.State.LOADED.id) {
            synchronized(delayedUpdates) {
                var i = 0
                while (i < delayedUpdates.size) {
                    val update = delayedUpdates[i]
                    if (update.isValid) {
                        if (update.delay(delta) <= 0) {
                            delayedUpdates.removeAt(i--)
                            if (update.isValidOn(
                                    typeG(update.x(), update.y(), update.z()),
                                    terrain)) {
                                update.run(terrain)
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
                terrain.type(block).update(terrain, x + posBlock.x,
                        y + posBlock.y, z, terrain.data(block))
            }
        }
    }

    fun disposeAndWrite(): TagMap {
        entitiesMut.values.forEach { terrain.entityRemoved(it) }
        return TagMap { write(this, false) }
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
                            terrain)) {
                        if (update::class.java == clazz) {
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
                    UpdateBlockUpdateUpdateTile(terrain.world.registry).set(
                            x + posBlock.x, y + posBlock.y, z, 0.0))
        } else {
            addDelayedUpdate(UpdateBlockUpdate(terrain.world.registry).set(
                    x + posBlock.x, y + posBlock.y, z, 0.0))
        }
        if (state.id >= TerrainInfiniteBaseChunk.State.SENDABLE.id) {
            val block = blockL(x, y, z)
            val type = terrain.type(block)
            if (type === terrain.air) {
                terrain.world.send(
                        PacketBlockChangeAir(terrain.world.registry,
                                x + posBlock.x, y + posBlock.y, z))
            } else {
                val data = terrain.data(block)
                terrain.world.send(PacketBlockChange(terrain.world.registry,
                        x + posBlock.x, y + posBlock.y, z, type.id, data))
            }
        }
        if (state.id >= TerrainInfiniteBaseChunk.State.LOADED.id) {
            terrain.lighting().updateLight(x + posBlock.x,
                    y + posBlock.y, z)
        }
    }

    override fun updateLight(x: Int,
                             y: Int,
                             z: Int) {
    }

    fun read(map: TagMap) {
        lockWrite {
            map["BlockID"]?.toList()?.let { bID.read(it) }
            map["BlockData"]?.toList()?.let { bData.read(it) }
            map["BlockLight"]?.toList()?.let { bLight.read(it) }
        }
        val oldTick = map["Tick"]?.toLong() ?: 0L
        map["Entities"]?.toList()?.asSequence()?.mapNotNull(
                Tag::toMap)?.forEach { tag ->
            tag["ID"]?.toInt()?.let {
                val entity = EntityServer.make(it, terrain.world)
                tag["UUID"]?.toUUID()?.let { entity.setEntityID(it) }
                tag["Data"]?.toMap()?.let { entity.read(it) }
                addEntity(entity)
                val newTick = terrain.world.tick
                if (newTick > oldTick) {
                    entity.tickSkip(oldTick, newTick)
                }
            }
        }
        map["Updates"]?.toList()?.asSequence()?.mapNotNull(
                Tag::toMap)?.forEach { tag ->
            var xy = tag["PosXY"]?.toInt() ?: 0
            if (xy < 0) {
                xy += 256
            }
            addDelayedUpdate(Update.make(terrain.world.plugins.registry,
                    (xy and 0xF) + posBlock.x,
                    (xy ushr 4) + posBlock.y,
                    tag["PosZ"]?.toInt() ?: 0, tag["Delay"]?.toDouble() ?: 0.0,
                    tag["ID"]?.toInt() ?: 0))
        }
        if (map["Populated"]?.toBoolean() ?: false) {
            state = TerrainInfiniteBaseChunk.State.POPULATED
        }
        map["MetaData"]?.toMap()?.let { metaData = it.toMutTag() }
        initHeightMap()
    }

    fun write(map: ReadWriteTagMap,
              packet: Boolean) {
        val tick = terrain.world.tick
        lockWrite {
            bID.compress()
            bData.compress()
            bLight.compress()
            map["BlockID"] = TagList { bID.write(this) }
            map["BlockData"] = TagList { bData.write(this) }
            map["BlockLight"] = TagList { bLight.write(this) }
        }
        map["MetaData"] = metaData.toTag()
        if (!packet) {
            map["Tick"] = tick
            val registry = terrain.world.registry
            map["Entities"] = TagList {
                entitiesMut.values.forEach { entity ->
                    add(TagMap {
                        this["UUID"] = entity.getUUID()
                        this["ID"] = entity.type.id
                        this["Data"] = TagMap { entity.write(this) }
                    })
                }
            }
            map["Updates"] = TagList {
                synchronized(delayedUpdates) {
                    delayedUpdates.asSequence().filter { update ->
                        update.isValidOn(
                                typeG(update.x(), update.y(), update.z()),
                                terrain)
                    }.forEach { update ->
                        add(TagMap {
                            this["ID"] = update.type.id
                            this["Delay"] = update.delay()
                            this["PosXY"] = (update.x() - posBlock.x or (update.y() - posBlock.y shl 4)).toByte()
                            this["PosZ"] = update.z()
                        })
                    }
                }
            }
            map["Populated"] = state.id >= TerrainInfiniteBaseChunk.State.POPULATED.id
        }
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
                    val registry = terrain.world.registry
                    output.updates.forEach { addDelayedUpdate(it(registry)) }
                    output.updates.clear()
                }
            }
        }
    }

    fun populate() {
        state = TerrainInfiniteBaseChunk.State.POPULATING
        terrain.queue({ handle ->
            terrain.populators.forEach { pop ->
                pop.populate(handle, this)
            }
            updateSunLight()
            lockWrite {
                bID.compress()
                bData.compress()
                bLight.compress()
            }
            state = TerrainInfiniteBaseChunk.State.POPULATED
        })
    }

    fun finish() {
        terrain.queue({ handle ->
            terrain.populators.forEach { pop ->
                pop.load(handle, this)
            }
            state = TerrainInfiniteBaseChunk.State.BORDER
            terrain.updateAdjacent(pos.x, pos.y)
        })
    }

    val isSendable: Boolean
        get() = state.id >= TerrainInfiniteBaseChunk.State.SENDABLE.id

    fun shouldPopulate(): Boolean {
        return state == TerrainInfiniteBaseChunk.State.SHOULD_POPULATE
    }

    fun shouldFinish(): Boolean {
        return state == TerrainInfiniteBaseChunk.State.POPULATED
    }

    fun updateAdjacent() {
        if (terrain.checkBorder(this, 1)) {
            if (state == TerrainInfiniteBaseChunk.State.BORDER) {
                state = TerrainInfiniteBaseChunk.State.LOADED
                terrain.updateAdjacent(pos.x, pos.y)
            } else if (state == TerrainInfiniteBaseChunk.State.NEW) {
                state = TerrainInfiniteBaseChunk.State.SHOULD_POPULATE
            }
            if (terrain.checkLoaded(this, 1)) {
                if (state == TerrainInfiniteBaseChunk.State.LOADED) {
                    state = TerrainInfiniteBaseChunk.State.SENDABLE
                    terrain.updateAdjacent(pos.x, pos.y)
                }
            } else if (state == TerrainInfiniteBaseChunk.State.SENDABLE) {
                state = TerrainInfiniteBaseChunk.State.LOADED
                terrain.updateAdjacent(pos.x, pos.y)
            }
        } else if (state.id >= TerrainInfiniteBaseChunk.State.LOADED.id) {
            state = TerrainInfiniteBaseChunk.State.BORDER
            terrain.updateAdjacent(pos.x, pos.y)
        }
    }
}
