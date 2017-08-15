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
import org.tobi29.scapes.chunk.generator.ChunkGenerator
import org.tobi29.scapes.chunk.generator.GeneratorOutput
import org.tobi29.scapes.engine.utils.andNull
import org.tobi29.scapes.engine.utils.assert
import org.tobi29.scapes.engine.utils.math.threadLocalRandom
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.math.vector.distanceSqr
import org.tobi29.scapes.engine.utils.profiler.profilerSection
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.terrain.infinite.TerrainInfiniteBaseChunk
import org.tobi29.scapes.terrain.infinite.chunk

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
            val updates = ArrayList<Update>(0)
            lockWrite {
                var i = 0
                while (i < delayedUpdates.size) {
                    val update = delayedUpdates[i]
                    // An update might be paused while getting added to avoid
                    // serious lock congestion during modify operations
                    if (!update.isPaused) {
                        if (update.isValid) {
                            if (update.delay(delta) <= 0) {
                                delayedUpdates.removeAt(i)
                                if (update.isValidOn(
                                        typeG(update.x(), update.y(),
                                                update.z()), terrain)) {
                                    updates.add(update)
                                }
                            } else {
                                i++
                            }
                        } else {
                            delayedUpdates.removeAt(i)
                        }
                    } else {
                        i++
                    }
                }
                null
            }
            updates.forEach { it.run(terrain) }
            val random = threadLocalRandom()
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
        assert { lock.isHeld() }
        delayedUpdates.add(update)
    }

    fun hasDelayedUpdate(x: Int,
                         y: Int,
                         z: Int,
                         clazz: Class<out Update>): Boolean {
        return lockWrite {
            for (update in delayedUpdates) {
                if (update.x() == x && update.y() == y && update.z() == z) {
                    if (update.isValidOn(
                            typeG(update.x(), update.y(), update.z()),
                            terrain)) {
                        if (update::class.java == clazz) {
                            return@lockWrite true
                        }
                    } else {
                        update.markAsInvalid()
                    }
                }
            }
            false
        }
    }

    override fun update(x: Int,
                        y: Int,
                        z: Int,
                        updateTile: Boolean) {
    }

    override fun updateLight(x: Int,
                             y: Int,
                             z: Int) {
    }

    fun read(map: TagMap) {
        lockWrite {
            map["BlockID"]?.toList()?.let {
                (data.idData.asSequence() zip it.asSequence().mapNotNull(
                        Tag::toMap).andNull()).forEach { (data, tag) ->
                    data.read(tag)
                }
            }
            map["BlockData"]?.toList()?.let {
                (data.dataData.asSequence() zip it.asSequence().mapNotNull(
                        Tag::toMap).andNull()).forEach { (data, tag) ->
                    data.read(tag)
                }
            }
            map["BlockLight"]?.toList()?.let {
                (data.lightData.asSequence() zip it.asSequence().mapNotNull(
                        Tag::toMap).andNull()).forEach { (data, tag) ->
                    data.read(tag)
                }
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
                        tag["PosZ"]?.toInt() ?: 0,
                        tag["Delay"]?.toDouble() ?: 0.0,
                        tag["ID"]?.toInt() ?: 0).apply { isPaused = false })
            }
            if (map["Populated"]?.toBoolean() ?: false) {
                state = TerrainInfiniteBaseChunk.State.POPULATED
            }
            map["MetaData"]?.toMap()?.let { metaData = it.toMutTag() }
            initHeightMap()
        }
    }

    fun write(map: ReadWriteTagMap,
              packet: Boolean) {
        val tick = terrain.world.tick
        val data = data
        lockWrite {
            data.idData.forEach { it.compress() }
            data.dataData.forEach { it.compress() }
            data.lightData.forEach { it.compress() }
            map["BlockID"] =
                    TagList(data.idData.asSequence().map { it.toTag() })
            map["BlockData"] =
                    TagList(data.dataData.asSequence().map { it.toTag() })
            map["BlockLight"] =
                    TagList(data.lightData.asSequence().map { it.toTag() })
            map["MetaData"] = metaData.toTag()
            if (!packet) {
                map["Tick"] = tick.toTag()
                map["Entities"] = TagList {
                    entitiesMut.values.forEach { entity ->
                        add(TagMap {
                            this["UUID"] = entity.getUUID().toTag()
                            this["ID"] = entity.type.id.toTag()
                            this["Data"] = TagMap { entity.write(this) }
                        })
                    }
                }
                map["Updates"] = TagList {
                    delayedUpdates.asSequence().filter { update ->
                        update.isValidOn(
                                typeG(update.x(), update.y(), update.z()),
                                terrain)
                    }.forEach { update ->
                        add(TagMap {
                            this["ID"] = update.type.id.toTag()
                            this["Delay"] = update.delay().toTag()
                            this["PosXY"] = (update.x() - posBlock.x or (update.y() - posBlock.y shl 4)).toByte().toTag()
                            this["PosZ"] = update.z().toTag()
                        })
                    }
                }
                map["Populated"] = (state.id >= TerrainInfiniteBaseChunk.State.POPULATED.id).toTag()
            }
        }
    }

    private fun generate(generator: ChunkGenerator,
                         output: GeneratorOutput) {
        generator.seed(posBlock.x, posBlock.y)
        // We do not need to lock here as the chunk is not added anywhere
        // yet and this might get triggered in a locked context causing
        // a crash
        val data = data
        for (y in 0..15) {
            val yy = posBlock.y + y
            for (x in 0..15) {
                val xx = posBlock.x + x
                generator.makeLand(xx, yy, 0, zSize, output)
                for (z in 0 until zSize) {
                    val type = output.type[z]
                    if (type != 0) {
                        data.setID(x, y, z, type)
                        val blockData = output.data[z]
                        if (blockData != 0) {
                            data.setData(x, y, z, blockData)
                        }
                    }
                }
                val registry = terrain.world.registry
                output.updates.forEach { delayedUpdates.add(it(registry)) }
                output.updates.clear()
            }
        }
    }

    fun populate() {
        state = TerrainInfiniteBaseChunk.State.POPULATING
        terrain.populators.forEach { pop ->
            pop.populate(terrain, this)
        }
        val data = data
        lockWrite {
            updateSunLight()
            data.idData.forEach { it.compress() }
            data.dataData.forEach { it.compress() }
            data.lightData.forEach { it.compress() }
        }
        state = TerrainInfiniteBaseChunk.State.POPULATED
    }

    fun finish() {
        terrain.populators.forEach { pop ->
            pop.load(terrain, this)
        }
        state = TerrainInfiniteBaseChunk.State.BORDER
        terrain.updateAdjacent(pos.x, pos.y)
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
