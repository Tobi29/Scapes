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

import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.model.Box;

public class ParticleFallenBodyPart extends Particle {
    private final Box box;
    private final boolean culling;
    private final Texture texture;
    private Vector3 rotation;
    private double time, xRot, zRot;

    public ParticleFallenBodyPart(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, Box box, Texture texture, Vector3 rotation,
            double xRot, double zRot, boolean culling) {
        super(particleManager, pos, speed,
                new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY,
                        box.maxZ));
        this.box = box;
        this.texture = texture;
        this.rotation = rotation;
        this.xRot = xRot;
        this.zRot = zRot;
        this.culling = culling;
    }

    @Override
    public void renderParticle(float x, float y, float z, float r, float g,
            float b, float a, GraphicsSystem graphics, Shader shader) {
        graphics.getTextureManager().bind(texture, graphics);
        OpenGL openGL = graphics.getOpenGL();
        if (!culling) {
            openGL.disableCulling();
        }
        MatrixStack matrixStack = graphics.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(x, y, z);
        matrix.rotate((float) zRot, 0, 0, 1);
        matrix.rotate((float) xRot, 1, 0, 0);
        box.render(r, g, b, a, graphics, shader);
        matrixStack.pop();
        if (!culling) {
            openGL.enableCulling();
        }
    }

    @Override
    public void update(double delta) {
        if (ground) {
            rotation = rotation.div(1.0 + 2.0 * delta);
        }
        xRot += rotation.doubleX();
        zRot += rotation.doubleZ();
        time += delta;
        if (time >= 6.0) {
            particleManager.delete(this);
        }
    }
}
