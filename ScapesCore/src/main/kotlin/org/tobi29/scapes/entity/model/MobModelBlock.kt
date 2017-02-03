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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.min
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.minus
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.entity.client.MobClient

class MobModelBlock(private val entity: MobClient,
                    private val item: ItemStack) : MobModel {
    private val pos = MutableVector3d()

    init {
        pos.set(entity.getCurrentPos())
    }

    override fun pitch(): Double {
        return 0.0
    }

    override fun yaw(): Double {
        return 0.0
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
        val factor = min(1.0, delta * 10.0)
        pos.plus(entity.getCurrentPos().minus(pos.now()).times(factor))
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
        val matrixStack = gl.matrixStack()
        val matrix = matrixStack.push()
        matrix.translate(posRenderX, posRenderY, posRenderZ)
        item.material().render(item, gl, shader)
        matrixStack.pop()
    }
}
