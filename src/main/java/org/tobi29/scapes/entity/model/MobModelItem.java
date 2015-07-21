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

package org.tobi29.scapes.entity.model;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.client.MobClient;

public class MobModelItem implements MobModel {
    private final MutableVector3 pos;
    private final MobClient entity;
    private final ItemStack item;
    private float dir;

    public MobModelItem(MobClient entity, ItemStack item) {
        this.entity = entity;
        pos = new MutableVector3d(entity.pos());
        this.item = item;
    }

    @Override
    public float pitch() {
        return 0.0f;
    }

    @Override
    public float yaw() {
        return dir;
    }

    @Override
    public Vector3 pos() {
        return pos.now();
    }

    @Override
    public void shapeAABB(AABB aabb) {
        aabb.minX = pos.doubleX() - 0.1;
        aabb.minY = pos.doubleY() - 0.1;
        aabb.minZ = pos.doubleZ() - 0.1;
        aabb.maxX = pos.doubleX() + 0.1;
        aabb.maxY = pos.doubleY() + 0.1;
        aabb.maxZ = pos.doubleZ() + 0.1;
    }

    @Override
    public void renderUpdate(double delta) {
        double factor = FastMath.min(1.0, delta * 10.0);
        pos.plus(entity.pos().minus(pos.now()).multiply(factor));
        dir += 45.0 * delta;
        dir %= 360.0f;
    }

    @Override
    public void render(GL gl, WorldClient world, Cam cam,
            Shader shader) {
        float posRenderX = (float) (pos.doubleX() - cam.position.doubleX());
        float posRenderY = (float) (pos.doubleY() - cam.position.doubleY());
        float posRenderZ = (float) (pos.doubleZ() - cam.position.doubleZ());
        gl.setAttribute2f(4, world.terrain()
                        .blockLight(FastMath.floor(entity.x()),
                                FastMath.floor(entity.y()),
                                FastMath.floor(entity.z())) / 15.0f,
                world.terrain().sunLight(FastMath.floor(entity.x()),
                        FastMath.floor(entity.y()),
                        FastMath.floor(entity.z())) / 15.0f);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(posRenderX, posRenderY, posRenderZ);
        matrix.scale(0.4f, 0.4f, 0.4f);
        matrix.rotate(dir, 0.0f, 0.0f, 1.0f);
        item.material().render(item, gl, shader, 1.0f, 1.0f, 1.0f, 1.0f);
        matrixStack.pop();
    }
}
