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
import org.tobi29.scapes.entity.client.MobClient;

public class MobModelItem implements MobModel {
    private final MutableVector3 pos;
    private final MobClient entity;
    private final ItemStack item;
    private float dir;

    public MobModelItem(MobClient entity, ItemStack item) {
        this.entity = entity;
        pos = new MutableVector3d(entity.getPos());
        this.item = item;
    }

    @Override
    public float getPitch() {
        return 0.0f;
    }

    @Override
    public float getYaw() {
        return dir;
    }

    @Override
    public Vector3 getPos() {
        return pos.now();
    }

    @Override
    public void renderUpdate(GraphicsSystem graphics, WorldClient world) {
        double div = FastMath.max(1.0, graphics.getSync().getTPS() / 10.0);
        pos.plus(entity.getPos().minus(pos.now()).div(div));
        dir += graphics.getSync().getSpeedFactor() * 45.0f;
        dir %= 360.0f;
    }

    @Override
    public void render(GraphicsSystem graphics, WorldClient world, Cam cam,
            Shader shader) {
        float posRenderX = (float) (pos.doubleX() - cam.position.doubleX());
        float posRenderY = (float) (pos.doubleY() - cam.position.doubleY());
        float posRenderZ = (float) (pos.doubleZ() - cam.position.doubleZ());
        OpenGL openGL = graphics.getOpenGL();
        openGL.setAttribute2f(4, world.getTerrain()
                        .getBlockLight(FastMath.floor(entity.getX()),
                                FastMath.floor(entity.getY()),
                                FastMath.floor(entity.getZ())) / 15.0f,
                world.getTerrain().getSunLight(FastMath.floor(entity.getX()),
                        FastMath.floor(entity.getY()),
                        FastMath.floor(entity.getZ())) / 15.0f);
        MatrixStack matrixStack = graphics.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(posRenderX, posRenderY, posRenderZ);
        matrix.scale(0.4f, 0.4f, 0.4f);
        matrix.rotate(dir, 0.0f, 0.0f, 1.0f);
        item.getMaterial()
                .render(item, graphics, shader, 1.0f, 1.0f, 1.0f, 1.0f);
        matrixStack.pop();
    }
}
