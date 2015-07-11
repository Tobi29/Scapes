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

import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.Mesh;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2f;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ParticleBlock extends Particle {
    private static final float SIZE = 0.125f;
    private static final Map<String, VAO> BLOCKS = new ConcurrentHashMap<>();
    private final float r, g, b, a, dir, texSize;
    private final Vector2 texPos;
    private final Texture texture;
    private double time;

    public ParticleBlock(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, TerrainTexture tex, float dir, float r, float g,
            float b, float a) {
        super(particleManager, pos, speed,
                new AABB(-SIZE, -SIZE, -SIZE, SIZE, SIZE, SIZE));
        this.dir = dir;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        texSize = tex.getSize() / 4.0f;
        Random random = ThreadLocalRandom.current();
        texPos = new Vector2f(tex.getX() + random.nextInt(3) * texSize,
                tex.getY() + random.nextInt(3) * texSize);
        time = random.nextDouble() * 1.0 + 2.0;
        texture = tex.getTerrainTextureRegistry().getTexture();
    }

    public static void clear() {
        BLOCKS.clear();
    }

    @Override
    public void renderParticle(float x, float y, float z, float r, float g,
            float b, float a, GL gl, Shader shader) {
        if (!BLOCKS.containsKey(texPos.floatX() + "/" + texPos.floatY())) {
            Mesh mesh = new Mesh(false, false);
            mesh.normal(0.0f, -1.0f, 0.0f);
            mesh.texture(texPos.floatX(), texPos.floatY() + texSize);
            mesh.vertex(-SIZE, 0.0f, -SIZE);
            mesh.texture(texPos.floatX() + texSize, texPos.floatY() + texSize);
            mesh.vertex(SIZE, 0.0f, -SIZE);
            mesh.texture(texPos.floatX() + texSize, texPos.floatY());
            mesh.vertex(SIZE, 0.0f, SIZE);
            mesh.texture(texPos.floatX(), texPos.floatY());
            mesh.vertex(-SIZE, 0.0f, SIZE);
            BLOCKS.put(texPos.floatX() + "/" + texPos.floatY(), mesh.finish());
        }
        MatrixStack matrixStack = gl.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(x, y, z - (float) FastMath.max(0.0, 1.0 - time));
        double camDir = FastMath.pointDirection(x, y, 0.0, 0.0);
        matrix.rotate((float) camDir + 90, 0, 0, 1);
        matrix.rotate((float) (FastMath.atan2(z, FastMath.length(x, y)) *
                FastMath.RAD_2_DEG), 1, 0, 0);
        matrix.rotate((float) (camDir + dir), 0, 1, 0);
        texture.bind(gl);
        OpenGL openGL = gl.getOpenGL();
        openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, r * this.r, g * this.g,
                b * this.b, a * this.a);
        BLOCKS.get(texPos.floatX() + "/" + texPos.floatY()).render(gl, shader);
        matrixStack.pop();
    }

    @Override
    public void update(double delta) {
        time -= delta;
        if (time <= 0.0) {
            particleManager.delete(this);
        }
    }
}
