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

package org.tobi29.scapes.entity.model

import org.tobi29.scapes.block.render
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.push
import org.tobi29.math.AABB
import org.tobi29.math.vector.MutableVector3d
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.minus
import org.tobi29.math.vector.times
import org.tobi29.stdex.atomic.AtomicReference
import org.tobi29.graphics.Cam
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.remP
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.inventory.Item
import kotlin.math.min

class MobModelItem(override val entity: MobClient,
                   private val item: AtomicReference<Item?>) : MobModel {
    private val pos = MutableVector3d()
    private var dir = 0.0

    init {
        pos.set(entity.getCurrentPos())
    }

    override fun pitch(): Double {
        return 0.0
    }

    override fun yaw(): Double {
        return dir
    }

    override fun pos() = pos.now()

    override fun setPos(pos: Vector3d) {
        this.pos.set(pos)
    }

    override fun shapeAABB(aabb: AABB) {
        aabb.minX = pos.x - 0.1
        aabb.minY = pos.y - 0.1
        aabb.minZ = pos.z - 0.1
        aabb.maxX = pos.x + 0.1
        aabb.maxY = pos.y + 0.1
        aabb.maxZ = pos.z + 0.1
    }

    override fun renderUpdate(delta: Double) {
        val factor = min(1.0, delta * 10.0)
        pos.add(entity.getCurrentPos().minus(pos.now()).times(factor))
        dir = (dir + 45.0 * delta) remP 360.0
    }

    override fun render(gl: GL,
                        world: WorldClient,
                        cam: Cam,
                        shader: Shader) {
        val posRenderX = (pos.x - cam.position.x).toFloat()
        val posRenderY = (pos.y - cam.position.y).toFloat()
        val posRenderZ = (pos.z - cam.position.z).toFloat()
        gl.setAttribute2f(4,
                world.terrain.blockLight(pos.x.floorToInt(), pos.y.floorToInt(),
                        pos.z.floorToInt()) / 15.0f,
                world.terrain.sunLight(pos.x.floorToInt(), pos.y.floorToInt(),
                        pos.z.floorToInt()) / 15.0f)
        gl.matrixStack.push { matrix ->
            matrix.translate(posRenderX, posRenderY, posRenderZ)
            matrix.scale(0.4f, 0.4f, 0.4f)
            matrix.rotate(dir, 0.0f, 0.0f, 1.0f)
            item.get().render(gl, shader)
        }
    }
}
