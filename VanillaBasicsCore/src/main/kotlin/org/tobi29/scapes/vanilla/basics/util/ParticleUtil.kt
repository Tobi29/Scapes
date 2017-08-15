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

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.threadLocalRandom
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleEmitterExplosion

fun WorldClient.explosion(pos: Vector3d,
                          speed: Vector3d,
                          size: Double) {
    val emitter = scene.particles().emitter(
            ParticleEmitterExplosion::class.java)
    val count = (size * 80).toInt()
    val speedFactor = sqrt(size)
    for (i in 0 until count) {
        emitter.add { instance ->
            val random = threadLocalRandom()
            val dirZ = random.nextDouble() * TWO_PI
            val dirX = random.nextDouble() * PI - HALF_PI
            val dirSpeed = (random.nextDouble() + 20.0) * speedFactor
            val dirSpeedX = cosTable(dirZ) * cosTable(dirX) *
                    dirSpeed
            val dirSpeedY = sinTable(dirZ) * cosTable(dirX) *
                    dirSpeed
            val dirSpeedZ = sinTable(dirX) * dirSpeed
            instance.pos.set(pos)
            instance.speed.set(dirSpeedX, dirSpeedY, dirSpeedZ)
            instance.speed.plus(speed)
            instance.time = 0.125f
        }
    }
}
