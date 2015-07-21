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
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
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

    static {
        BODY = new Box(0.015625f, -5, -6, -5, 5, 6, 5, 0, 0);
        HEAD = new Box(0.015625f, -4, 0, -5, 4, 8, 4, 0, 22);
        LEG_FRONT_LEFT = new Box(0.015625f, -2, -2, -6, 2, 2, 0, 44, 0);
        LEG_FRONT_RIGHT = new Box(0.015625f, -2, -2, -6, 2, 2, 0, 44, 10);
        LEG_BACK_LEFT = new Box(0.015625f, -2, -2, -6, 2, 2, 0, 44, 20);
        LEG_BACK_RIGHT = new Box(0.015625f, -2, -2, -6, 2, 2, 0, 44, 30);
    }

    private final MobLivingClient entity;
    private final Texture texture;
    private final MutableVector3 pos;
    private double swing, moveSpeedRender;
    private float xRotRender, zRotRender;

    public MobLivingModelPig(MobLivingClient entity, Texture texture) {
        this.entity = entity;
        pos = new MutableVector3d(entity.pos());
        this.texture = texture;
    }

    @Override
    public Vector3 pos() {
        return pos.now();
    }

    @Override
    public void shapeAABB(AABB aabb) {
        aabb.minX = pos.doubleX() - 0.75;
        aabb.minY = pos.doubleY() - 0.75;
        aabb.minZ = pos.doubleZ() - 0.45;
        aabb.maxX = pos.doubleX() + 0.75;
        aabb.maxY = pos.doubleY() + 0.75;
        aabb.maxZ = pos.doubleZ() + 0.7;
    }

    @Override
    public void renderUpdate(double delta) {
        double factorPos = FastMath.min(1.0, delta * 10.0);
        double factorRot = FastMath.min(1.0, delta * 20.0);
        double factorSpeed = FastMath.min(1.0, delta * 5.0);
        double moveSpeed = FastMath.min(
                FastMath.sqrt(FastMath.length((Vector2) entity.speed())),
                2.0);
        xRotRender -=
                FastMath.angleDiff(entity.pitch(), xRotRender) * factorRot;
        zRotRender -= FastMath.angleDiff(entity.yaw(), zRotRender) * factorRot;
        pos.plus(entity.pos().minus(pos.now()).multiply(factorPos));
        swing += moveSpeed * 2.0 * delta;
        swing %= FastMath.TWO_PI;
        moveSpeedRender += (moveSpeed - moveSpeedRender) * factorSpeed;
    }

    @Override
    public void render(GL gl, WorldClient world, Cam cam,
            Shader shader) {
        float damageColor = (float) (1.0 - FastMath.min(1.0,
                FastMath.max(0.0f, entity.invincibleTicks() / 0.8)));
        float posRenderX = (float) (pos.doubleX() - cam.position.doubleX());
        float posRenderY = (float) (pos.doubleY() - cam.position.doubleY());
        float posRenderZ = (float) (pos.doubleZ() - cam.position.doubleZ());
        double swingDir = FastMath.cosTable(swing) * moveSpeedRender * 0.5;
        gl.setAttribute2f(4, world.terrain()
                        .blockLight(pos.intX(), pos.intY(), pos.intZ()) / 15.0f,
                world.terrain()
                        .sunLight(pos.intX(), pos.intY(), pos.intZ()) / 15.0f);
        texture.bind(gl);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(posRenderX, posRenderY, posRenderZ);
        matrix.rotate(zRotRender - 90, 0, 0, 1);
        BODY.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrix = matrixStack.push();
        matrix.translate(0, 0.3125f, 0.0625f);
        matrix.rotate(xRotRender, 1, 0, 0);
        HEAD.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(-0.125f, 0.1875f, -0.3125f);
        matrix.rotate((float) swingDir * 30, 1, 0, 0);
        LEG_FRONT_LEFT.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(0.125f, 0.1875f, -0.3125f);
        matrix.rotate((float) -swingDir * 30, 1, 0, 0);
        LEG_FRONT_RIGHT
                .render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(-0.125f, -0.1875f, -0.3125f);
        matrix.rotate((float) -swingDir * 30, 1, 0, 0);
        LEG_BACK_LEFT.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(0.125f, -0.1875f, -0.3125f);
        matrix.rotate((float) swingDir * 30, 1, 0, 0);
        LEG_BACK_RIGHT.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrixStack.pop();
        matrixStack.pop();
    }

    @Override
    public float pitch() {
        return xRotRender;
    }

    @Override
    public float yaw() {
        return zRotRender;
    }
}
