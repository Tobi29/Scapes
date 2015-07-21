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

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.particle.Particle;
import org.tobi29.scapes.entity.particle.ParticleManager;
import org.tobi29.scapes.vanilla.basics.generator.WorldSkyboxOverworld;

public class ParticleRaindrop extends Particle {
    private static final VAO VAO = VAOUtility
            .createVNI(new float[]{0, 0.0f, -1.5f, 0, 0.0f, 1.5f},
                    new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f},
                    new int[]{0, 1}, RenderType.LINES);
    private double time;

    public ParticleRaindrop(ParticleManager particleManager, Vector3 pos,
            Vector3 speed) {
        super(particleManager, pos, speed,
                new AABB(-0.05, -0.05, -1.5, 0.05, 0.05, 1.5));
    }

    @Override
    public void move(double delta) {
        WorldClient world = particleManager.world();
        speed.div(airFriction);
        pos.plusX(speed.doubleX());
        pos.plusY(speed.doubleY());
        pos.plusZ(-180.0 * delta);
        if (inWater || ground || slidingWall ||
                world.terrain().highestBlockZAt(pos.intX(), pos.intY()) >
                        pos.doubleZ()) {
            if (time > 0.1) {
                ((WorldSkyboxOverworld) world.scene().skybox())
                        .addRaindrop();
                particleManager.delete(this);
            }
        } else {
            time += delta;
            if (time >= 3.0) {
                particleManager.delete(this);
            }
        }
    }

    @Override
    public void renderParticle(float x, float y, float z, float r, float g,
            float b, float a, GL gl, Shader shader) {
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(x, y, z);
        gl.textures().unbind(gl);
        gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, g * 0.3f, b * 0.5f,
                a * 0.3f);
        VAO.render(gl, shader);
        matrixStack.pop();
    }

    @Override
    public void update(double delta) {
    }
}
