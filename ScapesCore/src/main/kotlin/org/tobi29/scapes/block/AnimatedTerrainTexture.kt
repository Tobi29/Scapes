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

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.utils.AtomicInteger
import org.tobi29.scapes.engine.utils.graphics.generateMipMaps
import org.tobi29.scapes.engine.utils.io.ByteBuffer
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.engine.utils.math.remP
import org.tobi29.scapes.engine.utils.toArray

class AnimatedTerrainTexture(buffer: ByteBuffer,
                             width: Int,
                             height: Int,
                             shaderAnimation: ShaderAnimation,
                             engine: ScapesEngine,
                             texture: () -> Texture) : TerrainTexture(
        null, width, width, shaderAnimation, texture) {
    private val frames: Array<Array<ByteBuffer>>
    private val newFrame = AtomicInteger(-1)
    private var spin = 0.0

    init {
        val frameSize = width * width shl 2
        frames = (0..height / width - 1).asSequence().map {
            buffer.position(it * frameSize)
            buffer.limit(buffer.position() + frameSize)
            generateMipMaps(buffer, engine, width, width, 4,
                    true)
        }.toArray()
    }

    override fun renderAnim(gl: GL) {
        val frame = newFrame.getAndSet(-1)
        if (frame >= 0) {
            texture().bind(gl)
            gl.replaceTextureMipMap(x, y, width, height, *frames[frame])
        }
    }

    override fun updateAnim(delta: Double) {
        val old = floor(spin)
        spin = (spin + delta * 20.0) remP frames.size.toDouble()
        val i = floor(spin)
        if (old != i) {
            newFrame.set(i)
        }
    }
}
