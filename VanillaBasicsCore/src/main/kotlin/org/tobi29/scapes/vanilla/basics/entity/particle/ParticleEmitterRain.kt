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
import org.tobi29.scapes.client.loadShader
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.math.matrix.Matrix4f
import org.tobi29.scapes.engine.math.vector.times
import org.tobi29.scapes.engine.utils.AtomicInteger
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.shader.IntegerExpression
import org.tobi29.scapes.entity.particle.ParticleEmitterInstanced
import org.tobi29.scapes.entity.particle.ParticleInstance
import org.tobi29.scapes.entity.particle.ParticleSystem
import kotlin.math.max

class ParticleEmitterRain(system: ParticleSystem,
                          texture: Texture) : ParticleEmitterInstanced<ParticleInstance>(
        system, texture, ParticleEmitterRain.createAttributes(), 2,
        ParticleEmitterRain.createAttributesStream(), RenderType.LINES,
        Array(10240, { ParticleInstance() })) {
    private val matrix = Matrix4f()
    private val raindrops = AtomicInteger()

    val andResetRaindrops: Int
        get() = raindrops.getAndSet(0)

    override fun update(delta: Double) {
        if (!hasAlive) {
            return
        }
        val world = system.world
        val terrain = world.terrain
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
            instance.pos.plus(instance.speed.now().times(delta))
            val x = instance.pos.intX()
            val y = instance.pos.intY()
            val z = instance.pos.intZ()
            if (terrain.block(x, y, z) {
                isSolid(it) || !isTransparent(it)
            }) {
                raindrops.incrementAndGet()
                instance.state = ParticleInstance.State.DEAD
            }
        }
        this.hasAlive = hasAlive
    }

    override fun prepareShader(gl: GL,
                               width: Int,
                               height: Int,
                               cam: Cam): suspend () -> ((Shader) -> Unit) -> Unit {
        val shader = system.world.game.engine.graphics.loadShader(
                "VanillaBasics:shader/ParticleRain", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height)
        ))
        val world = system.world
        val scene = world.scene
        val player = world.player
        val environment = world.environment
        return {
            val s = shader.getAsync()
            ;{ render ->
            val sunLightReduction = environment.sunLightReduction(
                    cam.position.doubleX(),
                    cam.position.doubleY()) / 15.0f
            val playerLight = max(
                    player.leftWeapon().material().playerLight(
                            player.leftWeapon()),
                    player.rightWeapon().material().playerLight(
                            player.rightWeapon()))
            s.setUniform3f(gl, 4, scene.fogR(), scene.fogG(), scene.fogB())
            s.setUniform1f(gl, 5, scene.fogDistance() * scene.renderDistance())
            s.setUniform1i(gl, 6, 1)
            s.setUniform1f(gl, 7, sunLightReduction)
            s.setUniform1f(gl, 8, playerLight)
            render(s)
        }
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
                matrix.identity()
                matrix.translate(posRenderX, posRenderY, posRenderZ)
                // TODO: Add camera speed support
                matrix.scale(instance.speed.floatX(), instance.speed.floatY(),
                        instance.speed.floatZ())
                buffer.putFloat(terrain.blockLight(x, y, z) / 15.0f)
                buffer.putFloat(terrain.sunLight(x, y, z) / 15.0f)
                matrix.values.forEach { buffer.putFloat(it) }
                count++
            }
        }
        return count
    }

    companion object {
        private val EMPTY_FLOAT = floatArrayOf()
        private val SIZE = 0.25f

        private fun createAttributes(): List<ModelAttribute> {
            val attributes = ArrayList<ModelAttribute>()
            attributes.add(ModelAttribute(GL.VERTEX_ATTRIBUTE, 3,
                    floatArrayOf(0.0f, 0.0f, 0.0f, -SIZE, -SIZE, -SIZE), false,
                    0,
                    VertexType.HALF_FLOAT))
            attributes.add(ModelAttribute(GL.COLOR_ATTRIBUTE, 4,
                    floatArrayOf(0.0f, 0.3f, 0.5f, 0.6f, 0.0f, 0.3f, 0.5f,
                            0.0f),
                    true, 0, VertexType.UNSIGNED_BYTE))
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
