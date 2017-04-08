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

package org.tobi29.scapes.client.states.scenes

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.utils.chain
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.graphics.gaussianBlurOffset
import org.tobi29.scapes.engine.utils.graphics.gaussianBlurWeight
import org.tobi29.scapes.engine.utils.join
import org.tobi29.scapes.engine.utils.math.PI
import org.tobi29.scapes.engine.utils.math.cos
import org.tobi29.scapes.engine.utils.math.remP
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.use
import org.tobi29.scapes.server.format.WorldSource
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference

open class SceneMenu(engine: ScapesEngine) : Scene(engine) {
    private val textures = arrayOfNulls<Texture>(6)
    private val cam: Cam
    private val save = AtomicReference<WorldSource.Panorama?>()
    private var speed = -0.6f
    private var yaw = 0.0f

    init {
        cam = Cam(0.4f, 2.0f)
        val random = ThreadLocalRandom.current()
        yaw = random.nextFloat() * 360.0f
        loadTextures()
    }

    override fun appendToPipeline(gl: GL): () -> Unit {
        val shaderBlur1 = gl.engine.graphics.loadShader("Scapes:shader/Menu1") {
            supplyPreCompile { gl ->
                blur(gl, this)
            }
        }
        val shaderBlur2 = gl.engine.graphics.loadShader("Scapes:shader/Menu2") {
            supplyPreCompile { gl ->
                blur(gl, this)
            }
        }
        val shaderTextured = gl.engine.graphics.loadShader(
                "Engine:shader/Textured")
        val model = engine.graphics.createVTI(
                floatArrayOf(-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f,
                        -1.0f, -1.0f, 1.0f, -1.0f, -1.0f),
                floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f),
                intArrayOf(0, 1, 2, 2, 1, 3), RenderType.TRIANGLES)

        val f1 = gl.engine.graphics.createFramebuffer(
                gl.contentWidth(), gl.contentHeight(), 1, false, false, false)
        val f2 = gl.engine.graphics.createFramebuffer(
                gl.contentWidth(), gl.contentHeight(), 1, false, false, false)
        val f3 = gl.engine.graphics.createFramebuffer(
                gl.contentWidth(), gl.contentHeight(), 1, false, false, false,
                TextureFilter.LINEAR)
        val render = gl.into(f1) {
            gl.clearDepth()
            val save = save.getAndSet(null)
            if (save != null) {
                changeBackground(save)
            }
            cam.setPerspective(gl.aspectRatio().toFloat(), 90.0f)
            cam.setView(0.0f, yaw, 0.0f)
            gl.matrixStack.push { matrix ->
                gl.enableCulling()
                gl.enableDepthTest()
                gl.setBlending(BlendingMode.NORMAL)
                matrix.identity()
                matrix.modelViewProjection().perspective(cam.fov,
                        gl.aspectRatio().toFloat(), cam.near, cam.far)
                matrix.modelViewProjection().camera(cam)
                matrix.modelView().camera(cam)
                for (i in 0..5) {
                    val texture = textures[i]
                    if (texture != null) {
                        gl.matrixStack.push { matrix ->
                            if (i == 1) {
                                matrix.rotate(90.0f, 0.0f, 0.0f, 1.0f)
                            } else if (i == 2) {
                                matrix.rotate(180.0f, 0.0f, 0.0f, 1.0f)
                            } else if (i == 3) {
                                matrix.rotate(270.0f, 0.0f, 0.0f, 1.0f)
                            } else if (i == 4) {
                                matrix.rotate(90.0f, 1.0f, 0.0f, 0.0f)
                            } else if (i == 5) {
                                matrix.rotate(-90.0f, 1.0f, 0.0f, 0.0f)
                            }
                            texture.bind(gl)
                            gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 1.0f,
                                    1.0f, 1.0f)
                            model.render(gl, shaderTextured.get())
                        }
                    }
                }
            }
        }
        val pp1 = gl.into(f2, postProcess(gl, shaderBlur1, f1))
        val pp2 = gl.into(f3, postProcess(gl, shaderBlur2, f2))
        val upscale = postProcess(gl, shaderTextured, f3)
        return chain(render, pp1, pp2, upscale)
    }

    private fun blur(gl: GL,
                     processor: ShaderPreprocessor) {
        val space = gl.contentSpace()
        val samples = round(space * 8.0) + 8
        val blurOffsets = gaussianBlurOffset(samples, 0.04f)
        val blurWeights = gaussianBlurWeight(samples) { cos(it * PI) }
        processor.supplyProperty("BLUR_OFFSET", blurOffsets.joinToString())
        processor.supplyProperty("BLUR_WEIGHT", blurWeights.joinToString())
        processor.supplyProperty("BLUR_LENGTH", blurOffsets.size)
    }

    @Throws(IOException::class)
    fun changeBackground(source: WorldSource) {
        save.set(saveBackground(source))
    }

    fun step(delta: Double) {
        yaw = (yaw + speed * delta).toFloat() remP 360.0f
    }

    protected open fun loadTextures() {
        val game = engine.game as ScapesClient
        val saves = game.saves
        val random = ThreadLocalRandom.current()
        try {
            val list = saves.list().toList()
            if (list.isEmpty()) {
                throw IOException("No save available")
            }
            saves[list[random.nextInt(list.size)]].use { source ->
                val images = source.panorama()
                if (images != null) {
                    changeBackground(images)
                } else {
                    defaultBackground()
                }
            }
        } catch (e: IOException) {
            defaultBackground()
        }
    }

    protected fun setBackground(replace: Resource<Texture>,
                                i: Int) {
        textures[i] = replace.get()
    }

    protected fun setBackground(replace: Texture,
                                i: Int) {
        val texture = textures[i]
        texture?.markDisposed()
        textures[i] = replace
    }

    private fun saveBackground(source: WorldSource): WorldSource.Panorama? {
        return source.panorama()
    }

    private fun changeBackground(panorama: WorldSource.Panorama) {
        panorama.elements.indices.forEach {
            val image = panorama.elements[it]
            setBackground(engine.graphics.createTexture(image, 0,
                    TextureFilter.LINEAR,
                    TextureFilter.LINEAR, TextureWrap.CLAMP,
                    TextureWrap.CLAMP), it)
        }
    }

    private fun defaultBackground() {
        val random = ThreadLocalRandom.current()
        val r = random.nextInt(2)
        for (i in 0..5) {
            setBackground(
                    engine.graphics.textures()["Scapes:image/gui/panorama/$r/Panorama$i"],
                    i)
        }
    }
}
