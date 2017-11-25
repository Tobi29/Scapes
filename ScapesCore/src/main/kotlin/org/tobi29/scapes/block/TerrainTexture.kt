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

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.utils.io.ByteViewRO
import org.tobi29.scapes.engine.math.margin

open class TerrainTexture(var buffer: ByteViewRO?,
                          val width: Int,
                          val height: Int,
                          val asset: Array<out String>,
                          protected val shaderAnimation: ShaderAnimation,
                          protected val texture: () -> Texture) {
    var x = 0
        internal set
    var y = 0
        internal set
    var textureX = 0.0
        internal set
    var textureY = 0.0
        internal set
    var textureWidth = 0.0
        internal set
    var textureHeight = 0.0
        internal set

    fun getTexture(): Texture {
        return texture.invoke()
    }

    fun shaderAnimation(): ShaderAnimation {
        return shaderAnimation
    }

    open fun renderAnim(gl: GL) {
    }

    open fun updateAnim(delta: Double) {
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun TerrainTexture.atPixelX(value: Int): Double {
    return value / textureWidth
}

@Suppress("NOTHING_TO_INLINE")
inline fun TerrainTexture.atPixelY(value: Int): Double {
    return value / textureHeight
}

@Suppress("NOTHING_TO_INLINE")
inline fun TerrainTexture.atPixelMarginX(value: Int): Double {
    return marginX(atPixelX(value))
}

@Suppress("NOTHING_TO_INLINE")
inline fun TerrainTexture.atPixelMarginY(value: Int): Double {
    return marginY(atPixelX(value))
}

@Suppress("NOTHING_TO_INLINE")
inline fun TerrainTexture.marginX(value: Double,
                                  margin: Double = 0.005): Double {
    return textureX + margin(value, margin) * textureWidth
}

@Suppress("NOTHING_TO_INLINE")
inline fun TerrainTexture.marginY(value: Double,
                                  margin: Double = 0.005): Double {
    return textureY + margin(value, margin) * textureHeight
}
