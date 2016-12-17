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
package org.tobi29.scapes.block

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.utils.graphics.generateMipMaps
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.engine.utils.toArray
import java.nio.ByteBuffer

class AnimatedTerrainTexture(buffer: ByteBuffer, width: Int, height: Int,
                             shaderAnimation: ShaderAnimation, engine: ScapesEngine, texture: () -> Texture) : TerrainTexture(
        buffer, width, shaderAnimation, texture) {
    private val frames: Array<Array<ByteBuffer?>>
    private var dirty = true
    private var spin: Double = 0.0
    private var i = 0

    init {
        val frameSize = width * width shl 2
        frames = (0..height / width - 1).asSequence().map {
            buffer.position(it * frameSize)
            generateMipMaps(buffer, { engine.allocate(it) }, width, width, 4,
                    true)
        }.toArray()
    }

    override fun renderAnim(gl: GL) {
        if (dirty) {
            texture().bind(gl)
            gl.replaceTextureMipMap(tileX, tileY, resolution, resolution,
                    *frames[i])
            dirty = false
        }
    }

    override fun updateAnim(delta: Double) {
        spin += delta * 20.0
        var i = floor(spin)
        if (i >= frames.size) {
            spin -= frames.size.toDouble()
            i = 0
        }
        this.i = i
        dirty = true
    }
}
