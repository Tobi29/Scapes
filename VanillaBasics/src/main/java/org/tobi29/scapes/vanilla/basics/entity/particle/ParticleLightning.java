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
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.particle.Particle;
import org.tobi29.scapes.entity.particle.ParticleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ParticleLightning extends Particle {
    private static final VAO[] VAOS;

    static {
        VAOS = new VAO[16];
        for (int i = 0; i < VAOS.length; i++) {
            List<Line> lines = createLighting();
            float[] vertex = new float[lines.size() * 6];
            float[] normal = new float[lines.size() * 6];
            int j = 0;
            for (Line line : lines) {
                vertex[j] = line.start.floatX();
                normal[j++] = 0.0f;
                vertex[j] = line.start.floatY();
                normal[j++] = 0.0f;
                vertex[j] = line.start.floatZ();
                normal[j++] = 1.0f;
                vertex[j] = line.end.floatX();
                normal[j++] = 0.0f;
                vertex[j] = line.end.floatY();
                normal[j++] = 0.0f;
                vertex[j] = line.end.floatZ();
                normal[j++] = 1.0f;
            }
            int[] index = new int[(lines.size() << 1)];
            for (j = 0; j < index.length; j++) {
                index[j] = j;
            }
            VAOS[i] = VAOUtility
                    .createVNI(vertex, normal, index, RenderType.LINES);
        }
    }

    private final VAO vao;
    private double time;

    public ParticleLightning(ParticleManager particleManager, Vector3 pos,
            Vector3 speed) {
        super(particleManager, pos, speed, new AABB(-5, -5, -5, 5, 5, 5));
        Random random = ThreadLocalRandom.current();
        vao = VAOS[random.nextInt(VAOS.length)];
    }

    private static List<Line> createLighting() {
        Random random = ThreadLocalRandom.current();
        List<Line> lines = new ArrayList<>();
        double x = 0, y = 0;
        Vector3 start = Vector3d.ZERO;
        for (double z = 10.0; z < 100.0; z += random.nextDouble() * 4 + 2) {
            x += random.nextDouble() * 6.0 - 3.0;
            y += random.nextDouble() * 6.0 - 3.0;
            Vector3 end = new Vector3d(x, y, z);
            lines.add(new Line(start, end));
            start = end;
            if (random.nextInt(2) == 0 && z > 40) {
                createLightingArm(lines, x, y, z);
            }
        }
        return lines;
    }

    private static void createLightingArm(List<Line> lines, double x, double y,
            double z) {
        Random random = ThreadLocalRandom.current();
        Vector3 start = new Vector3d(x, y, z);
        double dir = random.nextDouble() * FastMath.TWO_PI;
        double xs = FastMath.cosTable(dir);
        double ys = FastMath.sinTable(dir);
        dir = FastMath.pow(random.nextDouble(), 6.0) * 20.0 + 0.2;
        for (int i = 0; i < random.nextInt(30) + 4; i++) {
            x += xs * random.nextDouble() * dir;
            y += ys * random.nextDouble() * dir;
            Vector3 end = new Vector3d(x, y, z);
            lines.add(new Line(start, end));
            start = end;
            z -= random.nextDouble() * 4.0 + 2.0;
            if (z < 20.0) {
                return;
            }
        }
    }

    @Override
    public void move(double delta) {
    }

    @Override
    public void renderParticle(float x, float y, float z, float r, float g,
            float b, float a, GL gl, Shader shader) {
        OpenGL openGL = gl.getOpenGL();
        MatrixStack matrixStack = gl.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(x, y, z);
        gl.getTextureManager().unbind(gl);
        openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, r * 0.2f, g * 0.7f,
                b * 1.0f, a * 0.7f);
        openGL.setBlending(BlendingMode.ADD);
        vao.render(gl, shader);
        matrixStack.pop();
        openGL.setBlending(BlendingMode.NORMAL);
    }

    @Override
    public void update(double delta) {
        time += delta;
        if (time >= 0.5) {
            particleManager.delete(this);
        }
    }

    private static class Line {
        private final Vector3 start, end;

        public Line(Vector3 start, Vector3 end) {
            this.start = start;
            this.end = end;
        }
    }
}
