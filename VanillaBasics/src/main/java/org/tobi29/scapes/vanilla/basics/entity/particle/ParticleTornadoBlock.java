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

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.particle.Particle;
import org.tobi29.scapes.entity.particle.ParticleManager;

public class ParticleTornadoBlock extends Particle {
    private final float dir;
    private final Vector3 basePos;
    private final double baseSpin, widthRandom;
    private final ItemStack item;
    private double time, spin, width;

    public ParticleTornadoBlock(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, float dir, double time, double spin, double baseSpin,
            double widthRandom, BlockType type, int data) {
        super(particleManager, pos, speed,
                new AABB(-0.125f, -0.125f, -0.125f, 0.125f, 0.125f, 0.125f));
        basePos = pos;
        this.dir = dir;
        this.time = time;
        this.spin = spin;
        this.baseSpin = baseSpin * FastMath.DEG_2_RAD;
        this.widthRandom = widthRandom;
        item = new ItemStack(type, data);
    }

    @Override
    public void move(double delta) {
        pos.plusZ(18.0 * delta);
    }

    @Override
    public void renderParticle(float x, float y, float z, float r, float g,
            float b, float a, GraphicsSystem graphics, Shader shader) {
        MatrixStack matrixStack = graphics.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(x, y, z);
        matrix.rotate((float) spin, 0, 0, 1);
        matrix.rotate(dir, 1, 0, 0);
        item.getMaterial().render(item, graphics, shader, r, g, b, a);
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
            spin %= 360;
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
