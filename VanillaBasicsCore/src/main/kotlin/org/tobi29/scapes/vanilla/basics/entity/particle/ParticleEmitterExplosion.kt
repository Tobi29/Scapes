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

package org.tobi29.scapes.vanilla.basics.entity.particle

import org.tobi29.graphics.Cam
import org.tobi29.math.AABB3
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.entity.particle.*
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.stdex.math.TWO_PI

class ParticleEmitterExplosion(system: ParticleSystem) : ParticleEmitter<ParticleInstanceExplosion>(
        system, Array(256, { ParticleInstanceExplosion() })) {

    override fun update(delta: Double) {
        if (!hasAlive) {
            return
        }
        val plugin = system.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val aabb = AABB3()
        val gravitation = system.world.gravity.toFloat()
        val terrain = system.world.terrain
        val emitter = system.emitter(ParticleEmitterTransparent::class.java)
        var hasAlive = false
        for (instance in instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue
            }
            instance.time -= delta.toFloat()
            if (instance.time <= 0.0) {
                val random = threadLocalRandom()
                smoke(emitter, instance.pos.now(), instance.speed.now(),
                        plugin.particles.smoke,
                        random.nextFloat() * 8.0f + 12.0f)
                instance.state = ParticleInstance.State.DEAD
                continue
            }
            hasAlive = true
            instance.puff -= delta.toFloat()
            // Not a while loop to avoid particles in same position
            if (instance.puff <= 0.0) {
                instance.puff += 0.0125f
                trail(emitter, instance.pos.now(), instance.speed.now(),
                        plugin.particles.explosion)
            }
            aabb.min.x = instance.pos.x - SIZE
            aabb.min.y = instance.pos.y - SIZE
            aabb.min.z = instance.pos.z - SIZE
            aabb.max.x = instance.pos.x + SIZE
            aabb.max.y = instance.pos.y + SIZE
            aabb.max.z = instance.pos.z + SIZE
            ParticlePhysics.update(delta, instance, terrain, aabb, gravitation,
                    0.01f,
                    0.2f, 0.4f, 8.0f)
        }
        this.hasAlive = hasAlive
    }

    override fun addToPipeline(gl: GL,
                               width: Int,
                               height: Int,
                               cam: Cam): suspend () -> (Double) -> Unit {
        return { {} }
    }

    companion object {
        private val SIZE = 0.125f

        private fun trail(emitter: ParticleEmitterTransparent,
                          pos: Vector3d,
                          speed: Vector3d,
                          texture: Int) {
            emitter.add { instance ->
                val random = threadLocalRandom()
                instance.pos.set(pos)
                instance.speed.set(speed)
                instance.time = 1.0f
                instance.setPhysics(-0.01f)
                instance.setTexture(emitter, texture)
                instance.rStart = 4.0f
                instance.gStart = 3.0f
                instance.bStart = 0.3f
                instance.aStart = 1.0f
                instance.rEnd = 1.0f
                instance.gEnd = 0.2f
                instance.bEnd = 0.0f
                instance.aEnd = 0.0f
                instance.sizeStart = 1.0f
                instance.sizeEnd = 4.0f
                instance.dir = random.nextFloat() * TWO_PI.toFloat()
            }
        }

        private fun smoke(emitter: ParticleEmitterTransparent,
                          pos: Vector3d,
                          speed: Vector3d,
                          texture: Int,
                          time: Float) {
            emitter.add { instance ->
                val random = threadLocalRandom()
                instance.pos.set(pos)
                instance.speed.set(speed)
                instance.time = time
                instance.setPhysics(-0.2f, 0.6f)
                instance.setTexture(emitter, texture)
                instance.rStart = 0.7f
                instance.gStart = 0.7f
                instance.bStart = 0.7f
                instance.aStart = 1.0f
                instance.rEnd = 0.3f
                instance.gEnd = 0.3f
                instance.bEnd = 0.3f
                instance.aEnd = 0.0f
                instance.sizeStart = 3.0f
                instance.sizeEnd = 16.0f
                instance.dir = random.nextFloat() * TWO_PI.toFloat()
            }
        }
    }
}
