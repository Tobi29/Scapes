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

package org.tobi29.scapes.entity.model

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.client.MobLivingEquippedClient
import org.tobi29.scapes.entity.particle.ParticleEmitterFallenBodyPart
import org.tobi29.scapes.entity.particle.ParticleSystem
import java.util.concurrent.ThreadLocalRandom

class MobLivingModelHuman(shared: MobLivingModelHumanShared,
                          private val entity: MobLivingEquippedClient,
                          private val texture: Texture, thin: Boolean = false,
                          culling: Boolean = true,
                          private val precise: Boolean = false) : MobLivingModel {
    private val pos: MutableVector3d
    private val body: Box
    private val head: Box
    private val legLeft: Box
    private val legRight: Box
    private val armLeft: Box
    private val armRight: Box
    private var swing = 0.0
    private var lazyName = 0.0
    private var moveSpeedRender = 0.0
    private var armDirLeft = 0.0
    private var armDirRight = 0.0
    private var armDirLeftRender = 0.0
    private var armDirRightRender = 0.0
    private var armDirLeft2 = 0.0
    private var armDirRight2 = 0.0
    private var pitch = 0.0
    private var yaw = 0.0

    init {
        pos = MutableVector3d(entity.getCurrentPos())
        if (culling) {
            body = shared.body
            head = shared.head
            if (thin) {
                legLeft = shared.legThinLeft
                legRight = shared.legThinRight
                armLeft = shared.armThinLeft
                armRight = shared.armThinRight
            } else {
                legLeft = shared.legNormalLeft
                legRight = shared.legNormalRight
                armLeft = shared.armNormalLeft
                armRight = shared.armNormalRight
            }
        } else {
            body = shared.bodyNoCull
            head = shared.headNoCull
            if (thin) {
                legLeft = shared.legThinLeftNoCull
                legRight = shared.legThinRightNoCull
                armLeft = shared.armThinLeftNoCull
                armRight = shared.armThinRightNoCull
            } else {
                legLeft = shared.legNormalLeftNoCull
                legRight = shared.legNormalRightNoCull
                armLeft = shared.armNormalLeftNoCull
                armRight = shared.armNormalRightNoCull
            }
        }
    }

    override fun pos(): Vector3d {
        return pos.now()
    }

    override fun shapeAABB(aabb: AABB) {
        aabb.minX = pos.doubleX() - 1.0
        aabb.minY = pos.doubleY() - 1.0
        aabb.minZ = pos.doubleZ() - 1.0
        aabb.maxX = pos.doubleX() + 1.0
        aabb.maxY = pos.doubleY() + 1.0
        aabb.maxZ = pos.doubleZ() + 1.2
    }

    override fun renderUpdate(delta: Double) {
        val factorPos = min(1.0, delta * 20.0)
        val factorRot: Double
        if (precise) {
            factorRot = 0.9
        } else {
            factorRot = min(1.0, delta * 40.0)
        }
        val factorSpeed = min(1.0, delta * 5.0)
        val speed = entity.speed()
        val moveSpeed = min(sqrt(length(speed.x, speed.y)), 2.0)
        pitch -= (angleDiff(entity.pitch(),
                pitch.toDouble()) * factorRot).toFloat()
        yaw -= (angleDiff(entity.yaw(), yaw.toDouble()) * factorRot).toFloat()
        pos.plus(entity.getCurrentPos().minus(pos.now()).times(factorPos))
        swing += moveSpeed * 2.0 * delta
        swing %= FastMath.TWO_PI
        lazyName += delta
        lazyName %= FastMath.TWO_PI
        moveSpeedRender += (moveSpeed - moveSpeedRender) * factorSpeed
        val newChargeLeft = entity.leftCharge().toDouble()
        val weaponLeft = entity.leftWeapon()
        if (newChargeLeft > 0.01) {
            armDirLeft2 += (2.4 * delta).toFloat()
            armDirLeft2 -= armDirLeft2.toDouble() * 2.7 * delta.toFloat()
        } else {
            if (armDirLeft > 0.01f) {
                if (armDirLeft >= 0.45f) {
                    if (weaponLeft.material().isWeapon(weaponLeft)) {
                        armDirLeftRender = -1.1
                    } else {
                        armDirLeftRender = -1.5
                    }
                }
            }
            if (armDirLeft < 0.01f) {
                armDirLeft2 -= armDirLeft2.toDouble() * 2.7 * delta.toFloat()
            }
        }
        armDirLeft = newChargeLeft
        if (weaponLeft.material().isWeapon(
                weaponLeft) && armDirLeftRender < -0.6) {
            armDirLeftRender += (armDirLeft - armDirLeftRender) * factorPos / 12.0
        } else {
            armDirLeftRender += (armDirLeft - armDirLeftRender) * factorPos
        }
        val newChargeRight = entity.rightCharge().toDouble()
        val weaponRight = entity.rightWeapon()
        if (newChargeRight > 0.01) {
            armDirRight2 += 2.4 * delta
            armDirRight2 -= armDirRight2.toDouble() * 2.7 * delta
        } else {
            if (armDirRight > 0.01) {
                if (armDirRight >= 0.45) {
                    if (weaponRight.material().isWeapon(weaponRight)) {
                        armDirRightRender = -1.1
                    } else {
                        armDirRightRender = -1.5
                    }
                }
            }
            if (armDirRight < 0.01f) {
                armDirRight2 -= armDirRight2.toDouble() * 2.7 * delta
            }
        }
        armDirRight = newChargeRight
        if (weaponRight.material().isWeapon(
                weaponRight) && armDirRightRender < -0.6) {
            armDirRightRender += ((armDirRight - armDirRightRender) * factorPos / 12.0)
        } else {
            armDirRightRender += ((armDirRight - armDirRightRender) * factorPos)
        }
    }

    override fun render(gl: GL,
                        world: WorldClient,
                        cam: Cam,
                        shader: Shader) {
        val damageColor = (1.0 - min(1.0,
                max(0.0, entity.invincibleTicks() / 0.8))).toFloat()
        var posRenderX = (pos.doubleX() - cam.position.doubleX()).toFloat()
        var posRenderY = (pos.doubleY() - cam.position.doubleY()).toFloat()
        val posRenderZ = (pos.doubleZ() - cam.position.doubleZ()).toFloat()
        val l = pitch * 0.004
        if (l < 0.0) {
            val d = yaw.toRad().toDouble()
            posRenderX += (cosTable(d) * l).toFloat()
            posRenderY += (sinTable(d) * l).toFloat()
        }
        val swingDir = cosTable(swing) * moveSpeedRender * 0.5
        val lazyNameDir = (cosTable(
                lazyName) * 0.5 + 0.5) * (moveSpeedRender * 0.2 + 0.2)
        gl.setAttribute2f(4, world.terrain.blockLight(pos.intX(), pos.intY(),
                pos.intZ()) / 15.0f,
                world.terrain.sunLight(pos.intX(), pos.intY(),
                        pos.intZ()) / 15.0f)
        texture.bind(gl)
        val matrixStack = gl.matrixStack()
        var matrix = matrixStack.push()
        matrix.translate(posRenderX, posRenderY, posRenderZ)
        matrix.rotate(yaw - 90.0, 0.0f, 0.0f, 1.0f)
        body.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrix = matrixStack.push()
        matrix.translate(0f, 0f, 0.375f)
        matrix.rotate(pitch, 1f, 0f, 0f)
        head.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrixStack.pop()
        matrix = matrixStack.push()
        matrix.translate(-0.125f, 0f, -0.5f)
        matrix.rotate((swingDir * 30.0).toFloat(), 1f, 0f, 0f)
        legLeft.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrixStack.pop()
        matrix = matrixStack.push()
        matrix.translate(0.125f, 0f, -0.5f)
        matrix.rotate((-swingDir * 30.0).toFloat(), 1f, 0f, 0f)
        legRight.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrixStack.pop()
        val wieldMode = entity.wieldMode()
        matrix = matrixStack.push()
        matrix.translate(-0.25f, 0f, 0.25f)
        var rot: Double
        var charge: Double
        var charge2: Double
        var item: ItemStack
        if (wieldMode === WieldMode.RIGHT) {
            item = entity.rightWeapon()
            charge = armDirRightRender.toDouble()
            charge2 = armDirRight2 * 0.4
        } else {
            item = entity.leftWeapon()
            charge = armDirLeftRender.toDouble()
            charge2 = armDirLeft2.toDouble()
        }
        if (item.material() === world.air) {
            matrix.rotate((lazyNameDir * 60.0).toFloat(), 0f, 1f, 0f)
            matrix.rotate((-swingDir * 60.0).toFloat(), 1f, 0f, 0f)
            rot = sinTable(charge * FastMath.PI)
            matrix.rotate((charge2 * -20.0).toFloat(), 0f, 0f, 1f)
            matrix.rotate(
                    (rot * 50.0 + charge2 * 90.0 + pitch * charge2).toFloat(),
                    1f,
                    0f, 0f)
        } else {
            if (wieldMode === WieldMode.DUAL) {
                matrix.rotate(lazyNameDir.toFloat() * 60, 0f, 1f, 0f)
                matrix.rotate((-swingDir).toFloat() * 30, 1f, 0f, 0f)
            } else {
                matrix.rotate(swingDir.toFloat() * 10, 1f, 0f, 0f)
            }
            if (item.material().isWeapon(item)) {
                rot = sinTable(-charge * FastMath.PI)
                if (rot < 0 && charge > 0.0) {
                    rot /= 4.0
                }
                if (wieldMode === WieldMode.RIGHT) {
                    rot = -rot
                }
                matrix.rotate((rot * -60.0).toFloat(), 0.0f, 0.0f, 1.0f)
                matrix.rotate((rot * -60.0).toFloat(), 0.0f, 1.0f, 0.0f)
                matrix.rotate((rot * 10.0 + 40.0 + charge2 * 45.0 +
                        pitch * charge2).toFloat(), 1.0f, 0.0f, 0.0f)
            } else {
                rot = sinTable(charge * FastMath.PI)
                matrix.rotate((charge2 * -20.0).toFloat(), 0f, 0f, 1f)
                matrix.rotate((rot * 50 + 40.0 + charge2 * 45 +
                        pitch * charge2).toFloat(), 1f, 0f, 0f)
                if (charge > -1.5f && charge < -0.5f) {
                    val center = cosTable(charge * FastMath.PI).toFloat()
                    matrix.rotate(center * 30.0f, 0.0f, 1.0f, 0.0f)
                }
            }
            if (wieldMode === WieldMode.LEFT) {
                matrix.rotate(-20.0f, 0f, 1f, 0f)
            } else if (wieldMode === WieldMode.RIGHT) {
                matrix.rotate(-50.0f, 0f, 1f, 0f)
                matrix.rotate(-2.0f, 1f, 0f, 0f)
            }
        }
        rot = min(rot * 2, 1.0)
        armLeft.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        if (wieldMode !== WieldMode.RIGHT) {
            matrix.translate(-0.3f, 0.4f, -0.4f)
            if (item.material().isWeapon(item)) {
                matrix.translate(0.0f, -0.6f, -0.05f)
                matrix.rotate((rot * -60.0).toFloat(), 1.0f, 0.0f, 0.0f)
                matrix.rotate((rot * -50.0).toFloat(), 0.0f, 1.0f, 0.0f)
                matrix.translate(0.0f, 0.6f, 0.0f)
            } else if (!item.material().isTool(item)) {
                matrix.translate(0.31f, -0.3f, -0.2f)
                matrix.scale(0.3f, 0.3f, 0.3f)
            }
            matrix.rotate(120.0f, 0.0f, 0.0f, 1.0f)
            matrix.rotate(60.0f, 0.0f, 1.0f, 0.0f)
            item.material().render(item, gl, shader)
        }
        matrixStack.pop()
        matrix = matrixStack.push()
        matrix.translate(0.25f, 0f, 0.25f)
        if (wieldMode === WieldMode.LEFT) {
            item = entity.leftWeapon()
            charge = armDirLeftRender.toDouble()
            charge2 = armDirLeft2 * 0.4
        } else {
            item = entity.rightWeapon()
            charge = armDirRightRender.toDouble()
            charge2 = armDirRight2.toDouble()
        }
        if (item.material() === world.air) {
            matrix.rotate((lazyNameDir * -60.0).toFloat(), 0f, 1f, 0f)
            matrix.rotate((swingDir * 60.0).toFloat(), 1f, 0f, 0f)
            rot = sinTable(charge * FastMath.PI)
            matrix.rotate((charge2 * -20.0).toFloat(), 0f, 0f, 1f)
            matrix.rotate(
                    (rot * 50.0 + charge2 * 90.0 + pitch * charge2).toFloat(),
                    1f,
                    0f, 0f)
        } else {
            if (wieldMode === WieldMode.DUAL) {
                matrix.rotate(lazyNameDir.toFloat() * -60, 0f, 1f, 0f)
                matrix.rotate(swingDir.toFloat() * 30, 1f, 0f, 0f)
            } else {
                matrix.rotate(swingDir.toFloat() * 10, 1f, 0f, 0f)
            }
            if (item.material().isWeapon(item)) {
                rot = sinTable(-charge * FastMath.PI).toFloat().toDouble()
                if (rot < 0.0f && charge > 0.0) {
                    rot /= 4.0
                }
                if (wieldMode === WieldMode.LEFT) {
                    rot = -rot
                }
                matrix.rotate((rot * 60.0).toFloat(), 0.0f, 0.0f, 1.0f)
                matrix.rotate((rot * 60.0).toFloat(), 0.0f, 1.0f, 0.0f)
                matrix.rotate((rot * 10.0 + 40.0 + charge2 * 45.0 +
                        pitch * charge2).toFloat(), 1.0f, 0.0f, 0.0f)
            } else {
                rot = sinTable(charge * FastMath.PI).toFloat().toDouble()
                matrix.rotate((charge2 * -20).toFloat(), 0f, 0f, 1f)
                matrix.rotate((rot * 50 + 40.0 + charge2 * 45 +
                        pitch * charge2).toFloat(), 1f, 0f, 0f)
                if (charge > -1.5f && charge < -0.5f) {
                    val center = cosTable(charge * FastMath.PI).toFloat()
                    matrix.rotate(center * -30.0f, 0.0f, 1.0f, 0.0f)
                }
            }
            if (wieldMode === WieldMode.RIGHT) {
                matrix.rotate(20.0f, 0f, 1f, 0f)
            } else if (wieldMode === WieldMode.LEFT) {
                matrix.rotate(50.0f, 0f, 1f, 0f)
                matrix.rotate(-2.0f, 1f, 0f, 0f)
            }
        }
        rot = min(rot * 2, 1.0)
        texture.bind(gl)
        armRight.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        if (wieldMode !== WieldMode.LEFT) {
            matrix.translate(0.3f, 0.4f, -0.4f)
            if (item.material().isWeapon(item)) {
                matrix.translate(0.0f, -0.6f, -0.05f)
                matrix.rotate((rot * -60.0).toFloat(), 1.0f, 0.0f, 0.0f)
                matrix.rotate((rot * 50.0).toFloat(), 0.0f, 1.0f, 0.0f)
                matrix.translate(0.0f, 0.6f, 0.0f)
            } else if (!item.material().isTool(item)) {
                matrix.translate(-0.31f, -0.3f, -0.2f)
                matrix.scale(0.3f, 0.3f, 0.3f)
            }
            matrix.rotate(60.0f, 0.0f, 0.0f, 1.0f)
            matrix.rotate(60.0f, 0.0f, 1.0f, 0.0f)
            item.material().render(item, gl, shader)
        }
        matrixStack.pop()
        matrixStack.pop()
    }

    override fun pitch(): Double {
        return pitch
    }

    override fun yaw(): Double {
        return yaw
    }

    companion object {

        fun particles(shared: MobLivingModelHumanShared,
                      particles: ParticleSystem,
                      pos: Vector3d,
                      speed: Vector3d,
                      rot: Vector3d,
                      texture: Texture,
                      thin: Boolean = false,
                      culling: Boolean = true) {
            val emitter = particles.emitter(
                    ParticleEmitterFallenBodyPart::class.java)
            val body: Box
            val head: Box
            val legLeft: Box
            val legRight: Box
            val armLeft: Box
            val armRight: Box
            if (culling) {
                body = shared.body
                head = shared.head
                if (thin) {
                    legLeft = shared.legThinLeft
                    legRight = shared.legThinRight
                    armLeft = shared.armThinLeft
                    armRight = shared.armThinRight
                } else {
                    legLeft = shared.legNormalLeft
                    legRight = shared.legNormalRight
                    armLeft = shared.armNormalLeft
                    armRight = shared.armNormalRight
                }
            } else {
                body = shared.bodyNoCull
                head = shared.headNoCull
                if (thin) {
                    legLeft = shared.legThinLeftNoCull
                    legRight = shared.legThinRightNoCull
                    armLeft = shared.armThinLeftNoCull
                    armRight = shared.armThinRightNoCull
                } else {
                    legLeft = shared.legNormalLeftNoCull
                    legRight = shared.legNormalRightNoCull
                    armLeft = shared.armNormalLeftNoCull
                    armRight = shared.armNormalRightNoCull
                }
            }
            val rotRender = rot.plus(Vector3d(0.0, 0.0, -90.0))
            val z = rotRender.z.toRad()
            val random = ThreadLocalRandom.current()
            emitter.add { instance ->
                instance.pos.set(pos.plus(Vector3d(0.0, 0.0, 0.125)))
                instance.speed.set(
                        speed.plus(Vector3d(random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat().toDouble())))
                instance.box = body
                instance.texture = texture
                instance.rotation.set(rotRender)
                instance.rotationSpeed.set(
                        Vector3d(random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5).normalizeSafe().times(
                                480.0))
                instance.time = 6.0f
            }
            emitter.add { instance ->
                instance.pos.set(pos.plus(Vector3d(0.0, 0.0, 0.7)))
                instance.speed.set(
                        speed.plus(Vector3d(random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat().toDouble())))
                instance.box = head
                instance.texture = texture
                instance.rotation.set(rotRender)
                instance.rotationSpeed.set(
                        Vector3d(random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5).normalizeSafe().times(
                                480.0))
                instance.time = 6.0f
            }
            emitter.add { instance ->
                instance.pos.set(pos.plus(
                        Vector3d(cosTable(z) * -0.375,
                                sinTable(z) * -0.375, 0.375)))
                instance.speed.set(
                        speed.plus(Vector3d(random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat().toDouble())))
                instance.box = armLeft
                instance.texture = texture
                instance.rotation.set(rotRender)
                instance.rotationSpeed.set(
                        Vector3d(random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5).normalizeSafe().times(
                                480.0))
                instance.time = 6.0f
            }
            emitter.add { instance ->
                instance.pos.set(pos.plus(
                        Vector3d(cosTable(z) * 0.375,
                                sinTable(z) * 0.375, 0.375)))
                instance.speed.set(
                        speed.plus(Vector3d(random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat().toDouble())))
                instance.box = armRight
                instance.texture = texture
                instance.rotation.set(rotRender)
                instance.rotationSpeed.set(
                        Vector3d(random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5).normalizeSafe().times(
                                480.0))
                instance.time = 6.0f
            }
            emitter.add { instance ->
                instance.pos.set(pos.plus(
                        Vector3d(cosTable(z) * 0.125,
                                sinTable(z) * 0.125, -0.375)))
                instance.speed.set(
                        speed.plus(Vector3d(random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat().toDouble())))
                instance.box = legLeft
                instance.texture = texture
                instance.rotation.set(rotRender)
                instance.rotationSpeed.set(
                        Vector3d(random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5).normalizeSafe().times(
                                480.0))
                instance.time = 6.0f
            }
            emitter.add { instance ->
                instance.pos.set(pos.plus(
                        Vector3d(cosTable(z) * -0.125,
                                sinTable(z) * -0.125, -0.375)))
                instance.speed.set(
                        speed.plus(Vector3d(random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat() * 0.4 - 0.2,
                                random.nextFloat().toDouble())))
                instance.box = legRight
                instance.texture = texture
                instance.rotation.set(rotRender)
                instance.rotationSpeed.set(
                        Vector3d(random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5).normalizeSafe().times(
                                480.0))
                instance.time = 6.0f
            }
        }
    }
}
