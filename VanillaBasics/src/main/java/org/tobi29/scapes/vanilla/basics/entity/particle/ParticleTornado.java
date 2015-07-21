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

import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.particle.Particle;
import org.tobi29.scapes.entity.particle.ParticleManager;

public class ParticleTornado extends Particle {
    private static final VAO VAO = VAOUtility.createVTNI(
            new float[]{-1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f,
                    -1.0f, 0.0f, 1.0f},
            new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
            new float[]{0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
                    0.0f, -1.0f, 0.0f}, new int[]{0, 1, 2, 3, 0, 2},
            RenderType.TRIANGLES);
    private final float dir;
    private final Vector3 basePos;
    private final double baseSpin, widthRandom;
    private double time, spin, width;

    public ParticleTornado(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, float dir, double time, double spin, double baseSpin,
            double widthRandom) {
        super(particleManager, pos, speed,
                new AABB(-0.125f, -0.125f, -0.125f, 0.125f, 0.125f, 0.125f));
        basePos = pos;
        this.dir = dir;
        this.time = time;
        this.spin = spin;
        this.baseSpin = baseSpin * FastMath.DEG_2_RAD;
        this.widthRandom = widthRandom;
    }

    @Override
    public void move(double delta) {
        pos.plusZ(18.0 * delta);
    }

    @Override
    public void renderParticle(float x, float y, float z, float r, float g,
            float b, float a, GL gl, Shader shader) {
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(x, y, z);
        double camDir = FastMath.pointDirection(x, y, 0.0, 0.0);
        matrix.rotate((float) camDir + 90, 0, 0, 1);
        matrix.rotate((float) (FastMath.atan2(z, FastMath.length(x, y)) *
                FastMath.RAD_2_DEG), 1, 0, 0);
        matrix.rotate((float) (camDir + dir), 0, 1, 0);
        gl.textures()
                .bind("VanillaBasics:image/entity/particle/Cloud", gl);
        gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, r, g, b, a);
        float size = (float) width + 1;
        matrix.scale(size, 1.0f, size);
        VAO.render(gl, shader);
        matrixStack.pop();
    }

    @Override
    public void update(double delta) {
        time -= delta;
        if (time <= 0.0) {
            particleManager.delete(this);
        } else {
            width += 0.8 * delta;
            spin += 220.0 * delta;
            spin %= 360.0;
            double s = spin * FastMath.DEG_2_RAD;
            pos.setX(basePos.doubleX() +
                    FastMath.cosTable(s) * width * widthRandom +
                    FastMath.cosTable(baseSpin) * width);
            pos.setY(basePos.doubleY() +
                    FastMath.sinTable(s) * width * widthRandom +
                    FastMath.sinTable(baseSpin) * width * 3.0);
        }
    }
}
