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

package org.tobi29.scapes.vanilla.basics.entity.particle

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.TWO_PI
import org.tobi29.scapes.engine.utils.math.cosTable
import org.tobi29.scapes.engine.utils.math.sinTable
import org.tobi29.scapes.engine.utils.math.toRad
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.particle.*
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import java.util.concurrent.ThreadLocalRandom

class ParticleEmitterTornado(system: ParticleSystem) : ParticleEmitter<ParticleInstanceTornado>(
        system, Array(10240, { ParticleInstanceTornado() })) {

    private fun trail(emitter: ParticleEmitterTransparent,
                      pos: Vector3d,
                      speed: Vector3d,
                      texture: ParticleTransparentTexture) {
        emitter.add { instance ->
            val random = ThreadLocalRandom.current()
            instance.pos.set(pos)
            instance.speed.set(speed)
            instance.time = 1.0f
            instance.disablePhysics()
            instance.setTexture(texture)
            instance.rStart = 0.6f
            instance.gStart = 0.6f
            instance.bStart = 0.6f
            instance.aStart = 1.0f
            instance.rEnd = 0.9f
            instance.gEnd = 0.9f
            instance.bEnd = 0.9f
            instance.aEnd = 0.0f
            instance.sizeStart = 4.0f
            instance.sizeEnd = 3.0f
            instance.dir = random.nextFloat() * TWO_PI.toFloat()
        }
    }

    override fun update(delta: Double) {
        if (!hasAlive) {
            return
        }
        val plugin = system.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val emitter = system.emitter(ParticleEmitterTransparent::class.java)
        var hasAlive = false
        for (instance in instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue
            }
            instance.time -= delta.toFloat()
            if (instance.time <= 0.0) {
                instance.state = ParticleInstance.State.DEAD
                continue
            }
            hasAlive = true
            instance.pos.plusZ(18.0 * delta)
            instance.width += (0.8 * delta).toFloat()
            instance.spin += (220.0 * delta).toFloat()
            instance.spin %= 360.0f
            instance.puff -= delta.toFloat()
            if (instance.puff <= 0.0) {
                instance.puff += 0.1f
                val s = instance.spin.toRad().toDouble()
                val x = instance.pos.doubleX() + cosTable(
                        s) * instance.width.toDouble() * instance.widthRandom.toDouble() + cosTable(
                        instance.baseSpin.toDouble()) * instance.width
                val y = instance.pos.doubleY() + sinTable(
                        s) * instance.width.toDouble() * instance.widthRandom.toDouble() + sinTable(
                        instance.baseSpin.toDouble()) * instance.width.toDouble() * 3.0
                trail(emitter, Vector3d(x, y, instance.pos.doubleZ()),
                        Vector3d.ZERO, plugin.particles.cloud)
            }
        }
        this.hasAlive = hasAlive
    }

    override fun render(gl: GL,
                        cam: Cam) {
    }
}
