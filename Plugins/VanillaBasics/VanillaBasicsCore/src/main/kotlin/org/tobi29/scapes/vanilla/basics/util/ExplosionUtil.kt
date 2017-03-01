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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.engine.utils.putAbsent
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobFlyingBlockServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.vanilla.basics.material.BlockExplosive
import java.util.*
import java.util.concurrent.ThreadLocalRandom

private val LOCATIONS = ThreadLocal { Pool { Location() } }

fun WorldServer.explosionEntities(x: Double,
                                  y: Double,
                                  z: Double,
                                  radius: Double,
                                  push: Double,
                                  damage: Double) {
    assert(checkThread())
    getEntities(Vector3d(x, y, z),
            radius).filterMap<MobServer>().forEach { mob ->
        val relative = mob.getCurrentPos().minus(Vector3d(x, y, z))
        val s = radius - relative.length()
        if (s > 0) {
            val p = s * push
            val force = relative.normalizeSafe().times(p)
            mob.push(force.x, force.y, force.z)
            if (mob is MobLivingServer) {
                mob.damage(s * damage)
            }
        }
    }
}

fun TerrainServer.TerrainMutable.explosionBlockPush(
        x: Double,
        y: Double,
        z: Double,
        size: Double,
        dropChance: Double,
        blockChance: Double,
        push: Double,
        damage: Double) {
    val entities = ArrayList<EntityServer>()
    val locations = LOCATIONS.get()
    val random = ThreadLocalRandom.current()
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
            var blast = size
            while (true) {
                val xxx = floor(x + deltaX * distance)
                val yyy = floor(y + deltaY * distance)
                val zzz = floor(z + deltaZ * distance)
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
                // TODO: Blast resistanceq
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
                world.dropItems(type.drops(
                        ItemStack(world.registry),
                        data), xxx, yyy, zzz)
            } else if (type.isSolid(this, xxx, yyy, zzz) &&
                    !type.isTransparent(this, xxx, yyy,
                            zzz) &&
                    random.nextDouble() < blockChance) {
                entities.add(MobFlyingBlockServer(world,
                        Vector3d(xxx + 0.5, yyy + 0.5,
                                zzz + 0.5), Vector3d(
                        random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 1 + 2), type,
                        data))
            }
        }
        typeData(xxx, yyy, zzz, air, 0)
    }
    locations.reset()
    world.taskExecutor.addTaskOnce({
        entities.forEach { world.addEntityNew(it) }
        world.explosionEntities(x, y, z, size, push, damage)
    }, "Explosion-Entities")
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
