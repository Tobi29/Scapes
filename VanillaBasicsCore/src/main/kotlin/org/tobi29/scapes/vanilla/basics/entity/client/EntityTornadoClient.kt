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

package org.tobi29.scapes.vanilla.basics.entity.client

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.math.TWO_PI
import org.tobi29.scapes.engine.utils.math.cosTable
import org.tobi29.scapes.engine.utils.math.sinTable
import org.tobi29.scapes.engine.utils.math.toRad
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.normalizeSafe
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.threadLocalRandom
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.MobPositionReceiver
import org.tobi29.scapes.entity.client.EntityAbstractClient
import org.tobi29.scapes.entity.client.MobileEntityClient
import org.tobi29.scapes.entity.particle.ParticleEmitter3DBlock
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleEmitterTornado

class EntityTornadoClient(type: EntityType<*, *>,
                          world: WorldClient) : EntityAbstractClient(
        type, world,
        Vector3d.ZERO), MobileEntityClient {
    override val positionReceiver: MobPositionReceiver
    private var puff = 0.0
    private var baseSpin = 0.0f

    init {
        positionReceiver = MobPositionReceiver({ this.pos.set(it) },
                { newSpeed -> },
                { newPos -> },
                { ground, slidingWall, inWater, swimming -> })
    }

    override fun read(map: TagMap) {
        super.read(map)
        positionReceiver.receiveMoveAbsolute(pos.doubleX(), pos.doubleY(),
                pos.doubleZ())
    }

    override fun update(delta: Double) {
        baseSpin += (40.0 * delta).toFloat()
        baseSpin %= 360.0f
        puff -= delta
        while (puff <= 0.0) {
            puff += 0.05
            val random = threadLocalRandom()
            val spin = random.nextFloat() * 360.0f
            val emitter = world.scene.particles().emitter(
                    ParticleEmitterTornado::class.java)
            emitter.add { instance ->
                instance.pos.set(pos.now())
                instance.speed.set(Vector3d.ZERO)
                instance.time = 12.0f
                instance.dir = random.nextFloat() * 360.0f
                instance.spin = spin
                instance.baseSpin = baseSpin.toRad()
                instance.width = 0.0f
                instance.widthRandom = random.nextFloat() + 3.0f
            }
            emitter.add { instance ->
                instance.pos.set(pos.now())
                instance.speed.set(Vector3d.ZERO)
                instance.time = 1.0f
                instance.dir = random.nextFloat() * 360.0f
                instance.spin = spin
                instance.baseSpin = baseSpin.toRad()
                instance.width = 0.0f
                instance.widthRandom = random.nextFloat() * 20.0f + 20.0f
            }
            if (random.nextInt(10) == 0) {
                emitter.add { instance ->
                    instance.pos.set(pos.now())
                    instance.speed.set(Vector3d.ZERO)
                    instance.time = 12.0f
                    instance.dir = random.nextFloat() * 360.0f
                    instance.spin = spin
                    instance.baseSpin = baseSpin.toRad()
                    instance.width = 0.0f
                    instance.widthRandom = random.nextFloat() * 10.0f + 6.0f
                }
            }
            val terrain = world.terrain
            val x = pos.intX() + random.nextInt(9) - 4
            val y = pos.intY() + random.nextInt(9) - 4
            val z = pos.intZ() + random.nextInt(7) - 3
            val block = terrain.block(x, y, z)
            val type = terrain.type(block)
            if (type !== world.air) {
                val emitter2 = world.scene.particles().emitter(
                        ParticleEmitter3DBlock::class.java)
                emitter2.add { instance ->
                    val random2 = threadLocalRandom()
                    val dir = random2.nextDouble() * TWO_PI
                    val dirSpeed = random2.nextDouble() * 12.0 + 20.0
                    val dirSpeedX = cosTable(dir) * cosTable(dir) * dirSpeed
                    val dirSpeedY = sinTable(dir) * cosTable(dir) * dirSpeed
                    val dirSpeedZ = random2.nextDouble() * 6.0 + 24.0
                    instance.pos.set(pos.now())
                    instance.speed.set(dirSpeedX, dirSpeedY, dirSpeedZ)
                    instance.time = 5.0f
                    instance.rotation.set(0.0f, 0.0f, 0.0f)
                    instance.rotationSpeed.set(
                            Vector3d(random2.nextDouble() - 0.5,
                                    random2.nextDouble() - 0.5,
                                    random2.nextDouble() - 0.5).normalizeSafe().times(
                                    480.0))
                    instance.item = ItemStack(type, terrain.data(block))
                }
            }
        }
    }
}
