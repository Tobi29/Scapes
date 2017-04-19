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

import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.engine.utils.graphics.marginX
import org.tobi29.scapes.engine.utils.graphics.marginY
import org.tobi29.scapes.engine.utils.Random
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2d

class ParticleInstanceBlock : ParticleInstance() {
    val textureOffset = MutableVector2d()
    val textureSize = MutableVector2d()
    var friction = 0.0f
    var dir = 0.0f
    var r: Byte = 0
    var g: Byte = 0
    var b: Byte = 0
    var a: Byte = 0

    fun setColor(r: Float,
                 g: Float,
                 b: Float,
                 a: Float) {
        setColor(r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble())
    }

    fun setColor(r: Double,
                 g: Double,
                 b: Double,
                 a: Double) {
        this.r = round(r * 255.0).toByte()
        this.g = round(g * 255.0).toByte()
        this.b = round(b * 255.0).toByte()
        this.a = round(a * 255.0).toByte()
    }

    fun setTexture(texture: TerrainTexture,
                   random: Random) {
        val size = 0.25
        val rx = random.nextInt(4) * size
        val ry = random.nextInt(4) * size
        setTexture(texture, rx, ry, rx + size, ry + size)
    }

    fun setTexture(texture: TerrainTexture,
                   tMinX: Double,
                   tMinY: Double,
                   tMaxX: Double,
                   tMaxY: Double) {
        val texMinX = texture.marginX(tMinX)
        val texMaxX = texture.marginX(tMaxX)
        val texMinY = texture.marginY(tMinY)
        val texMaxY = texture.marginY(tMaxY)
        textureOffset.set(texMinX, texMinY)
        textureSize.set(texMaxX - texMinX, texMaxY - texMinY)
    }
}
