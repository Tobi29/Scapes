/*
 * Copyright 2012-2016 Tobi29
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

import java8.util.stream.Collectors
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.graphics.gaussianBlurOffset
import org.tobi29.scapes.engine.utils.graphics.gaussianBlurWeight
import org.tobi29.scapes.engine.utils.io.use
import org.tobi29.scapes.engine.utils.join
import org.tobi29.scapes.engine.utils.math.FastMath
import org.tobi29.scapes.engine.utils.math.cos
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.server.format.WorldSource
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference

open class SceneMenu(engine: ScapesEngine) : Scene(engine) {
    private val textures = arrayOfNulls<Texture>(6)
    private val cam: Cam
    private val shaderBlur1: Shader
    private val shaderBlur2: Shader
    private val shaderTextured: Shader
    private var speed = 0.6f
    private var yaw = 0.0f
    private var model: Model? = null
    private val save = AtomicReference<WorldSource.Panorama?>()
    private var texturesLoaded = false

    init {
        cam = Cam(0.4f, 2.0f)
        val random = ThreadLocalRandom.current()
        yaw = random.nextFloat() * 360.0f
        val graphics = engine.graphics
        shaderBlur1 = graphics.createShader("Scapes:shader/Menu1"
        ) { information ->
            information.supplyPreCompile({ gl, processor ->
                blur(gl, processor)
            })
        }
        shaderBlur2 = graphics.createShader("Scapes:shader/Menu2"
        ) { information ->
            information.supplyPreCompile({ gl, processor ->
                blur(gl, processor)
            })
        }
        shaderTextured = graphics.createShader("Engine:shader/Textured")
    }

    private fun blur(gl: GL,
                     processor: ShaderPreprocessor) {
        val space = gl.sceneSpace()
        val samples = round(space * 8.0) + 8
        val blurOffsets = gaussianBlurOffset(samples, 0.04f)
        val blurWeights = gaussianBlurWeight(samples) { cos(it * FastMath.PI) }
        val blurLength = blurOffsets.size
        val blurOffset = join(*blurOffsets)
        val blurWeight = join(*blurWeights)
        processor.supplyProperty("BLUR_OFFSET", blurOffset)
        processor.supplyProperty("BLUR_WEIGHT", blurWeight)
        processor.supplyProperty("BLUR_LENGTH", blurLength)
    }

    @Throws(IOException::class)
    fun changeBackground(source: WorldSource) {
        save.set(saveBackground(source))
    }

    override fun init(gl: GL) {
        model = createVTI(engine,
                floatArrayOf(-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f,
                        -1.0f, -1.0f, 1.0f, -1.0f, -1.0f),
                floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f),
                intArrayOf(0, 1, 2, 2, 1, 3), RenderType.TRIANGLES)
        loadTextures(gl)
    }

    override fun renderScene(gl: GL) {
        val save = save.getAndSet(null)
        if (save != null) {
            changeBackground(save)
        }
        cam.setPerspective(gl.sceneWidth().toFloat() / gl.sceneHeight(), 90.0f)
        cam.setView(0.0f, yaw, 0.0f)
        gl.setProjectionPerspective(gl.sceneWidth().toFloat(),
                gl.sceneHeight().toFloat(), cam)
        val matrixStack = gl.matrixStack()
        for (i in 0..5) {
            val texture = textures[i]
            if (texture != null) {
                val matrix = matrixStack.push()
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
                gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f, 1.0f)
                model!!.render(gl, shaderTextured)
                matrixStack.pop()
            }
        }
    }

    override fun postRender(gl: GL,
                            delta: Double) {
        yaw -= (speed * delta).toFloat()
        yaw %= 360f
    }

    override fun postProcessing(gl: GL,
                                pass: Int): Shader? {
        when (pass) {
            0 -> return shaderBlur1
            1 -> return shaderBlur2
        }
        return null
    }

    override fun width(width: Int): Int {
        return width shr 2
    }

    override fun height(height: Int): Int {
        return height shr 2
    }

    override fun renderPasses(): Int {
        return 2
    }

    override fun dispose() {
    }

    fun setSpeed(speed: Float) {
        this.speed = speed
    }

    protected open fun loadTextures(gl: GL) {
        if (texturesLoaded) {
            return
        }
        texturesLoaded = true
        val game = engine.game as ScapesClient
        val saves = game.saves()
        val random = ThreadLocalRandom.current()
        try {
            val list = saves.list().collect(Collectors.toList<String>())
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
