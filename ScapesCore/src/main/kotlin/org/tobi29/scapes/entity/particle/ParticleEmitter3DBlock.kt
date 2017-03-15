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

package org.tobi29.scapes.entity.particle

import org.tobi29.scapes.chunk.terrain.block
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.times

class ParticleEmitter3DBlock(system: ParticleSystem) : ParticleEmitter<ParticleInstance3DBlock>(
        system, Array(256, { ParticleInstance3DBlock() })) {
    override fun update(delta: Double) {
        if (!hasAlive) {
            return
        }
        val aabb = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val gravitation = system.world.gravity.toFloat()
        val terrain = system.world.terrain
        var hasAlive = false
        for (instance in instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue
            }
            hasAlive = true
            instance.time -= delta.toFloat()
            if (instance.time <= 0.0) {
                instance.state = ParticleInstance.State.DEAD
                continue
            }
            aabb.minX = instance.pos.doubleX() - SIZE
            aabb.minY = instance.pos.doubleY() - SIZE
            aabb.minZ = instance.pos.doubleZ() - SIZE
            aabb.maxX = instance.pos.doubleX() + SIZE
            aabb.maxY = instance.pos.doubleY() + SIZE
            aabb.maxZ = instance.pos.doubleZ() + SIZE
            if (ParticlePhysics.update(delta, instance, terrain, aabb,
                    gravitation, 1.0f,
                    0.2f, 0.4f, 8.0f)) {
                instance.rotationSpeed.div(
                        1.0 + 0.4 * delta * gravitation.toDouble())
            }
            instance.rotation.plus(instance.rotationSpeed.now().times(delta))
        }
        this.hasAlive = hasAlive
    }

    override fun addToPipeline(gl: GL,
                               width: Int,
                               height: Int,
                               cam: Cam): () -> Unit {
        val shader = gl.engine.graphics.loadShader("Scapes:shader/Entity") {
            supplyPreCompile {
                supplyProperty("SCENE_WIDTH", width)
                supplyProperty("SCENE_HEIGHT", height)
            }
        }
        return render@ {
            if (!hasAlive) {
                return@render
            }
            val world = system.world
            val terrain = world.terrain
            val s = shader.get()
            for (instance in instances) {
                if (instance.state != ParticleInstance.State.ALIVE) {
                    continue
                }
                val x = instance.pos.intX()
                val y = instance.pos.intY()
                val z = instance.pos.intZ()
                if (terrain.block(x, y, z) {
                    !isSolid(it) || isTransparent(it)
                }) {
                    val posRenderX = (instance.pos.doubleX() - cam.position.doubleX()).toFloat()
                    val posRenderY = (instance.pos.doubleY() - cam.position.doubleY()).toFloat()
                    val posRenderZ = (instance.pos.doubleZ() - cam.position.doubleZ()).toFloat()
                    val matrixStack = gl.matrixStack()
                    val matrix = matrixStack.push()
                    matrix.translate(posRenderX, posRenderY, posRenderZ)
                    matrix.rotate(instance.rotation.floatZ(), 0f, 0f, 1f)
                    matrix.rotate(instance.rotation.floatX(), 1f, 0f, 0f)
                    gl.setAttribute2f(4,
                            terrain.blockLight(x, y, z) / 15.0f,
                            terrain.sunLight(x, y, z) / 15.0f)
                    instance.item.material().render(instance.item, gl, s)
                    matrixStack.pop()
                }
            }
        }
    }

    companion object {
        private val SIZE = 0.5f
    }
}
