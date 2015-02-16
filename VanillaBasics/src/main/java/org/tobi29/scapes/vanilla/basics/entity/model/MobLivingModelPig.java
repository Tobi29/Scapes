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
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.client.MobLivingClient;
import org.tobi29.scapes.entity.model.Box;
import org.tobi29.scapes.entity.model.MobModel;

public class MobLivingModelPig implements MobModel {
    private static final Box BODY, HEAD, LEG_FRONT_LEFT, LEG_FRONT_RIGHT,
            LEG_BACK_LEFT, LEG_BACK_RIGHT;
    private final MobLivingClient entity;
    private final Texture texture;
    private final MutableVector3 pos;

    static {
        BODY = new Box(0.015625f, -5, -6, -5, 5, 6, 5, 0, 0);
        HEAD = new Box(0.015625f, -4, 0, -5, 4, 8, 4, 0, 22);
        LEG_FRONT_LEFT = new Box(0.015625f, -2, -2, -6, 2, 2, 0, 44, 0);
        LEG_FRONT_RIGHT = new Box(0.015625f, -2, -2, -6, 2, 2, 0, 44, 10);
        LEG_BACK_LEFT = new Box(0.015625f, -2, -2, -6, 2, 2, 0, 44, 20);
        LEG_BACK_RIGHT = new Box(0.015625f, -2, -2, -6, 2, 2, 0, 44, 30);
    }

    private double swing, moveSpeedRender;
    private float xRotRender, zRotRender;

    public MobLivingModelPig(MobLivingClient entity, Texture texture) {
        this.entity = entity;
        pos = new MutableVector3d(entity.getPos());
        this.texture = texture;
    }

    @Override
    public Vector3 getPos() {
        return pos.now();
    }

    @Override
    public void renderUpdate(GraphicsSystem graphics, WorldClient world,
            double delta) {
        double divPos = 1.0 + 256.0 * delta;
        double divRot = 1.0 + 64.0 * delta;
        double divSpeed = 1.0 + 1024.0 * delta;
        double moveSpeed = FastMath.min(
                FastMath.sqrt(FastMath.length((Vector2) entity.getSpeed())),
                2.0);
        xRotRender -= FastMath.angleDiff(entity.getXRot(), xRotRender) / divRot;
        zRotRender -= FastMath.angleDiff(entity.getZRot(), zRotRender) / divRot;
        pos.plus(entity.getPos().minus(pos.now()).div(divPos));
        swing += moveSpeed * 2.0 * delta;
        swing %= FastMath.TWO_PI;
        moveSpeedRender += (moveSpeed - moveSpeedRender) / divSpeed;
    }

    @Override
    public void render(GraphicsSystem graphics, WorldClient world, Cam cam,
            Shader shader) {
        float damageColor = (float) (1.0 - FastMath.min(1.0,
                FastMath.max(0.0f, entity.getInvincibleTicks() / 0.8)));
        float posRenderX = (float) (pos.doubleX() - cam.position.doubleX());
        float posRenderY = (float) (pos.doubleY() - cam.position.doubleY());
        float posRenderZ = (float) (pos.doubleZ() - cam.position.doubleZ());
        double swingDir = FastMath.cosTable(swing) * moveSpeedRender * 0.5;
        OpenGL openGL = graphics.getOpenGL();
        openGL.setAttribute2f(4, world.getTerrain()
                        .getBlockLight(pos.intX(), pos.intY(), pos.intZ()) /
                        15.0f, world.getTerrain()
                        .getSunLight(pos.intX(), pos.intY(), pos.intZ()) /
                        15.0f);
        texture.bind(graphics);
        MatrixStack matrixStack = graphics.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(posRenderX, posRenderY, posRenderZ);
        matrix.rotate(zRotRender - 90, 0, 0, 1);
        BODY.render(1.0f, damageColor, damageColor, 1.0f, graphics, shader);
        matrix = matrixStack.push();
        matrix.translate(0, 0.3125f, 0.0625f);
        matrix.rotate(xRotRender, 1, 0, 0);
        HEAD.render(1.0f, damageColor, damageColor, 1.0f, graphics, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(-0.125f, 0.1875f, -0.3125f);
        matrix.rotate((float) swingDir * 30, 1, 0, 0);
        LEG_FRONT_LEFT
                .render(1.0f, damageColor, damageColor, 1.0f, graphics, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(0.125f, 0.1875f, -0.3125f);
        matrix.rotate((float) -swingDir * 30, 1, 0, 0);
        LEG_FRONT_RIGHT
                .render(1.0f, damageColor, damageColor, 1.0f, graphics, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(-0.125f, -0.1875f, -0.3125f);
        matrix.rotate((float) -swingDir * 30, 1, 0, 0);
        LEG_BACK_LEFT
                .render(1.0f, damageColor, damageColor, 1.0f, graphics, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(0.125f, -0.1875f, -0.3125f);
        matrix.rotate((float) swingDir * 30, 1, 0, 0);
        LEG_BACK_RIGHT
                .render(1.0f, damageColor, damageColor, 1.0f, graphics, shader);
        matrixStack.pop();
        matrixStack.pop();
    }

    @Override
    public float getPitch() {
        return xRotRender;
    }

    @Override
    public float getYaw() {
        return zRotRender;
    }
}
