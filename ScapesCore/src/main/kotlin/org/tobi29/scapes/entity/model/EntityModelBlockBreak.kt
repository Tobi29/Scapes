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

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Model
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.minus
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.entity.client.EntityBlockBreakClient

class EntityModelBlockBreak(shared: EntityModelBlockBreakShared,
                            private val entity: EntityBlockBreakClient) : EntityModel {
    private val pos: MutableVector3d
    private val pointerPanes = Pool { PointerPane() }
    private val model: Model
    private val texture = Array(9) {
        entity.world.game.engine.graphics.textures["Scapes:image/entity/Break${it + 1}"]
    }

    init {
        pos = MutableVector3d(entity.getCurrentPos())
        model = shared.model
    }

    override fun pos(): Vector3d {
        return pos.now()
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
        val factor = min(1.0, delta * 5.0)
        pos.plus(entity.getCurrentPos().minus(pos.now()).times(factor))
        pointerPanes.reset()
        val terrain = entity.world.terrain
        val block = terrain.block(pos.intX(), pos.intY(), pos.intZ())
        terrain.type(block).addPointerCollision(terrain.data(block),
                pointerPanes, pos.intX(), pos.intY(), pos.intZ())
    }

    override fun render(gl: GL,
                        world: WorldClient,
                        cam: Cam,
                        shader: Shader) {
        val posRenderX = (pos.doubleX() - cam.position.doubleX()).toFloat()
        val posRenderY = (pos.doubleY() - cam.position.doubleY()).toFloat()
        val posRenderZ = (pos.doubleZ() - cam.position.doubleZ()).toFloat()
        val i = clamp(floor(entity.progress() * 10) + 1, 1, 9)
        if (i < 1 || i > 10) {
            return
        }
        gl.setAttribute2f(4,
                world.terrain.blockLight(pos.intX(), pos.intY(),
                        pos.intZ()) / 15.0f,
                world.terrain.sunLight(pos.intX(), pos.intY(),
                        pos.intZ()) / 15.0f)
        texture[i - 1].get().bind(gl)
        for (pane in pointerPanes) {
            val matrixStack = gl.matrixStack()
            val matrix = matrixStack.push()
            matrix.translate(
                    (posRenderX - 0.5 + (pane.aabb.minX + pane.aabb.maxX) / 2).toFloat(),
                    (posRenderY - 0.5 + (pane.aabb.minY + pane.aabb.maxY) / 2).toFloat(),
                    (posRenderZ - 0.5 + (pane.aabb.minZ + pane.aabb.maxZ) / 2).toFloat())
            matrix.scale((pane.aabb.maxX - pane.aabb.minX).toFloat() + 0.01f,
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
            matrixStack.pop()
        }
    }
}
