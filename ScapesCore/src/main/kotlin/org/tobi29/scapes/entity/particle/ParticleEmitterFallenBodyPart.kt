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
import org.tobi29.math.vector.add
import org.tobi29.math.vector.divide
import org.tobi29.math.vector.times
import org.tobi29.scapes.block.light
import org.tobi29.scapes.chunk.terrain.block
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.loadShader
import org.tobi29.scapes.engine.graphics.push
import org.tobi29.scapes.engine.shader.IntegerExpression
import org.tobi29.stdex.math.floorToInt
import kotlin.math.max

class ParticleEmitterFallenBodyPart(system: ParticleSystem) :
    ParticleEmitter<ParticleInstanceFallenBodyPart>(
        system, Array(256, { ParticleInstanceFallenBodyPart() })
    ) {
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
            aabb.min.x = instance.pos.x - instance.box.minX
            aabb.min.y = instance.pos.y - instance.box.minY
            aabb.min.z = instance.pos.z - instance.box.minZ
            aabb.max.x = instance.pos.x + instance.box.maxX
            aabb.max.y = instance.pos.y + instance.box.maxY
            aabb.max.z = instance.pos.z + instance.box.maxZ
            if (ParticlePhysics.update(
                    delta, instance, terrain, aabb,
                    gravitation, 1.0f,
                    0.2f, 0.4f, 8.0f
                )) {
                instance.rotationSpeed.divide(
                    1.0 + 0.4 * delta * gravitation.toDouble()
                )
            }
            instance.rotation.add(instance.rotationSpeed.now().times(delta))
        }
        this.hasAlive = hasAlive
    }

    override fun addToPipeline(
        gl: GL,
        width: Int,
        height: Int,
        cam: Cam
    ): suspend () -> (Double) -> Unit {
        val shader = system.world.game.engine.graphics.loadShader(
            "Scapes:shader/Entity.stag", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height)
            )
        )
        return {
            val s = shader.getAsync()
            ;render@{
            if (!hasAlive) {
                return@render
            }
            val world = system.world
            val terrain = world.terrain
            val scene = world.scene
            val environment = world.environment
            val player = world.player
            val cx = cam.position.x
            val cy = cam.position.y
            val cz = cam.position.z
            val time = gl.timer.toFloat()
            val sunLightReduction =
                environment.sunLightReduction(cx, cy) / 15.0f
            val playerLight = max(
                player.leftWeapon().light,
                player.rightWeapon().light
            ).toFloat()
            val sunlightNormal = environment.sunLightNormal(cx, cy)
            val snx = sunlightNormal.x.toFloat()
            val sny = sunlightNormal.y.toFloat()
            val snz = sunlightNormal.z.toFloat()
            val fr = scene.fogR()
            val fg = scene.fogG()
            val fb = scene.fogB()
            val d = scene.fogDistance() * scene.renderDistance()
            s.setUniform3f(gl, 4, fr, fg, fb)
            s.setUniform1f(gl, 5, d)
            s.setUniform1i(gl, 6, 1)
            s.setUniform1f(gl, 7, time)
            s.setUniform1f(gl, 8, sunLightReduction)
            s.setUniform3f(gl, 9, snx, sny, snz)
            s.setUniform1f(gl, 10, playerLight)
            gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f, 1.0f)
            for (instance in instances) {
                if (instance.state != ParticleInstance.State.ALIVE) {
                    continue
                }
                val x = instance.pos.x.floorToInt()
                val y = instance.pos.y.floorToInt()
                val z = instance.pos.z.floorToInt()
                if (terrain.block(x, y, z) {
                        !isSolid(it) || isTransparent(it)
                    }) {
                    val posRenderX = (instance.pos.x - cx).toFloat()
                    val posRenderY = (instance.pos.y - cy).toFloat()
                    val posRenderZ = (instance.pos.z - cz).toFloat()
                    instance.texture.bind(gl)
                    gl.matrixStack.push { matrix ->
                        matrix.translate(posRenderX, posRenderY, posRenderZ)
                        matrix.rotate(instance.rotation.y.toFloat(), 0f, 0f, 1f)
                        matrix.rotate(instance.rotation.x.toFloat(), 1f, 0f, 0f)
                        gl.setAttribute2f(
                            4,
                            world.terrain.blockLight(x, y, z) / 15.0f,
                            world.terrain.sunLight(x, y, z) / 15.0f
                        )
                        instance.box.render(1.0f, 1.0f, 1.0f, 1.0f, gl, s)
                    }
                }
            }
        }
        }
    }
}
