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
import org.tobi29.scapes.engine.utils.AtomicReference
import org.tobi29.scapes.engine.utils.chain
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.graphics.gaussianBlurOffset
import org.tobi29.scapes.engine.utils.graphics.gaussianBlurWeight
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.shader.ArrayExpression
import org.tobi29.scapes.engine.utils.shader.IntegerExpression
import org.tobi29.scapes.engine.utils.use
import org.tobi29.scapes.server.format.WorldSource

open class SceneMenu(engine: ScapesEngine) : Scene(engine) {
    private val textures = arrayOfNulls<Texture>(6)
    private val cam: Cam
    private val save = AtomicReference<WorldSource.Panorama?>()
    private var speed = -0.6f
    private var yaw = 0.0f

    init {
        cam = Cam(0.4f, 2.0f)
        val random = threadLocalRandom()
        yaw = random.nextFloat() * 360.0f
        loadTextures()
    }

    override fun appendToPipeline(gl: GL): suspend () -> (Double) -> Unit {
        val space = gl.contentSpace() / 8.0
        val blurSamples = round(space * 8.0) + 8
        val width = gl.contentWidth() / 8
        val height = gl.contentHeight() / 8

        // Background box pane
        val model = engine.graphics.createVTI(
                floatArrayOf(-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f,
                        -1.0f, -1.0f, 1.0f, -1.0f, -1.0f),
                floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f),
                intArrayOf(0, 1, 2, 2, 1, 3), RenderType.TRIANGLES)

        // Shaders
        val shaderBlur1 = gl.engine.graphics.loadShader(
                "Scapes:shader/Menu1", mapOf(
                "BLUR_LENGTH" to IntegerExpression(blurSamples),
                "BLUR_OFFSET" to ArrayExpression(
                        gaussianBlurOffset(blurSamples, 0.04)),
                "BLUR_WEIGHT" to ArrayExpression(
                        gaussianBlurWeight(blurSamples) { cos(it * PI) })
        ))
        val shaderBlur2 = gl.engine.graphics.loadShader(
                "Scapes:shader/Menu2", mapOf(
                "BLUR_LENGTH" to IntegerExpression(blurSamples),
                "BLUR_OFFSET" to ArrayExpression(
                        gaussianBlurOffset(blurSamples, 0.04)),
                "BLUR_WEIGHT" to ArrayExpression(
                        gaussianBlurWeight(blurSamples) { cos(it * PI) })
        ))
        val shaderTextured = gl.engine.graphics.loadShader(SHADER_TEXTURED)

        // Framebuffers
        val f1 = gl.engine.graphics.createFramebuffer(width, height, 1,
                false, false, false)
        val f2 = gl.engine.graphics.createFramebuffer(width, height, 1,
                false, false, false)
        val f3 = gl.engine.graphics.createFramebuffer(width, height, 1,
                false, false, false, TextureFilter.LINEAR)

        // Render steps
        val render: suspend () -> (Double) -> Unit = {
            val s = shaderTextured.getAsync()
            gl.into(f1) {
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
                        textures[i]?.let { texture ->
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
                                gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f,
                                        1.0f, 1.0f, 1.0f)
                                model.render(gl, s)
                            }
                        }
                    }
                }
            }
        }
        val pp1Render: suspend () -> (Double) -> Unit = {
            gl.into(f2, postProcess(gl, shaderBlur1.getAsync(), f1))
        }
        val pp2Render: suspend () -> (Double) -> Unit = {
            gl.into(f3, postProcess(gl, shaderBlur2.getAsync(), f2))
        }
        val upscaleRender: suspend () -> (Double) -> Unit = {
            postProcess(gl, shaderTextured.getAsync(), f3)
        }

        return {
            chain(render(), pp1Render(), pp2Render(), upscaleRender())
        }
    }

    fun changeBackground(source: WorldSource) {
        save.set(saveBackground(source))
    }

    fun step(delta: Double) {
        yaw = (yaw + speed * delta).toFloat() remP 360.0f
    }

    protected open fun loadTextures() {
        val game = engine.game as ScapesClient
        val saves = game.saves
        val random = threadLocalRandom()
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
        val random = threadLocalRandom()
        val r = random.nextInt(2)
        for (i in 0..5) {
            setBackground(
                    engine.graphics.textures()["Scapes:image/gui/panorama/$r/Panorama$i"],
                    i)
        }
    }
}
