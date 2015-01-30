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

public class ParticleSnowflake extends Particle {
    private static final float SIZE = 0.075f;
    private static final VAO VAO = VAOUtility.createVTNI(
            new float[]{-1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f,
                    -1.0f, 0.0f, 1.0f},
            new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
            new float[]{0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
                    0.0f, -1.0f, 0.0f}, new int[]{0, 1, 2, 3, 0, 2},
            RenderType.TRIANGLES);
    private final double dir;
    private double time;

    public ParticleSnowflake(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, double dir) {
        super(particleManager, pos, speed,
                new AABB(-SIZE, -SIZE, -SIZE, SIZE, SIZE, SIZE));
        this.dir = dir;
    }

    @Override
    public void move(double delta) {
        if (!ground) {
            speed.setZ(-8.0);
            super.move(delta);
        }
    }

    @Override
    public void renderParticle(float x, float y, float z, float r, float g,
            float b, float a, GraphicsSystem graphics, Shader shader) {
        MatrixStack matrixStack = graphics.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(x, y, z);
        double camDir = FastMath.pointDirection(x, y, 0.0, 0.0);
        matrix.rotate((float) camDir + 90, 0, 0, 1);
        matrix.rotate((float) (FastMath.atan2(z, FastMath.length(x, y)) *
                FastMath.RAD_2_DEG), 1, 0, 0);
        matrix.rotate((float) (camDir + dir), 0, 1, 0);
        graphics.getTextureManager().unbind(graphics);
        OpenGL openGL = graphics.getOpenGL();
        openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, r, g, b, a);
        matrix.scale(SIZE, 1.0f, SIZE);
        VAO.render(graphics, shader);
        matrixStack.pop();
    }

    @Override
    public void update(double delta) {
        time += delta;
        if (ground && time < 3.0) {
            time = 3.0;
        }
        if (time >= 4.0) {
            particleManager.delete(this);
        }
    }
}
