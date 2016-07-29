package org.tobi29.scapes.vanilla.basics.entity.particle;

import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.particle.*;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ParticleEmitterExplosion
        extends ParticleEmitter<ParticleInstanceExplosion> {
    private static final float SIZE = 0.125f;

    public ParticleEmitterExplosion(ParticleSystem system) {
        super(system, new ParticleInstanceExplosion[256],
                ParticleInstanceExplosion::new);
    }

    private static void trail(ParticleEmitterTransparent emitter, Vector3 pos,
            Vector3 speed, ParticleTransparentTexture texture) {
        emitter.add(instance -> {
            Random random = ThreadLocalRandom.current();
            instance.pos.set(pos);
            instance.speed.set(speed);
            instance.time = 1.0f;
            instance.setPhysics(-0.01f);
            instance.setTexture(texture);
            instance.rStart = 4.0f;
            instance.gStart = 3.0f;
            instance.bStart = 0.3f;
            instance.aStart = 1.0f;
            instance.rEnd = 1.0f;
            instance.gEnd = 0.2f;
            instance.bEnd = 0.0f;
            instance.aEnd = 0.0f;
            instance.sizeStart = 1.0f;
            instance.sizeEnd = 4.0f;
            instance.dir = random.nextFloat() * (float) FastMath.TWO_PI;
        });
    }

    private static void smoke(ParticleEmitterTransparent emitter, Vector3 pos,
            Vector3 speed, ParticleTransparentTexture texture, float time) {
        emitter.add(instance -> {
            Random random = ThreadLocalRandom.current();
            instance.pos.set(pos);
            instance.speed.set(speed);
            instance.time = time;
            instance.setPhysics(-0.2f, 0.6f);
            instance.setTexture(texture);
            instance.rStart = 0.7f;
            instance.gStart = 0.7f;
            instance.bStart = 0.7f;
            instance.aStart = 1.0f;
            instance.rEnd = 0.3f;
            instance.gEnd = 0.3f;
            instance.bEnd = 0.3f;
            instance.aEnd = 0.0f;
            instance.sizeStart = 3.0f;
            instance.sizeEnd = 16.0f;
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
        AABB aabb = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        float gravitation = (float) system.world().gravity();
        TerrainClient terrain = system.world().terrain();
        ParticleEmitterTransparent emitter =
                system.emitter(ParticleEmitterTransparent.class);
        boolean hasAlive = false;
        for (ParticleInstanceExplosion instance : instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            instance.time -= delta;
            if (instance.time <= 0.0) {
                Random random = ThreadLocalRandom.current();
                smoke(emitter, instance.pos.now(), instance.speed.now(),
                        plugin.particles.smoke,
                        random.nextFloat() * 8.0f + 12.0f);
                instance.state = ParticleInstance.State.DEAD;
                continue;
            }
            hasAlive = true;
            instance.puff -= delta;
            // Not a while loop to avoid particles in same position
            if (instance.puff <= 0.0) {
                instance.puff += 0.0125;
                trail(emitter, instance.pos.now(), instance.speed.now(),
                        plugin.particles.explosion);
            }
            aabb.minX = instance.pos.doubleX() - SIZE;
            aabb.minY = instance.pos.doubleY() - SIZE;
            aabb.minZ = instance.pos.doubleZ() - SIZE;
            aabb.maxX = instance.pos.doubleX() + SIZE;
            aabb.maxY = instance.pos.doubleY() + SIZE;
            aabb.maxZ = instance.pos.doubleZ() + SIZE;
            ParticlePhysics
                    .update(delta, instance, terrain, aabb, gravitation, 0.01f,
                            0.2f, 0.4f, 8.0f);
        }
        this.hasAlive = hasAlive;
    }

    @Override
    public void render(GL gl, Cam cam) {
    }
}
