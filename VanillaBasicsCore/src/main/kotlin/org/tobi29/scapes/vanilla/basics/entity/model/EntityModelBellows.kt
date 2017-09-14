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
import org.tobi29.scapes.engine.graphics.push
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.diff
import org.tobi29.scapes.engine.utils.math.min
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.minus
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.entity.model.Box
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBellowsClient

class EntityModelBellows(shared: EntityModelBellowsShared,
                         override val entity: EntityBellowsClient) : EntityModel {
    private val pos: MutableVector3d
    private val side: Box
    private val middle: Box
    private val pipe: Box
    private val textureSide = entity.world.game.engine.graphics.textures.getNow(
            "VanillaBasics:image/terrain/tree/oak/Planks")
    private val textureMiddle = entity.world.game.engine.graphics.textures.getNow(
            "VanillaBasics:image/terrain/tree/birch/Planks")
    private val texturePipe = entity.world.game.engine.graphics.textures.getNow(
            "VanillaBasics:image/terrain/device/Anvil")
    private var scale = 0.0

    init {
        pos = MutableVector3d(entity.getCurrentPos())
        side = shared.side
        middle = shared.middle
        pipe = shared.pipe
    }

    override fun pos() = pos.now()

    override fun setPos(pos: Vector3d) {
        this.pos.set(pos)
    }

    override fun shapeAABB(aabb: AABB) {
        aabb.minX = pos.doubleX() - 0.5
        aabb.minY = pos.doubleY() - 0.5
        aabb.minZ = pos.doubleZ() - 0.5
        aabb.maxX = pos.doubleX() + 0.5
        aabb.maxY = pos.doubleY() + 0.5
        aabb.maxZ = pos.doubleZ() + 0.5
    }

    override fun renderUpdate(delta: Double) {
        val factor = min(1.0, delta * 10.0)
        pos.plus(entity.getCurrentPos().minus(pos.now()).times(factor))
        val value = entity.scale
        scale += diff(scale,
                (if (value > 1.0) 2.0 - value else value) * 0.4 + 0.4,
                2.0) * factor
        scale %= 2f
    }

    override fun render(gl: GL,
                        world: WorldClient,
                        cam: Cam,
                        shader: Shader) {
        val posRenderX = (pos.doubleX() - cam.position.doubleX()).toFloat()
        val posRenderY = (pos.doubleY() - cam.position.doubleY()).toFloat()
        val posRenderZ = (pos.doubleZ() - cam.position.doubleZ()).toFloat()
        gl.setAttribute2f(4,
                world.terrain.blockLight(pos.intX(), pos.intY(),
                        pos.intZ()) / 15.0f,
                world.terrain.sunLight(pos.intX(), pos.intY(),
                        pos.intZ()) / 15.0f)
        gl.matrixStack.push { matrix ->
            matrix.translate(posRenderX, posRenderY, posRenderZ)
            gl.matrixStack.push { matrix ->
                matrix.scale(1.0f, 1.0f, scale.toFloat())
                textureMiddle.bind(gl)
                middle.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader)
            }
            gl.matrixStack.push { matrix ->
                textureSide.bind(gl)
                matrix.translate(0.0f, 0.0f, scale.toFloat() * 0.5f)
                side.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader)
            }
            gl.matrixStack.push { matrix ->
                matrix.translate(0.0f, 0.0f, -scale.toFloat() * 0.5f)
                side.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader)
            }
            side.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader)
            gl.matrixStack.push { matrix ->
                when (entity.face) {
                    Face.DOWN -> matrix.rotate(180f, 1f, 0f, 0f)
                    Face.NORTH -> matrix.rotate(90f, 1f, 0f, 0f)
                    Face.EAST -> matrix.rotate(90f, 0f, 1f, 0f)
                    Face.SOUTH -> matrix.rotate(270f, 1f, 0f, 0f)
                    Face.WEST -> matrix.rotate(270f, 0f, 1f, 0f)
                    Face.UP, Face.NONE -> {
                    }
                }
                texturePipe.bind(gl)
                pipe.render(1.0f, 1.0f, 1.0f, 1.0f, gl, shader)
            }
        }
    }
}
