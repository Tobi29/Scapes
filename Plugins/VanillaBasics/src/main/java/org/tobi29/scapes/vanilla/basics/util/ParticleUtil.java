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

package org.tobi29.scapes.vanilla.basics.util;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleEmitterExplosion;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class ParticleUtil {
    private ParticleUtil() {
    }

    public static void explosion(WorldClient world, Vector3 pos, Vector3 speed,
            double size) {
        ParticleEmitterExplosion emitter = world.scene().particles()
                .emitter(ParticleEmitterExplosion.class);
        int count = (int) (size * 80);
        double speedFactor = FastMath.sqrt(size);
        for (int i = 0; i < count; i++) {
            emitter.add(instance -> {
                Random random = ThreadLocalRandom.current();
                double dirZ = random.nextDouble() * FastMath.TWO_PI;
                double dirX =
                        random.nextDouble() * FastMath.PI - FastMath.HALF_PI;
                double dirSpeed = (random.nextDouble() + 20.0) * speedFactor;
                double dirSpeedX =
                        FastMath.cosTable(dirZ) * FastMath.cosTable(dirX) *
                                dirSpeed;
                double dirSpeedY =
                        FastMath.sinTable(dirZ) * FastMath.cosTable(dirX) *
                                dirSpeed;
                double dirSpeedZ = FastMath.sinTable(dirX) * dirSpeed;
                instance.pos.set(pos);
                instance.speed.set(dirSpeedX, dirSpeedY, dirSpeedZ).plus(speed);
                instance.time = 0.125f;
            });
        }
    }
}
