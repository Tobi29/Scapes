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
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.shader.IntegerExpression
import org.tobi29.scapes.entity.particle.ParticleEmitter
import org.tobi29.scapes.entity.particle.ParticleInstance
import org.tobi29.scapes.entity.particle.ParticleSystem

class ParticleEmitterLightning(system: ParticleSystem) : ParticleEmitter<ParticleInstanceLightning>(
        system, Array(256, { ParticleInstanceLightning() })) {
    private val models: Array<Model>

    init {
        models = Array(16) {
            val lines = createLighting()
            val vertex = FloatArray(lines.size * 6)
            val normal = FloatArray(lines.size * 6)
            var j = 0
            for (line in lines) {
                vertex[j] = line.start.floatX()
                normal[j++] = 0.0f
                vertex[j] = line.start.floatY()
                normal[j++] = 0.0f
                vertex[j] = line.start.floatZ()
                normal[j++] = 1.0f
                vertex[j] = line.end.floatX()
                normal[j++] = 0.0f
                vertex[j] = line.end.floatY()
                normal[j++] = 0.0f
                vertex[j] = line.end.floatZ()
                normal[j++] = 1.0f
            }
            val index = IntArray(lines.size shl 1)
            j = 0
            while (j < index.size) {
                index[j] = j
                j++
            }
            system.world.game.engine.graphics.createVNI(vertex, normal, index,
                    RenderType.LINES)
        }
        for (i in models.indices) {
            val lines = createLighting()
            val vertex = FloatArray(lines.size * 6)
            val normal = FloatArray(lines.size * 6)
            var j = 0
            for (line in lines) {
                vertex[j] = line.start.floatX()
                normal[j++] = 0.0f
                vertex[j] = line.start.floatY()
                normal[j++] = 0.0f
                vertex[j] = line.start.floatZ()
                normal[j++] = 1.0f
                vertex[j] = line.end.floatX()
                normal[j++] = 0.0f
                vertex[j] = line.end.floatY()
                normal[j++] = 0.0f
                vertex[j] = line.end.floatZ()
                normal[j++] = 1.0f
            }
            val index = IntArray(lines.size shl 1)
            j = 0
            while (j < index.size) {
                index[j] = j
                j++
            }
            models[i] = system.world.game.engine.graphics.createVNI(vertex,
                    normal, index, RenderType.LINES)
        }
    }

    private fun createLighting(): List<Line> {
        val random = threadLocalRandom()
        val lines = ArrayList<Line>()
        var x = 0.0
        var y = 0.0
        var start = Vector3d.ZERO
        var z = 10.0
        while (z < 100.0) {
            x += random.nextDouble() * 6.0 - 3.0
            y += random.nextDouble() * 6.0 - 3.0
            val end = Vector3d(x, y, z)
            lines.add(Line(start, end))
            start = end
            if (random.nextInt(2) == 0 && z > 40) {
                createLightingArm(lines, x, y, z)
            }
            z += random.nextDouble() * 4 + 2
        }
        return lines
    }

    private fun createLightingArm(lines: MutableList<Line>,
                                  x: Double,
                                  y: Double,
                                  z: Double) {
        val random = threadLocalRandom()
        var start = Vector3d(x, y, z)
        var dir = random.nextDouble() * TWO_PI
        var xx = x
        var yy = y
        var zz = z
        val xs = cos(dir)
        val ys = sin(dir)
        dir = pow(random.nextDouble(), 6.0) * 20.0 + 0.2
        for (i in 0 until random.nextInt(30) + 4) {
            xx += xs * random.nextDouble() * dir
            yy += ys * random.nextDouble() * dir
            val end = Vector3d(xx, yy, zz)
            lines.add(Line(start, end))
            start = end
            zz -= random.nextDouble() * 4.0 + 2.0
            if (zz < 20.0) {
                return
            }
        }
    }

    fun maxVAO(): Int {
        return models.size
    }

    override fun update(delta: Double) {
        if (!hasAlive) {
            return
        }
        var hasAlive = false
        for (instance in instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue
            }
            hasAlive = true
            instance.time -= delta.toFloat()
            if (instance.time <= 0.0) {
                instance.state = ParticleInstance.State.DEAD
            }
        }
        this.hasAlive = hasAlive
    }

    override fun addToPipeline(gl: GL,
                               width: Int,
                               height: Int,
                               cam: Cam): () -> Unit {
        val shader = gl.engine.graphics.loadShader(
                "VanillaBasics:shader/ParticleLightning", mapOf(
                "SCENE_WIDTH" to IntegerExpression(width),
                "SCENE_HEIGHT" to IntegerExpression(height)
        ))
        return render@ {
            if (!hasAlive) {
                return@render
            }
            val world = system.world
            val terrain = world.terrain
            gl.engine.graphics.textureEmpty().bind(gl)
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
                    gl.matrixStack.push { matrix ->
                        matrix.translate(posRenderX, posRenderY, posRenderZ)
                        models[instance.vao].render(gl, s)
                    }
                }
            }
        }
    }

    private class Line(val start: Vector3d,
                       val end: Vector3d)
}
