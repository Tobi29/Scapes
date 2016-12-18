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
package scapes.plugin.tobi29.vanilla.basics.util

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobFlyingBlockServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobServer
import scapes.plugin.tobi29.vanilla.basics.material.block.BlockExplosive
import java.util.*
import java.util.concurrent.ThreadLocalRandom

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
    val random = ThreadLocalRandom.current()
    val step = 360.0 / TWO_PI / size
    var pitch = 90.0
    while (pitch >= -90.0) {
        val cosYaw = cosTable(pitch.toRad())
        val stepYawForPitch = abs(step / cosYaw)
        val deltaZ = sinTable(pitch.toRad())
        var yaw = 0.0
        while (yaw < 360.0) {
            val deltaX = cosTable(yaw.toRad()) * cosYaw
            val deltaY = sinTable(yaw.toRad()) * cosYaw
            var distance = 0.0
            while (distance < size) {
                val xxx = floor(x + deltaX * distance)
                val yyy = floor(y + deltaY * distance)
                val zzz = floor(z + deltaZ * distance)
                val block = block(xxx, yyy, zzz)
                val type = type(block)
                val data = data(block)
                if (type != air) {
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
                distance++
            }
            yaw += stepYawForPitch
        }
        pitch -= step
    }
    world.taskExecutor.addTaskOnce({
        entities.forEach { world.addEntityNew(it) }
        world.explosionEntities(x, y, z, size, push, damage)
    }, "Explosion-Entities")
}
