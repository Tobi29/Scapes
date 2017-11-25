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

package org.tobi29.scapes.block

import kotlinx.coroutines.experimental.runBlocking
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.math.vector.MutableVector2i
import org.tobi29.scapes.engine.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.graphics.*
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.ReadSource
import org.tobi29.scapes.engine.utils.logging.KLogging

class TerrainTextureRegistry(val engine: ScapesEngine) {
    private val minSize: Int = 16
    private val registered = ConcurrentHashMap<Triple<List<ReadSource>, Boolean, ShaderAnimation>, TerrainTexture>()
    private val sources = ConcurrentHashMap<ReadSource, Image>()
    private var imageMut: Image? = null
    val image: Image
        get() = imageMut ?: throw IllegalStateException(
                "Atlas not finished yet")
    var textureMut: Texture? = null
        private set
    val texture: Texture
        get() = textureMut ?: throw IllegalStateException(
                "Atlas not finished yet")

    fun init(): Int {
        val entries = registered.entries.map { (key, value) ->
            Triple(key, value, MutableVector2i())
        }
        val atlasSize = assembleAtlas(
                entries.asSequence().map {
                    Vector2i(it.second.width, it.second.height) to it.third
                })
        val image = MutableImage(atlasSize.x, atlasSize.y)
        paint(entries.asSequence().map { it.second to it.third }, image)
        imageMut = image.toImage()
        sources.clear()
        return registered.size
    }

    private fun paint(entries: Sequence<Pair<TerrainTexture, MutableVector2i>>,
                      image: MutableImage) {
        for ((texture, position) in entries) {
            texture.textureX = position.x.toDouble() / image.width
            texture.textureY = position.y.toDouble() / image.height
            texture.x = position.x
            texture.y = position.y
            texture.textureWidth = texture.width.toDouble() / image.width
            texture.textureHeight = texture.height.toDouble() / image.height
            texture.buffer?.let {
                image.set(position.x, position.y,
                        texture.width, texture.height, it)
            }
            texture.buffer = null
        }
    }

    private suspend fun loadIfNeeded(resource: ReadSource): Image =
            sources[resource] ?: run {
                resource.readAsync { decodePNG(it) }.also {
                    sources.put(resource, it)
                }
            }

    private suspend fun load(paths: Iterable<ReadSource>): Image {
        val iterator = paths.iterator()
        return if (!iterator.hasNext()) Image(minSize, minSize)
        else try {
            val source = loadIfNeeded(iterator.next())
            if (iterator.hasNext()) {
                val merge = source.toMutableImage()
                while (iterator.hasNext()) {
                    val path = iterator.next()
                    val layer = loadIfNeeded(path)
                    if (merge.width != layer.width || merge.height != layer.height) {
                        logger.warn { "Invalid size for layered texture from: $path" }
                        continue
                    }
                    merge.mergeBelow(layer)
                }
                merge.toImage()
            } else source
        } catch (e: IOException) {
            logger.error { "Failed to load texture: $e" }
            Image(minSize, minSize)
        }
    }

    fun initTexture(mipmaps: Int = 0) {
        textureMut = engine.graphics.createTexture(image, mipmaps)
    }

    fun registerTexture(vararg paths: String): TerrainTexture {
        return registerTexture(paths, false, ShaderAnimation.NONE)
    }

    fun registerTexture(path: String,
                        shaderAnimation: ShaderAnimation): TerrainTexture {
        return registerTexture(path, false, shaderAnimation)
    }

    fun registerTexture(paths: Array<out String>,
                        shaderAnimation: ShaderAnimation): TerrainTexture {
        return registerTexture(paths, false, shaderAnimation)
    }

    fun registerTexture(path: String,
                        animated: Boolean = false,
                        shaderAnimation: ShaderAnimation = ShaderAnimation.NONE): TerrainTexture {
        return registerTexture(arrayOf(path), animated, shaderAnimation)
    }

    fun registerTexture(paths: Array<out String>,
                        animated: Boolean,
                        shaderAnimation: ShaderAnimation = ShaderAnimation.NONE): TerrainTexture {
        return registered.computeIfAbsent(
                Triple(paths.map { engine.files[it] }, animated,
                        shaderAnimation)) {
            val image = runBlocking { load(paths.map { engine.files[it] }) }
            if (animated) {
                AnimatedTerrainTexture(image.view, image.width, image.height,
                        paths, shaderAnimation, { texture })
            } else {
                TerrainTexture(image.view, image.width, image.height,
                        paths, shaderAnimation, { texture })
            }
        }
    }

    fun render(gl: GL) {
        for (texture in registered.values) {
            texture.renderAnim(gl)
        }
    }

    fun update(delta: Double) {
        registered.values.forEach { it.updateAnim(delta) }
    }

    companion object : KLogging()
}
