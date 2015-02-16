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

package org.tobi29.scapes.vanilla.basics.entity.model;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.model.Box;
import org.tobi29.scapes.entity.model.EntityModel;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBellowsClient;

public class EntityModelBellows implements EntityModel {
    private static final Box SIDE, MIDDLE, PIPE;
    private final MutableVector3 pos;
    private final EntityBellowsClient entity;
    private float scale;

    static {
        SIDE = new Box(0.0625f, -7, -7, -1, 7, 7, 1, 0, 0);
        MIDDLE = new Box(0.0625f, -6, -6, -7, 6, 6, 7, 0, 0);
        PIPE = new Box(0.0625f, -2, -2, -16, 2, 2, 0, 0, 0);
    }

    public EntityModelBellows(EntityBellowsClient entity) {
        this.entity = entity;
        pos = new MutableVector3d(entity.getPos());
    }

    @Override
    public Vector3 getPos() {
        return pos.now();
    }

    @Override
    public void renderUpdate(GraphicsSystem graphics, WorldClient world,
            double delta) {
        double div = 1.0 + 256.0 * delta;
        pos.plus(entity.getPos().minus(pos.now()).div(div));
        float value = entity.getScale();
        scale += FastMath.diff(scale,
                (value > 1.0f ? 2.0f - value : value) * 0.4f + 0.4f, 2) / div;
        scale %= 2;
    }

    @Override
    public void render(GraphicsSystem graphics, WorldClient world, Cam cam,
            Shader shader) {
        float posRenderX = (float) (pos.doubleX() - cam.position.doubleX());
        float posRenderY = (float) (pos.doubleY() - cam.position.doubleY());
        float posRenderZ = (float) (pos.doubleZ() - cam.position.doubleZ());
        OpenGL openGL = graphics.getOpenGL();
        openGL.setAttribute2f(4, world.getTerrain()
                .getBlockLight(pos.intX(), pos.intY(), pos.intZ()) / 15.0f,
                world.getTerrain()
                        .getSunLight(pos.intX(), pos.intY(), pos.intZ()) /
                        15.0f);
        MatrixStack matrixStack = graphics.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(posRenderX, posRenderY, posRenderZ);
        matrix = matrixStack.push();
        matrix.scale(1.0f, 1.0f, scale);
        graphics.getTextureManager()
                .bind("VanillaBasics:image/terrain/tree/planks/Birch",
                        graphics);
        MIDDLE.render(1.0f, 1.0f, 1.0f, 1.0f, graphics, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        graphics.getTextureManager()
                .bind("VanillaBasics:image/terrain/tree/planks/Oak", graphics);
        matrix.translate(0.0f, 0.0f, scale * 0.5f);
        SIDE.render(1.0f, 1.0f, 1.0f, 1.0f, graphics, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(0.0f, 0.0f, -scale * 0.5f);
        SIDE.render(1.0f, 1.0f, 1.0f, 1.0f, graphics, shader);
        matrixStack.pop();
        SIDE.render(1.0f, 1.0f, 1.0f, 1.0f, graphics, shader);
        matrix = matrixStack.push();
        switch (entity.getFace()) {
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
        graphics.getTextureManager()
                .bind("VanillaBasics:image/terrain/device/Anvil", graphics);
        PIPE.render(1.0f, 1.0f, 1.0f, 1.0f, graphics, shader);
        matrixStack.pop();
        matrixStack.pop();
    }
}
