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
package org.tobi29.scapes.vanilla.basics.util

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.launch
import org.tobi29.math.cosTable
import org.tobi29.math.sinTable
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.*
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStackData
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.BlockExplosive
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock
import org.tobi29.stdex.ThreadLocal
import org.tobi29.stdex.assert
import org.tobi29.stdex.math.TWO_PI
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.putAbsent
import org.tobi29.utils.Pool
import kotlin.math.PI
import kotlin.math.abs

private val LOCATIONS = ThreadLocal { Pool { Location() } }

fun WorldServer.explosionEntities(x: Double,
                                  y: Double,
                                  z: Double,
                                  radius: Double,
                                  push: Double,
                                  damage: Double) {
    assert { checkThread() }
    getEntities(Vector3d(x, y, z),
            radius).filterIsInstance<MobServer>().forEach { mob ->
        val relative = mob.getCurrentPos().minus(Vector3d(x, y, z))
        val s = radius - relative.length()
        if (s > 0) {
            val p = s * push
            val force = relative.normalizedSafe().times(p)
            mob.push(force.x, force.y, force.z)
            if (mob is MobLivingServer) {
                mob.damage(s * damage)
            }
        }
    }
}

fun TerrainServer.explosionBlockPush(
        x: Int,
        y: Int,
        z: Int,
        size: Int,
        dropChance: Double,
        blockChance: Double,
        push: Double,
        damage: Double) {
    val plugin = world.plugins.plugin<VanillaBasics>()
    val entities = ArrayList<EntityServer>()
    val locations = LOCATIONS.get()
    val random = threadLocalRandom()
    val step = 2.0 / PI / size
    var pitch = 0.0
    val set = HashMap<Location, Location>()
    while (pitch < TWO_PI) {
        val sinYaw = sinTable(pitch)
        val stepYawForPitch = abs(step / sinYaw)
        val deltaZ = cosTable(pitch)
        var yaw = 0.0
        while (yaw < TWO_PI) {
            val deltaX = cosTable(yaw) * sinYaw
            val deltaY = sinTable(yaw) * sinYaw
            var distance = 0.0
            var blast = size.toDouble()
            while (true) {
                val xxx = (x + 0.5 + deltaX * distance).floorToInt()
                val yyy = (y + 0.5 + deltaY * distance).floorToInt()
                val zzz = (z + 0.5 + deltaZ * distance).floorToInt()
                val location = locations.push().apply {
                    set(xxx, yyy, zzz)
                }
                val previous = set.putAbsent(location, location)
                val type: BlockType
                val data: Int
                if (previous == null) {
                    val block = block(xxx, yyy, zzz)
                    type = type(block)
                    data = data(block)
                    location.set(type, data)
                } else {
                    locations.pop()
                    type = previous.type
                    data = previous.data
                }
                // TODO: Blast resistance
                blast -= 1.0
                distance++
                if (blast < 0.0) {
                    if (previous == null) {
                        locations.pop()
                    }
                    break
                }
            }
            yaw += stepYawForPitch
        }
        pitch += step
    }
    modify(x - size, y - size, z - size,
            size shl 1, size shl 1, size shl 1) { terrain ->
        locations.forEach { location ->
            val xxx = location.x
            val yyy = location.y
            val zzz = location.z
            val type = location.type
            val data = location.data
            if (type is BlockExplosive) {
                type.igniteByExplosion(this, xxx, yyy, zzz, data)
            } else {
                if (random.nextDouble() < dropChance) {
                    if (type is VanillaBlock) {
                        world.dropItems(type.drops(null, data), xxx, yyy, zzz)
                    }
                } else if (type.isSolid(data) &&
                        !type.isTransparent(data) &&
                        random.nextDouble() < blockChance) {
                    entities.add(plugin.entityTypes.flyingBlock.createServer(
                            world).apply {
                        setPos(Vector3d(xxx + 0.5, yyy + 0.5, zzz + 0.5))
                        setSpeed(Vector3d(random.nextDouble() * 0.1 - 0.05,
                                random.nextDouble() * 0.1 - 0.05,
                                random.nextDouble() * 1 + 2))
                        setType(ItemStackData(type, data))
                    })
                }
            }
            terrain.typeData(xxx, yyy, zzz, air, 0)
        }
    }
    locations.reset()
    launch(world + CoroutineName("Explosion-Entities")) {
        entities.forEach { world.addEntityNew(it) }
        world.explosionEntities(x + 0.5, y + 0.5, z + 0.5, size.toDouble(),
                push, damage)
    }
}

private class Location {
    var x = 0
    var y = 0
    var z = 0
    lateinit var type: BlockType
    var data = 0

    fun set(x: Int,
            y: Int,
            z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun set(type: BlockType,
            data: Int) {
        this.type = type
        this.data = data
    }

    override fun hashCode(): Int {
        return (x * 31 + y) * 31 + z
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Location) {
            return false
        }
        if (x != other.x || y != other.y || z != other.z) {
            return false
        }
        return true
    }
}
