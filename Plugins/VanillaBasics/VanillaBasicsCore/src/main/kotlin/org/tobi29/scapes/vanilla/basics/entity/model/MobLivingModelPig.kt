/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.vanilla.basics.entity.model

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.entity.client.MobLivingClient
import org.tobi29.scapes.entity.model.Box
import org.tobi29.scapes.entity.model.MobLivingModel

class MobLivingModelPig(shared: MobLivingModelPigShared,
                        private val entity: MobLivingClient,
                        private val texture: Resource<Texture>) : MobLivingModel {
    private val pos: MutableVector3d
    private val body: Box
    private val head: Box
    private val legFrontLeft: Box
    private val legFrontRight: Box
    private val legBackLeft: Box
    private val legBackRight: Box
    private var swing = 0.0
    private var moveSpeedRender = 0.0
    private var pitch = 0.0
    private var yaw = 0.0

    init {
        pos = MutableVector3d(entity.getCurrentPos())
        body = shared.body
        head = shared.head
        legFrontLeft = shared.legFrontLeft
        legFrontRight = shared.legFrontRight
        legBackLeft = shared.legBackLeft
        legBackRight = shared.legBackRight
    }

    override fun pos(): Vector3d {
        return pos.now()
    }

    override fun shapeAABB(aabb: AABB) {
        aabb.minX = pos.doubleX() - 0.75
        aabb.minY = pos.doubleY() - 0.75
        aabb.minZ = pos.doubleZ() - 0.45
        aabb.maxX = pos.doubleX() + 0.75
        aabb.maxY = pos.doubleY() + 0.75
        aabb.maxZ = pos.doubleZ() + 0.7
    }

    override fun renderUpdate(delta: Double) {
        val factorPos = min(1.0, delta * 20.0)
        val factorRot = min(1.0, delta * 40.0)
        val factorSpeed = min(1.0, delta * 5.0)
        val speed = entity.speed()
        val moveSpeed = min(sqrt(length(speed.x, speed.y)), 2.0)
        pitch -= angleDiff(entity.pitch(), pitch) * factorRot
        yaw -= angleDiff(entity.yaw(), yaw) * factorRot
        pos.plus(entity.getCurrentPos().minus(pos.now()).times(factorPos))
        swing += moveSpeed * 2.0 * delta
        swing %= TWO_PI
        moveSpeedRender += (moveSpeed - moveSpeedRender) * factorSpeed
    }

    override fun render(gl: GL,
                        world: WorldClient,
                        cam: Cam,
                        shader: Shader) {
        val damageColor = (1.0 - min(1.0,
                max(0.0, entity.invincibleTicks() / 0.8))).toFloat()
        val posRenderX = (pos.doubleX() - cam.position.doubleX()).toFloat()
        val posRenderY = (pos.doubleY() - cam.position.doubleY()).toFloat()
        val posRenderZ = (pos.doubleZ() - cam.position.doubleZ()).toFloat()
        val swingDir = cosTable(swing) * moveSpeedRender * 0.5
        gl.setAttribute2f(4,
                world.terrain.blockLight(pos.intX(), pos.intY(),
                        pos.intZ()) / 15.0f,
                world.terrain.sunLight(pos.intX(), pos.intY(),
                        pos.intZ()) / 15.0f)
        texture.get().bind(gl)
        val matrixStack = gl.matrixStack()
        var matrix = matrixStack.push()
        matrix.translate(posRenderX, posRenderY, posRenderZ)
        matrix.rotate(yaw - 90, 0f, 0f, 1f)
        body.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrix = matrixStack.push()
        matrix.translate(0f, 0.3125f, 0.0625f)
        matrix.rotate(pitch, 1f, 0f, 0f)
        head.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrixStack.pop()
        matrix = matrixStack.push()
        matrix.translate(-0.125f, 0.1875f, -0.3125f)
        matrix.rotate(swingDir.toFloat() * 30, 1f, 0f, 0f)
        legFrontLeft.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrixStack.pop()
        matrix = matrixStack.push()
        matrix.translate(0.125f, 0.1875f, -0.3125f)
        matrix.rotate((-swingDir).toFloat() * 30, 1f, 0f, 0f)
        legFrontRight.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrixStack.pop()
        matrix = matrixStack.push()
        matrix.translate(-0.125f, -0.1875f, -0.3125f)
        matrix.rotate((-swingDir).toFloat() * 30, 1f, 0f, 0f)
        legBackLeft.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrixStack.pop()
        matrix = matrixStack.push()
        matrix.translate(0.125f, -0.1875f, -0.3125f)
        matrix.rotate(swingDir.toFloat() * 30, 1f, 0f, 0f)
        legBackRight.render(1.0f, damageColor, damageColor, 1.0f, gl, shader)
        matrixStack.pop()
        matrixStack.pop()
    }

    override fun pitch(): Double {
        return pitch
    }

    override fun yaw(): Double {
        return yaw
    }
}
