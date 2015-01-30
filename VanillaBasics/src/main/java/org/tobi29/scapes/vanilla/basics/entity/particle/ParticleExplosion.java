/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.vanilla.basics.entity.particle;

import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.particle.Particle;
import org.tobi29.scapes.entity.particle.ParticleManager;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ParticleExplosion extends Particle {
    private double time, puff;
    private int state;

    public ParticleExplosion(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, double time) {
        super(particleManager, pos, speed,
                new AABB(-0.125f, -0.125f, -0.125f, 0.125f, 0.125f, 0.125f));
        this.time = time;
        gravitationMultiplier = 0.01;
    }

    @Override
    public void renderParticle(float x, float y, float z, float r, float g,
            float b, float a, GraphicsSystem graphics, Shader shader) {
    }

    @Override
    public void update(double delta) {
        time -= delta;
        puff -= delta;
        Random random = ThreadLocalRandom.current();
        while (puff <= 0.0) {
            puff += 0.1;
            particleManager
                    .add(new ParticleExplosionTrail(particleManager, pos.now(),
                            speed.now(), random.nextFloat() * 360.0f, 1.0));
        }
        if (time <= 0.8 && state == 0) {
            particleManager.add(new ParticleSmoke(particleManager, pos.now(),
                    speed.now(), random.nextFloat() * 360.0f, 6.0));
            state = 1;
        }
        if (time <= 0.3 && state == 1) {
            particleManager.add(new ParticleSmoke(particleManager, pos.now(),
                    speed.now(), random.nextFloat() * 360.0f, 4.0));
            state = 2;
        }
        if (time <= 0.0) {
            particleManager.delete(this);
        }
    }
}
