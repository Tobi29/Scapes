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
