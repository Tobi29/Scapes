package org.tobi29.scapes.vanilla.basics.entity.particle;

import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.particle.*;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ParticleEmitterTornado
        extends ParticleEmitter<ParticleInstanceTornado> {
    public ParticleEmitterTornado(ParticleSystem system) {
        super(system, new ParticleInstanceTornado[10240],
                ParticleInstanceTornado::new);
    }

    private static void trail(ParticleEmitterTransparent emitter, Vector3 pos,
            Vector3 speed, ParticleTransparentTexture texture) {
        emitter.add(instance -> {
            Random random = ThreadLocalRandom.current();
            instance.pos.set(pos);
            instance.speed.set(speed);
            instance.time = 1.0f;
            instance.disablePhysics();
            instance.setTexture(texture);
            instance.rStart = 0.6f;
            instance.gStart = 0.6f;
            instance.bStart = 0.6f;
            instance.aStart = 1.0f;
            instance.rEnd = 0.9f;
            instance.gEnd = 0.9f;
            instance.bEnd = 0.9f;
            instance.aEnd = 0.0f;
            instance.sizeStart = 4.0f;
            instance.sizeEnd = 3.0f;
            instance.dir = random.nextFloat() * (float) FastMath.TWO_PI;
        });
    }

    @Override
    public void update(double delta) {
        if (!hasAlive) {
            return;
        }
        VanillaBasics plugin = (VanillaBasics) system.world().plugins()
                .plugin("VanillaBasics");
        ParticleEmitterTransparent emitter =
                system.emitter(ParticleEmitterTransparent.class);
        boolean hasAlive = false;
        for (ParticleInstanceTornado instance : instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            instance.time -= delta;
            if (instance.time <= 0.0) {
                instance.state = ParticleInstance.State.DEAD;
                continue;
            }
            hasAlive = true;
            instance.pos.plusZ(18.0 * delta);
            instance.width += 0.8 * delta;
            instance.spin += 220.0 * delta;
            instance.spin %= 360.0;
            instance.puff -= delta;
            if (instance.puff <= 0.0) {
                instance.puff += 0.1;
                double s = instance.spin * FastMath.DEG_2_RAD;
                double x = instance.pos.doubleX() +
                        FastMath.cosTable(s) * instance.width *
                                instance.widthRandom +
                        FastMath.cosTable(instance.baseSpin) * instance.width;
                double y = instance.pos.doubleY() +
                        FastMath.sinTable(s) * instance.width *
                                instance.widthRandom +
                        FastMath.sinTable(instance.baseSpin) * instance.width *
                                3.0;
                trail(emitter, new Vector3d(x, y, instance.pos.doubleZ()),
                        Vector3d.ZERO, plugin.particles.cloud);
            }
        }
        this.hasAlive = hasAlive;
    }

    @Override
    public void render(GL gl, Cam cam) {
    }
}
