/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.vanilla.basics.entity.model;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Matrix;
import org.tobi29.scapes.engine.graphics.MatrixStack;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.model.Box;
import org.tobi29.scapes.entity.model.EntityModel;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBellowsClient;

public class EntityModelBellows implements EntityModel {
    private final MutableVector3 pos;
    private final EntityBellowsClient entity;
    private final Box side, middle, pipe;
    private float scale;

    public EntityModelBellows(EntityModelBellowsShared shared,
            EntityBellowsClient entity) {
        this.entity = entity;
        pos = new MutableVector3d(entity.pos());
        side = shared.side;
        middle = shared.middle;
        pipe = shared.pipe;
    }

    @Override
    public Vector3 pos() {
        return pos.now();
    }

    @Override
    public void shapeAABB(AABB aabb) {
        aabb.minX = pos.doubleX() - 0.5;
        aabb.minY = pos.doubleY() - 0.5;
        aabb.minZ = pos.doubleZ() - 0.5;
        aabb.maxX = pos.doubleX() + 0.5;
        aabb.maxY = pos.doubleY() + 0.5;
        aabb.maxZ = pos.doubleZ() + 0.5;
    }

    @Override
    public void renderUpdate(double delta) {
        double factor = FastMath.min(1.0, delta * 10.0);
        pos.plus(entity.pos().minus(pos.now()).multiply(factor));
        float value = entity.scale();
        scale += FastMath.diff(scale,
                (value > 1.0f ? 2.0f - value : value) * 0.4f + 0.4f, 2) *
                factor;
        scale %= 2;
    }

    @Override
    public void render(GL gl, WorldClient world, Cam cam, Shader shader) {
        float posRenderX = (float) (pos.doubleX() - cam.position.doubleX());
        float posRenderY = (float) (pos.doubleY() - cam.position.doubleY());
        float posRenderZ = (float) (pos.doubleZ() - cam.position.doubleZ());
        gl.setAttribute2f(4,
                world.terrain().blockLight(pos.intX(), pos.intY(), pos.intZ()) /
                        15.0f,
                world.terrain().sunLight(pos.intX(), pos.intY(), pos.intZ()) /
                        15.0f);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(posRenderX, posRenderY, posRenderZ);
        matrix = matrixStack.push();
        matrix.scale(1.0f, 1.0f, scale);
        gl.textures().bind("VanillaBasics:image/terrain/tree/birch/Planks", gl);
        middle.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        gl.textures().bind("VanillaBasics:image/terrain/tree/oak/Planks", gl);
        matrix.translate(0.0f, 0.0f, scale * 0.5f);
        side.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(0.0f, 0.0f, -scale * 0.5f);
        side.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader);
        matrixStack.pop();
        side.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader);
        matrix = matrixStack.push();
        switch (entity.face()) {
            case DOWN:
                matrix.rotate(180, 1, 0, 0);
                break;
            case NORTH:
                matrix.rotate(90, 1, 0, 0);
                break;
            case EAST:
                matrix.rotate(90, 0, 1, 0);
                break;
            case SOUTH:
                matrix.rotate(270, 1, 0, 0);
                break;
            case WEST:
                matrix.rotate(270, 0, 1, 0);
                break;
        }
        gl.textures().bind("VanillaBasics:image/terrain/device/Anvil", gl);
        pipe.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader);
        matrixStack.pop();
        matrixStack.pop();
    }
}
