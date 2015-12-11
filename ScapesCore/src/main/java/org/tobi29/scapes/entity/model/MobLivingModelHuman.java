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
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.*;
import org.tobi29.scapes.entity.WieldMode;
import org.tobi29.scapes.entity.client.MobLivingEquippedClient;
import org.tobi29.scapes.entity.particle.ParticleFallenBodyPart;
import org.tobi29.scapes.entity.particle.ParticleManager;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobLivingModelHuman implements MobModel {
    private static final Box BODY, HEAD, LEG_NORMAL_LEFT, LEG_NORMAL_RIGHT,
            ARM_NORMAL_LEFT, ARM_NORMAL_RIGHT, LEG_THIN_LEFT, LEG_THIN_RIGHT,
            ARM_THIN_LEFT, ARM_THIN_RIGHT;

    static {
        BODY = new Box(0.015625f, -4, -2, -6, 4, 2, 6, 0, 0);
        HEAD = new Box(0.015625f, -4, -4, 0, 4, 4, 8, 0, 32);
        LEG_NORMAL_LEFT = new Box(0.015625f, -2, -2, -10, 2, 2, 2, 24, 0);
        LEG_NORMAL_RIGHT = new Box(0.015625f, -2, -2, -10, 2, 2, 2, 40, 0);
        ARM_NORMAL_LEFT = new Box(0.015625f, -4, -2, -10, 0, 2, 2, 24, 16);
        ARM_NORMAL_RIGHT = new Box(0.015625f, 0, -2, -10, 4, 2, 2, 40, 16);
        LEG_THIN_LEFT = new Box(0.015625f, -1, -1, -10, 1, 1, 2, 24, 0);
        LEG_THIN_RIGHT = new Box(0.015625f, -1, -1, -10, 1, 1, 2, 32, 0);
        ARM_THIN_LEFT = new Box(0.015625f, -2, -1, -10, 0, 1, 2, 24, 16);
        ARM_THIN_RIGHT = new Box(0.015625f, 0, -1, -10, 2, 1, 2, 32, 16);
    }

    private final MobLivingEquippedClient entity;
    private final Texture texture;
    private final boolean culling;
    private final MutableVector3 pos;
    private final Box legLeft, legRight, armLeft, armRight;
    private double swing, lazyName, moveSpeedRender;
    private float armDirLeft, armDirRight, armDirLeftRender, armDirRightRender,
            armDirLeft2, armDirRight2, pitch, yaw;

    public MobLivingModelHuman(MobLivingEquippedClient entity,
            Texture texture) {
        this(entity, texture, false);
    }

    public MobLivingModelHuman(MobLivingEquippedClient entity, Texture texture,
            boolean thin) {
        this(entity, texture, thin, true);
    }

    public MobLivingModelHuman(MobLivingEquippedClient entity, Texture texture,
            boolean thin, boolean culling) {
        this.entity = entity;
        pos = new MutableVector3d(entity.pos());
        this.texture = texture;
        this.culling = culling;
        if (thin) {
            legLeft = LEG_THIN_LEFT;
            legRight = LEG_THIN_RIGHT;
            armLeft = ARM_THIN_LEFT;
            armRight = ARM_THIN_RIGHT;
        } else {
            legLeft = LEG_NORMAL_LEFT;
            legRight = LEG_NORMAL_RIGHT;
            armLeft = ARM_NORMAL_LEFT;
            armRight = ARM_NORMAL_RIGHT;
        }
    }

    public static void particles(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, Vector3 rot, Texture texture) {
        particles(particleManager, pos, speed, rot, texture, false);
    }

    public static void particles(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, Vector3 rot, Texture texture, boolean thin) {
        particles(particleManager, pos, speed, rot, texture, thin, true);
    }

    public static void particles(ParticleManager particleManager, Vector3 pos,
            Vector3 speed, Vector3 rot, Texture texture, boolean thin,
            boolean culling) {
        Box legLeft, legRight, armLeft, armRight;
        if (thin) {
            legLeft = LEG_THIN_LEFT;
            legRight = LEG_THIN_RIGHT;
            armLeft = ARM_THIN_LEFT;
            armRight = ARM_THIN_RIGHT;
        } else {
            legLeft = LEG_NORMAL_LEFT;
            legRight = LEG_NORMAL_RIGHT;
            armLeft = ARM_NORMAL_LEFT;
            armRight = ARM_NORMAL_RIGHT;
        }
        double z = rot.doubleZ() * FastMath.DEG_2_RAD;
        Random random = ThreadLocalRandom.current();
        particleManager.add(new ParticleFallenBodyPart(particleManager,
                pos.plus(new Vector3d(0, 0, 0.7)), speed.plus(
                new Vector3d(random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.4 - 0.2, random.nextDouble())),
                HEAD, texture, FastMath.normalizeSafe(
                new Vector3d(random.nextDouble() * 4 - 2,
                        random.nextDouble() * 4 - 2,
                        random.nextDouble() * 4 - 2)).multiply(8),
                rot.doubleX(), rot.doubleZ(), culling));
        particleManager.add(new ParticleFallenBodyPart(particleManager,
                pos.plus(new Vector3d(0, 0, 0.125)), speed.plus(
                new Vector3d(random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.2)), BODY, texture,
                FastMath.normalizeSafe(new Vector3d(random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1)).multiply(8), 0,
                rot.doubleZ(), culling));
        particleManager.add(new ParticleFallenBodyPart(particleManager,
                pos.plus(new Vector3d(FastMath.cosTable(z) * 0.125,
                        FastMath.cosTable(z) * 0.125, -0.375)), speed.plus(
                new Vector3d(random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.2)), legLeft, texture,
                FastMath.normalizeSafe(new Vector3d(random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1)).multiply(8), 0,
                rot.doubleZ(), culling));
        particleManager.add(new ParticleFallenBodyPart(particleManager,
                pos.plus(new Vector3d(FastMath.cosTable(z) * -0.125,
                        FastMath.cosTable(z) * -0.125, -0.375)), speed.plus(
                new Vector3d(random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.2)), legRight, texture,
                FastMath.normalizeSafe(new Vector3d(random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1)).multiply(8), 0,
                rot.doubleZ(), culling));
        particleManager.add(new ParticleFallenBodyPart(particleManager,
                pos.plus(new Vector3d(FastMath.cosTable(z) * -0.375,
                        FastMath.cosTable(z) * -0.375, 0.375)), speed.plus(
                new Vector3d(random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.2)), armLeft, texture,
                FastMath.normalizeSafe(new Vector3d(random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1)).multiply(8), 0,
                rot.doubleZ(), culling));
        particleManager.add(new ParticleFallenBodyPart(particleManager,
                pos.plus(new Vector3d(FastMath.cosTable(z) * 0.375,
                        FastMath.cosTable(z) * 0.375, 0.375)), speed.plus(
                new Vector3d(random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.4 - 0.2,
                        random.nextDouble() * 0.2)), armRight, texture,
                FastMath.normalizeSafe(new Vector3d(random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1,
                        random.nextDouble() * 2 - 1)).multiply(8), 0,
                rot.doubleZ(), culling));
    }

    @Override
    public Vector3 pos() {
        return pos.now();
    }

    @Override
    public void shapeAABB(AABB aabb) {
        aabb.minX = pos.doubleX() - 1.0;
        aabb.minY = pos.doubleY() - 1.0;
        aabb.minZ = pos.doubleZ() - 1.0;
        aabb.maxX = pos.doubleX() + 1.0;
        aabb.maxY = pos.doubleY() + 1.0;
        aabb.maxZ = pos.doubleZ() + 1.2;
    }

    @Override
    public void renderUpdate(double delta) {
        double factorPos = FastMath.min(1.0, delta * 20.0);
        double factorRot = FastMath.min(1.0, delta * 40.0);
        double factorSpeed = FastMath.min(1.0, delta * 5.0);
        double moveSpeed = FastMath.min(
                FastMath.sqrt(FastMath.length((Vector2) entity.speed())), 2.0);
        pitch -= FastMath.angleDiff(entity.pitch(), pitch) * factorRot;
        yaw -= FastMath.angleDiff(entity.yaw(), yaw) * factorRot;
        pos.plus(entity.pos().minus(pos.now()).multiply(factorPos));
        swing += moveSpeed * 2.0 * delta;
        swing %= FastMath.TWO_PI;
        lazyName += delta;
        lazyName %= FastMath.TWO_PI;
        moveSpeedRender += (moveSpeed - moveSpeedRender) * factorSpeed;
        float newChargeLeft = entity.leftCharge();
        ItemStack weaponLeft = entity.leftWeapon();
        if (newChargeLeft > 0.01f) {
            armDirLeft2 += 2.4 * delta;
            armDirLeft2 -= armDirLeft2 * 2.7 * delta;
        } else {
            if (armDirLeft > 0.01f) {
                if (armDirLeft >= 0.45f) {
                    if (weaponLeft.material().isWeapon(weaponLeft)) {
                        armDirLeftRender = -1.1f;
                    } else {
                        armDirLeftRender = -1.5f;
                    }
                }
            }
            if (armDirLeft < 0.01f) {
                armDirLeft2 -= armDirLeft2 * 2.7 * delta;
            }
        }
        armDirLeft = newChargeLeft;
        if (weaponLeft.material().isWeapon(weaponLeft) &&
                armDirLeftRender < -0.6f) {
            armDirLeftRender +=
                    (armDirLeft - armDirLeftRender) * factorPos / 12.0;
        } else {
            armDirLeftRender += (armDirLeft - armDirLeftRender) * factorPos;
        }
        float newChargeRight = entity.rightCharge();
        ItemStack weaponRight = entity.rightWeapon();
        if (newChargeRight > 0.01f) {
            armDirRight2 += 2.4 * delta;
            armDirRight2 -= armDirRight2 * 2.7 * delta;
        } else {
            if (armDirRight > 0.01f) {
                if (armDirRight >= 0.45f) {
                    if (weaponRight.material().isWeapon(weaponRight)) {
                        armDirRightRender = -1.1f;
                    } else {
                        armDirRightRender = -1.5f;
                    }
                }
            }
            if (armDirRight < 0.01f) {
                armDirRight2 -= armDirRight2 * 2.7 * delta;
            }
        }
        armDirRight = newChargeRight;
        if (weaponRight.material().isWeapon(weaponRight) &&
                armDirRightRender < -0.6f) {
            armDirRightRender +=
                    (armDirRight - armDirRightRender) * factorPos / 12.0;
        } else {
            armDirRightRender += (armDirRight - armDirRightRender) * factorPos;
        }
    }

    @Override
    public void render(GL gl, WorldClient world, Cam cam, Shader shader) {
        float damageColor = (float) (1.0 - FastMath.min(1.0,
                FastMath.max(0.0f, entity.invincibleTicks() / 0.8)));
        float posRenderX = (float) (pos.doubleX() - cam.position.doubleX());
        float posRenderY = (float) (pos.doubleY() - cam.position.doubleY());
        float posRenderZ = (float) (pos.doubleZ() - cam.position.doubleZ());
        double l = pitch * 0.004;
        if (l < 0.0) {
            double d = yaw * FastMath.DEG_2_RAD;
            posRenderX += FastMath.cosTable(d) * l;
            posRenderY += FastMath.sinTable(d) * l;
        }
        double swingDir = FastMath.cosTable(swing) * moveSpeedRender * 0.5;
        double lazyNameDir = (FastMath.cosTable(lazyName) * 0.5 + 0.5) *
                (moveSpeedRender * 0.2 + 0.2);
        gl.setAttribute2f(4, world.terrain()
                        .blockLight(FastMath.floor(entity.x()),
                                FastMath.floor(entity.y()),
                                FastMath.floor(entity.z())) / 15.0f,
                world.terrain().sunLight(FastMath.floor(entity.x()),
                        FastMath.floor(entity.y()),
                        FastMath.floor(entity.z())) / 15.0f);
        texture.bind(gl);
        if (!culling) {
            gl.disableCulling();
        }
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(posRenderX, posRenderY, posRenderZ);
        matrix.rotate(yaw - 90.0f, 0.0f, 0.0f, 1.0f);
        BODY.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrix = matrixStack.push();
        matrix.translate(0, 0, 0.375f);
        matrix.rotate(pitch, 1, 0, 0);
        HEAD.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(-0.125f, 0, -0.5f);
        matrix.rotate((float) swingDir * 30, 1, 0, 0);
        legLeft.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(0.125f, 0, -0.5f);
        matrix.rotate((float) -swingDir * 30, 1, 0, 0);
        legRight.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        matrixStack.pop();
        WieldMode wieldMode = entity.wieldMode();
        matrix = matrixStack.push();
        matrix.translate(-0.25f, 0, 0.25f);
        float rot, charge, charge2;
        ItemStack item;
        if (wieldMode == WieldMode.RIGHT) {
            item = entity.rightWeapon();
            charge = armDirRightRender;
            charge2 = armDirRight2 * 0.4f;
        } else {
            item = entity.leftWeapon();
            charge = armDirLeftRender;
            charge2 = armDirLeft2;
        }
        if (item.material() == world.air()) {
            matrix.rotate((float) lazyNameDir * 60, 0, 1, 0);
            matrix.rotate((float) -swingDir * 60, 1, 0, 0);
            rot = (float) FastMath.sinTable(charge * FastMath.PI);
            matrix.rotate(charge2 * -20, 0, 0, 1);
            matrix.rotate(rot * 50 + charge2 * 90 + pitch * charge2, 1, 0, 0);
        } else {
            if (wieldMode == WieldMode.DUAL) {
                matrix.rotate((float) lazyNameDir * 60, 0, 1, 0);
                matrix.rotate((float) -swingDir * 30, 1, 0, 0);
            } else {
                matrix.rotate((float) swingDir * 10, 1, 0, 0);
            }
            if (item.material().isWeapon(item)) {
                rot = (float) FastMath.sinTable(-charge * FastMath.PI);
                if (rot < 0 && charge > 0.0) {
                    rot /= 4.0f;
                }
                if (wieldMode == WieldMode.RIGHT) {
                    rot = -rot;
                }
                matrix.rotate(rot * -60.0f, 0.0f, 0.0f, 1.0f);
                matrix.rotate(rot * -60.0f, 0.0f, 1.0f, 0.0f);
                matrix.rotate(rot * 10.0f + 40.0f + charge2 * 45.0f +
                        pitch * charge2, 1.0f, 0.0f, 0.0f);
            } else {
                rot = (float) FastMath.sinTable(charge * FastMath.PI);
                matrix.rotate(charge2 * -20, 0, 0, 1);
                matrix.rotate(rot * 50 + 40 + charge2 * 45 +
                        pitch * charge2, 1, 0, 0);
                if (charge > -1.5f && charge < -0.5f) {
                    float center =
                            (float) FastMath.cosTable(charge * FastMath.PI);
                    matrix.rotate(center * 30.0f, 0.0f, 1.0f, 0.0f);
                }
            }
            if (wieldMode == WieldMode.LEFT) {
                matrix.rotate(-20.0f, 0, 1, 0);
            } else if (wieldMode == WieldMode.RIGHT) {
                matrix.rotate(-50.0f, 0, 1, 0);
                matrix.rotate(-2.0f, 1, 0, 0);
            }
        }
        rot = FastMath.min(rot * 2, 1);
        armLeft.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        if (wieldMode != WieldMode.RIGHT) {
            matrix.translate(-0.3f, 0.4f, -0.4f);
            if (item.material().isWeapon(item)) {
                matrix.translate(0.0f, -0.6f, -0.05f);
                matrix.rotate(rot * -60.0f, 1.0f, 0.0f, 0.0f);
                matrix.rotate(rot * -50.0f, 0.0f, 1.0f, 0.0f);
                matrix.translate(0.0f, 0.6f, 0.0f);
            } else if (!item.material().isTool(item)) {
                matrix.translate(0.31f, -0.3f, -0.2f);
                matrix.scale(0.3f, 0.3f, 0.3f);
            }
            matrix.rotate(120.0f, 0.0f, 0.0f, 1.0f);
            matrix.rotate(60.0f, 0.0f, 1.0f, 0.0f);
            if (!culling) {
                gl.enableCulling();
            }
            item.material()
                    .render(item, gl, shader, 1.0f, damageColor, damageColor,
                            1.0f);
            if (!culling) {
                gl.disableCulling();
            }
        }
        matrixStack.pop();
        matrix = matrixStack.push();
        matrix.translate(0.25f, 0, 0.25f);
        if (wieldMode == WieldMode.LEFT) {
            item = entity.leftWeapon();
            charge = armDirLeftRender;
            charge2 = armDirLeft2 * 0.4f;
        } else {
            item = entity.rightWeapon();
            charge = armDirRightRender;
            charge2 = armDirRight2;
        }
        if (item.material() == world.air()) {
            matrix.rotate((float) lazyNameDir * -60, 0, 1, 0);
            matrix.rotate((float) swingDir * 60, 1, 0, 0);
            rot = (float) FastMath.sinTable(charge * FastMath.PI);
            matrix.rotate(charge2 * -20, 0, 0, 1);
            matrix.rotate(rot * 50 + charge2 * 90 + pitch * charge2, 1, 0, 0);
        } else {
            if (wieldMode == WieldMode.DUAL) {
                matrix.rotate((float) lazyNameDir * -60, 0, 1, 0);
                matrix.rotate((float) swingDir * 30, 1, 0, 0);
            } else {
                matrix.rotate((float) swingDir * 10, 1, 0, 0);
            }
            if (item.material().isWeapon(item)) {
                rot = (float) FastMath.sinTable(-charge * FastMath.PI);
                if (rot < 0.0f && charge > 0.0) {
                    rot /= 4.0f;
                }
                if (wieldMode == WieldMode.LEFT) {
                    rot = -rot;
                }
                matrix.rotate(rot * 60.0f, 0.0f, 0.0f, 1.0f);
                matrix.rotate(rot * 60.0f, 0.0f, 1.0f, 0.0f);
                matrix.rotate(rot * 10.0f + 40.0f + charge2 * 45.0f +
                        pitch * charge2, 1.0f, 0.0f, 0.0f);
            } else {
                rot = (float) FastMath.sinTable(charge * FastMath.PI);
                matrix.rotate(charge2 * -20, 0, 0, 1);
                matrix.rotate(rot * 50 + 40 + charge2 * 45 +
                        pitch * charge2, 1, 0, 0);
                if (charge > -1.5f && charge < -0.5f) {
                    float center =
                            (float) FastMath.cosTable(charge * FastMath.PI);
                    matrix.rotate(center * -30.0f, 0.0f, 1.0f, 0.0f);
                }
            }
            if (wieldMode == WieldMode.RIGHT) {
                matrix.rotate(20.0f, 0, 1, 0);
            } else if (wieldMode == WieldMode.LEFT) {
                matrix.rotate(50.0f, 0, 1, 0);
                matrix.rotate(-2.0f, 1, 0, 0);
            }
        }
        rot = FastMath.min(rot * 2, 1);
        texture.bind(gl);
        armRight.render(1.0f, damageColor, damageColor, 1.0f, gl, shader);
        if (wieldMode != WieldMode.LEFT) {
            matrix.translate(0.3f, 0.4f, -0.4f);
            if (item.material().isWeapon(item)) {
                matrix.translate(0.0f, -0.6f, -0.05f);
                matrix.rotate(rot * -60.0f, 1.0f, 0.0f, 0.0f);
                matrix.rotate(rot * 50.0f, 0.0f, 1.0f, 0.0f);
                matrix.translate(0.0f, 0.6f, 0.0f);
            } else if (!item.material().isTool(item)) {
                matrix.translate(-0.31f, -0.3f, -0.2f);
                matrix.scale(0.3f, 0.3f, 0.3f);
            }
            matrix.rotate(60.0f, 0.0f, 0.0f, 1.0f);
            matrix.rotate(60.0f, 0.0f, 1.0f, 0.0f);
            if (!culling) {
                gl.enableCulling();
            }
            item.material()
                    .render(item, gl, shader, 1.0f, damageColor, damageColor,
                            1.0f);
        }
        matrixStack.pop();
        matrixStack.pop();
    }

    @Override
    public float pitch() {
        return pitch;
    }

    @Override
    public float yaw() {
        return yaw;
    }
}