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

package org.tobi29.scapes.entity.particle;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

public class ParticleManager {
    private final List<Particle> particles = new ArrayList<>(), addParticles =
            new ArrayList<>(), removeParticles = new ArrayList<>();
    private final WorldClient world;

    public ParticleManager(WorldClient world) {
        this.world = world;
    }

    public void add(Particle add) {
        synchronized (particles) {
            addParticles.add(add);
        }
    }

    public void delete(Particle del) {
        synchronized (particles) {
            removeParticles.add(del);
        }
    }

    public void render(GL gl, Cam cam) {
        Vector3 camPos = cam.position.now();
        Shader shader =
                gl.getShaderManager().getShader("Scapes:shader/Entity", gl);
        synchronized (particles) {
            particles.stream().sorted((particle1, particle2) -> {
                double distance1 =
                        FastMath.pointDistanceSqr(particle1.getPosRender(),
                                camPos);
                double distance2 =
                        FastMath.pointDistanceSqr(particle2.getPosRender(),
                                camPos);
                return distance1 == distance2 ? 0 :
                        distance1 < distance2 ? 1 : -1;
            }).forEach(particle -> particle.render(gl, cam, shader));
        }
    }

    public void update(double delta) {
        synchronized (particles) {
            particles.addAll(addParticles);
            addParticles.clear();
            particles.removeAll(removeParticles);
            removeParticles.clear();
        }
        particles.forEach(particle -> particle.update(delta));
        particles.forEach(particle -> particle.move(delta));
    }

    public WorldClient getWorld() {
        return world;
    }
}
