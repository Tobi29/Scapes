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

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GraphicsObjectSupplier
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.math.margin
import org.tobi29.math.vector.MutableVector2i
import org.tobi29.stdex.atomic.AtomicInt
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.computeAbsent
import org.tobi29.graphics.*

class ParticleTransparentAtlas internal constructor(
        val texture: Texture,
        private val entries: Array<ParticleTransparentEntry?>) {
    fun entry(id: Int): ParticleTransparentEntry? = entries.getOrNull(id)
}

class ParticleTransparentEntry(val x: Int,
                               val y: Int,
                               val width: Int,
                               val height: Int,
                               atlasWidth: Int,
                               atlasHeight: Int) {
    val textureX = x.toDouble() / atlasWidth
    val textureY = y.toDouble() / atlasHeight
    val textureWidth = width.toDouble() / atlasWidth
    val textureHeight = height.toDouble() / atlasHeight
}

inline fun ParticleTransparentEntry.atPixelX(value: Int): Double {
    return value / textureWidth
}

inline fun ParticleTransparentEntry.atPixelY(value: Int): Double {
    return value / textureHeight
}

inline fun ParticleTransparentEntry.atPixelMarginX(value: Int): Double {
    return marginX(atPixelX(value))
}

inline fun ParticleTransparentEntry.atPixelMarginY(value: Int): Double {
    return marginY(atPixelX(value))
}

inline fun ParticleTransparentEntry.marginX(value: Double,
                                            margin: Double = 0.005): Double {
    return textureX + margin(value, margin) * textureWidth
}

inline fun ParticleTransparentEntry.marginY(value: Double,
                                            margin: Double = 0.005): Double {
    return textureY + margin(value, margin) * textureHeight
}

class ParticleTransparentAtlasBuilder {
    private val textures = ConcurrentHashMap<String, Int>()
    private val idCounter = AtomicInt(0)

    fun registerTexture(asset: String): Int =
            textures.computeAbsent(asset) { idCounter.getAndIncrement() }

    suspend fun build(
            engine: ScapesEngine,
            texture: GraphicsObjectSupplier.(Image) -> Texture
    ): ParticleTransparentAtlas {
        val tiles = textures.entries.map { (asset, id) ->
            val resource = engine.resources.load {
                engine.files[asset].readAsync { decodePNG(it) }
            }
            id to resource
        }.map { (id, resource) ->
            val image = resource.getAsync()
            Triple(id, image, MutableVector2i())
        }
        val atlasSize = assembleAtlas(
                tiles.asSequence().map { (_, image, position) -> image.size to position })
        val atlas = MutableImage(atlasSize.x, atlasSize.y)
        for ((_, image, position) in tiles) {
            atlas.set(position.x, position.y, image)
        }
        val otiles = ArrayList<ParticleTransparentEntry?>(tiles.size)
        for ((id, image, position) in tiles) {
            while (otiles.size <= id) otiles.add(null)
            otiles[id] = ParticleTransparentEntry(position.x, position.y,
                    image.width, image.height, atlas.width, atlas.height)
        }
        return ParticleTransparentAtlas(
                texture(engine.graphics, atlas.toImage()),
                otiles.toTypedArray())
    }
}
