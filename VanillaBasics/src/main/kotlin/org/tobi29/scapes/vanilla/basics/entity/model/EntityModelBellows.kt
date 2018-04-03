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

import org.tobi29.graphics.Cam
import org.tobi29.math.AABB3
import org.tobi29.math.Face
import org.tobi29.math.diff
import org.tobi29.math.vector.*
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.push
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBellowsClient
import org.tobi29.stdex.math.floorToInt
import kotlin.math.min

class EntityModelBellows(
    shared: EntityModelBellowsShared,
    override val entity: EntityBellowsClient
) : EntityModel {
    private val pos = MutableVector3d(entity.getCurrentPos())
    private val side = shared.side
    private val middle = shared.middle
    private val pipe = shared.pipe
    private val textureSide = shared.textureSide
    private val textureMiddle = shared.textureMiddle
    private val texturePipe = shared.texturePipe
    private var scale = 0.0

    override fun pos() = pos.now()

    override fun setPos(pos: Vector3d) {
        this.pos.set(pos)
    }

    override fun shapeAABB(aabb: AABB3) {
        aabb.min.x = pos.x - 0.5
        aabb.min.y = pos.y - 0.5
        aabb.min.z = pos.z - 0.5
        aabb.max.x = pos.x + 0.5
        aabb.max.y = pos.y + 0.5
        aabb.max.z = pos.z + 0.5
    }

    override fun renderUpdate(delta: Double) {
        val factor = min(1.0, delta * 10.0)
        pos.add(entity.getCurrentPos().minus(pos.now()).times(factor))
        val value = entity.scale
        scale += diff(
            scale,
            (if (value > 1.0) 2.0 - value else value) * 0.4 + 0.4,
            2.0
        ) * factor
        scale %= 2f
    }

    override fun render(
        gl: GL,
        world: WorldClient,
        cam: Cam,
        shader: Shader
    ) {
        val posRenderX = (pos.x - cam.position.x).toFloat()
        val posRenderY = (pos.y - cam.position.y).toFloat()
        val posRenderZ = (pos.z - cam.position.z).toFloat()
        gl.setAttribute2f(
            4,
            world.terrain.blockLight(
                pos.x.floorToInt(), pos.y.floorToInt(),
                pos.z.floorToInt()
            ) / 15.0f,
            world.terrain.sunLight(
                pos.x.floorToInt(), pos.y.floorToInt(),
                pos.z.floorToInt()
            ) / 15.0f
        )
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
