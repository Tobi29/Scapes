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

package org.tobi29.scapes.vanilla.basics.entity.particle

import org.tobi29.scapes.chunk.terrain.block
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.HALF_PI
import org.tobi29.scapes.engine.utils.math.atan2Fast
import org.tobi29.scapes.engine.utils.math.matrix.Matrix4f
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.length
import org.tobi29.scapes.entity.particle.ParticleEmitterInstanced
import org.tobi29.scapes.entity.particle.ParticleInstance
import org.tobi29.scapes.entity.particle.ParticlePhysics
import org.tobi29.scapes.entity.particle.ParticleSystem
import java.util.*

class ParticleEmitterSnow(system: ParticleSystem,
                          texture: Texture) : ParticleEmitterInstanced<ParticleInstanceSnow>(
        system, texture, ParticleEmitterSnow.createAttributes(), 6,
        ParticleEmitterSnow.createAttributesStream(), RenderType.TRIANGLES,
        Array(4096, { ParticleInstanceSnow() })) {
    private val matrix = Matrix4f()

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
            ParticlePhysics.update(delta, instance, terrain, aabb, gravitation,
                    1.0f,
                    0.9f, 0.4f, 8.0f)
        }
        this.hasAlive = hasAlive
    }

    override fun prepareShader(gl: GL,
                               width: Int,
                               height: Int,
                               cam: Cam): ((Shader) -> Unit) -> Unit {
        val shader = gl.engine.graphics.loadShader(
                "VanillaBasics:shader/ParticleSnow") {
            supplyPreCompile {
                supplyProperty("SCENE_WIDTH", width)
                supplyProperty("SCENE_HEIGHT", height)
            }
        }
        val world = system.world
        val scene = world.scene
        val player = world.player
        val environment = world.environment
        return { render ->
            val sunLightReduction = environment.sunLightReduction(
                    cam.position.doubleX(),
                    cam.position.doubleY()) / 15.0f
            val playerLight = max(
                    player.leftWeapon().material().playerLight(
                            player.leftWeapon()),
                    player.rightWeapon().material().playerLight(
                            player.rightWeapon()))
            val s = shader.get()
            s.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB())
            s.setUniform1f(5, scene.fogDistance() * scene.renderDistance())
            s.setUniform1i(6, 1)
            s.setUniform1f(7, sunLightReduction)
            s.setUniform1f(8, playerLight)
            render(s)
        }
    }

    override fun prepareBuffer(cam: Cam): Int {
        val world = system.world
        val terrain = world.terrain
        var count = 0
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
                val yaw = atan2Fast(-posRenderY, -posRenderX)
                val pitch = atan2Fast(posRenderZ,
                        length(posRenderX, posRenderY))
                matrix.identity()
                matrix.translate(posRenderX, posRenderY, posRenderZ)
                matrix.rotateRad(yaw + HALF_PI.toFloat(), 0f, 0f, 1f)
                matrix.rotateRad(pitch, 1f, 0f, 0f)
                matrix.rotateRad(yaw + instance.dir, 0f, 1f, 0f)
                buffer.putFloat(terrain.blockLight(x, y, z) / 15.0f)
                buffer.putFloat(terrain.sunLight(x, y, z) / 15.0f)
                matrix.putInto(buffer)
                count++
            }
        }
        return count
    }

    companion object {
        private val EMPTY_FLOAT = floatArrayOf()
        private val SIZE = 0.075f

        private fun createAttributes(): List<ModelAttribute> {
            val attributes = ArrayList<ModelAttribute>()
            attributes.add(ModelAttribute(GL.VERTEX_ATTRIBUTE, 3,
                    floatArrayOf(-SIZE, 0.0f, -SIZE, SIZE, 0.0f, -SIZE, SIZE,
                            0.0f, SIZE, -SIZE, 0.0f, SIZE, -SIZE, 0.0f, -SIZE,
                            SIZE, 0.0f, SIZE), false, 0, VertexType.HALF_FLOAT))
            attributes.add(ModelAttribute(GL.COLOR_ATTRIBUTE, 4,
                    floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
                    true, 0,
                    VertexType.UNSIGNED_BYTE))
            attributes.add(ModelAttribute(GL.TEXTURE_ATTRIBUTE, 2,
                    floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f), false, 0,
                    VertexType.FLOAT))
            return attributes
        }

        private fun createAttributesStream(): List<ModelAttribute> {
            val attributes = ArrayList<ModelAttribute>()
            attributes.add(ModelAttribute(4, 2, EMPTY_FLOAT, false, 1,
                    VertexType.FLOAT))
            attributes.add(ModelAttribute(5, 4, EMPTY_FLOAT, false, 1,
                    VertexType.FLOAT))
            attributes.add(ModelAttribute(6, 4, EMPTY_FLOAT, false, 1,
                    VertexType.FLOAT))
            attributes.add(ModelAttribute(7, 4, EMPTY_FLOAT, false, 1,
                    VertexType.FLOAT))
            attributes.add(ModelAttribute(8, 4, EMPTY_FLOAT, false, 1,
                    VertexType.FLOAT))
            return attributes
        }
    }
}
