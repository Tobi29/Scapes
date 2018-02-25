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
import org.tobi29.math.AABB
import org.tobi29.math.Face
import org.tobi29.math.PointerPane
import org.tobi29.math.vector.MutableVector3d
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.minus
import org.tobi29.math.vector.times
import org.tobi29.utils.Pool
import org.tobi29.graphics.Cam
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.floorToInt
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.entity.model.EntityModelBlockBreakShared
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBlockBreakClient
import kotlin.math.min

class EntityModelBlockBreak(shared: EntityModelBlockBreakShared,
                            override val entity: EntityBlockBreakClient) : EntityModel {
    private val pos = MutableVector3d(entity.getCurrentPos())
    private val pointerPanes = Pool { PointerPane() }
    private val model = shared.model
    private val textures = shared.textures

    override fun pos() = pos.now()

    override fun setPos(pos: Vector3d) {
        this.pos.set(pos)
    }

    override fun shapeAABB(aabb: AABB) {
        aabb.minX = pos.x - 0.5
        aabb.minY = pos.y - 0.5
        aabb.minZ = pos.z - 0.5
        aabb.maxX = pos.x + 0.5
        aabb.maxY = pos.y + 0.5
        aabb.maxZ = pos.z + 0.5
    }

    override fun renderUpdate(delta: Double) {
        val factor = min(1.0, delta * 5.0)
        pos.add((entity.getCurrentPos() - pos.now()) * factor)
        pointerPanes.reset()
        val terrain = entity.world.terrain
        val block = terrain.block(pos.x.floorToInt(), pos.y.floorToInt(), pos.z.floorToInt())
        terrain.type(block).addPointerCollision(terrain.data(block),
                pointerPanes, pos.x.floorToInt(), pos.y.floorToInt(), pos.z.floorToInt())
    }

    override fun render(gl: GL,
                        world: WorldClient,
                        cam: Cam,
                        shader: Shader) {
        val posRenderX = (pos.x - cam.position.x).toFloat()
        val posRenderY = (pos.y - cam.position.y).toFloat()
        val posRenderZ = (pos.z - cam.position.z).toFloat()
        val i = clamp((entity.progress() * 10).floorToInt() + 1, 1, 9)
        if (i < 1 || i > 10) {
            return
        }
        gl.setAttribute2f(4,
                world.terrain.blockLight(pos.x.floorToInt(), pos.y.floorToInt(),
                        pos.z.floorToInt()) / 15.0f,
                world.terrain.sunLight(pos.x.floorToInt(), pos.y.floorToInt(),
                        pos.z.floorToInt()) / 15.0f)
        textures[i - 1].bind(gl)
        for (pane in pointerPanes) {
            gl.matrixStack.push { matrix ->
                matrix.translate(
                        (posRenderX - 0.5 + (pane.aabb.minX + pane.aabb.maxX) / 2).toFloat(),
                        (posRenderY - 0.5 + (pane.aabb.minY + pane.aabb.maxY) / 2).toFloat(),
                        (posRenderZ - 0.5 + (pane.aabb.minZ + pane.aabb.maxZ) / 2).toFloat())
                matrix.scale(
                        (pane.aabb.maxX - pane.aabb.minX).toFloat() + 0.01f,
                        (pane.aabb.maxY - pane.aabb.minY).toFloat() + 0.01f,
                        (pane.aabb.maxZ - pane.aabb.minZ).toFloat() + 0.01f)
                when (pane.face) {
                    Face.DOWN -> matrix.rotate(180f, 1f, 0f, 0f)
                    Face.NORTH -> matrix.rotate(90f, 1f, 0f, 0f)
                    Face.EAST -> matrix.rotate(90f, 0f, 1f, 0f)
                    Face.SOUTH -> matrix.rotate(270f, 1f, 0f, 0f)
                    Face.WEST -> matrix.rotate(270f, 0f, 1f, 0f)
                }
                gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 0.3f, 0.3f, 0.3f, 0.4f)
                model.render(gl, shader)
            }
        }
    }
}
