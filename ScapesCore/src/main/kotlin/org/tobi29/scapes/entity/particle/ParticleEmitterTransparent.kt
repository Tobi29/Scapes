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

import org.tobi29.graphics.Cam
import org.tobi29.math.AABB3
import org.tobi29.math.FastMath
import org.tobi29.math.atan2Fast
import org.tobi29.math.matrix.Matrix4f
import org.tobi29.math.vector.distanceSqr
import org.tobi29.math.vector.length
import org.tobi29.scapes.block.light
import org.tobi29.scapes.chunk.terrain.block
import org.tobi29.scapes.engine.graphics.loadShader
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.shader.IntegerExpression
import org.tobi29.stdex.math.HALF_PI
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.mix
import kotlin.math.max

class ParticleEmitterTransparent(
        system: ParticleSystem,
        val atlas: ParticleTransparentAtlas
) : ParticleEmitterInstanced<ParticleInstanceTransparent>(
        system, atlas.texture, ParticleEmitterTransparent.createAttributes(), 6,
        ParticleEmitterTransparent.createAttributesStream(),
        RenderType.TRIANGLES, Array(10240, { ParticleInstanceTransparent() })) {
    private val instancesSorted: Array<ParticleInstanceTransparent>
    private val matrix = Matrix4f()

    init {
        instancesSorted = Array(maxInstances, { instances[it] })
    }

    override fun prepareShader(gl: GL,
                               width: Int,
                               height: Int,
                               cam: Cam): suspend () -> ((Shader) -> Unit) -> Unit {
        val shader = system.world.game.engine.graphics.loadShader(
                "Scapes:shader/ParticleTransparent.stag", mapOf(
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
                    cam.position.x,
                    cam.position.y) / 15.0f
            val playerLight = max(
                    player.leftWeapon().light,
                    player.rightWeapon().light).toFloat()
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
        val camPos = cam.position.now()
        val world = system.world
        val terrain = world.terrain
        for (instance in instancesSorted) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue
            }
            instance.posRender = instance.pos.now()
        }
        instancesSorted.sortByDescending { it.posRender.distanceSqr(camPos) }
        var count = 0
        for (instance in instancesSorted) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue
            }
            val x = instance.posRender.x.floorToInt()
            val y = instance.posRender.y.floorToInt()
            val z = instance.posRender.z.floorToInt()
            if (terrain.block(x, y, z) {
                !isSolid(it) || isTransparent(it)
            }) {
                val posRenderX = (instance.posRender.x - camPos.x).toFloat()
                val posRenderY = (instance.posRender.y - camPos.y).toFloat()
                val posRenderZ = (instance.posRender.z - camPos.z).toFloat()
                val yaw = atan2Fast(-posRenderY, -posRenderX)
                val pitch = atan2Fast(posRenderZ,
                        length(posRenderX, posRenderY))
                matrix.identity()
                matrix.translate(posRenderX, posRenderY, posRenderZ)
                matrix.rotateRad(yaw + HALF_PI.toFloat(), 0f, 0f, 1f)
                matrix.rotateRad(pitch, 1f, 0f, 0f)
                matrix.rotateRad(yaw + instance.dir, 0f, 1f, 0f)
                val progress = instance.time / instance.timeMax
                val size = mix(instance.sizeEnd, instance.sizeStart, progress)
                val r = mix(instance.rEnd, instance.rStart, progress)
                val g = mix(instance.gEnd, instance.gStart, progress)
                val b = mix(instance.bEnd, instance.bStart, progress)
                val a = mix(instance.aEnd, instance.aStart, progress)
                matrix.scale(size, size, size)
                buffer.putShort(FastMath.convertFloatToHalf(r))
                buffer.putShort(FastMath.convertFloatToHalf(g))
                buffer.putShort(FastMath.convertFloatToHalf(b))
                buffer.putShort(FastMath.convertFloatToHalf(a))
                buffer.putFloat(terrain.blockLight(x, y, z) / 15.0f)
                buffer.putFloat(terrain.sunLight(x, y, z) / 15.0f)
                matrix.values.forEach { buffer.putFloat(it) }
                buffer.putFloat(instance.textureOffset.x.toFloat())
                buffer.putFloat(instance.textureOffset.y.toFloat())
                buffer.putFloat(instance.textureSize.x.toFloat())
                buffer.putFloat(instance.textureSize.y.toFloat())
                count++
            }
        }
        return count
    }

    override fun initInstance(instance: ParticleInstanceTransparent,
                              consumer: (ParticleInstanceTransparent) -> Unit) {
        super.initInstance(instance, consumer)
        instance.timeMax = instance.time
    }

    override fun update(delta: Double) {
        if (!hasAlive) {
            return
        }
        val aabb = AABB3()
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
            if (!instance.physics) {
                continue
            }
            aabb.min.x = instance.pos.x - instance.sizeStart
            aabb.min.y = instance.pos.y - instance.sizeStart
            aabb.min.z = instance.pos.z - instance.sizeStart
            aabb.max.x = instance.pos.x + instance.sizeStart
            aabb.max.y = instance.pos.y + instance.sizeStart
            aabb.max.z = instance.pos.z + instance.sizeStart
            ParticlePhysics.update(delta, instance, terrain, aabb, gravitation,
                    instance.gravitationMultiplier, instance.airFriction,
                    instance.groundFriction, instance.waterFriction)
        }
        this.hasAlive = hasAlive
    }

    companion object {
        private val EMPTY_FLOAT = floatArrayOf()

        private fun createAttributes(): List<ModelAttribute> {
            val attributes = ArrayList<ModelAttribute>()
            attributes.add(ModelAttribute(GL.VERTEX_ATTRIBUTE, 3,
                    floatArrayOf(-1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f,
                            0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f,
                            1.0f, 0.0f, 1.0f), false, 0, VertexType.HALF_FLOAT))
            attributes.add(ModelAttribute(GL.TEXTURE_ATTRIBUTE, 2,
                    floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f), false, 0,
                    VertexType.FLOAT))
            return attributes
        }

        private fun createAttributesStream(): List<ModelAttribute> {
            val attributes = ArrayList<ModelAttribute>()
            attributes.add(ModelAttribute(GL.COLOR_ATTRIBUTE, 4, EMPTY_FLOAT,
                    false, 1, VertexType.HALF_FLOAT))
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
            attributes.add(ModelAttribute(9, 4, EMPTY_FLOAT, false, 1,
                    VertexType.FLOAT))
            return attributes
        }
    }
}
