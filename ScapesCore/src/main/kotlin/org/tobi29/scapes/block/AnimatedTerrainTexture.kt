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
import org.tobi29.stdex.atomic.AtomicInt
import org.tobi29.graphics.generateMipMaps
import org.tobi29.io.ByteBufferNative
import org.tobi29.io.ByteBufferView
import org.tobi29.io.ByteViewRO
import org.tobi29.io.viewBufferE
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.remP
import org.tobi29.utils.toArray

class AnimatedTerrainTexture(buffer: ByteViewRO,
                             width: Int,
                             height: Int,
                             asset: Array<out String>,
                             shaderAnimation: ShaderAnimation,
                             texture: () -> Texture) : TerrainTexture(
        null, width, width, asset, shaderAnimation, texture) {
    private val frames: Array<Array<ByteBufferView>>
    private val newFrame = AtomicInt(-1)
    private var spin = 0.0

    init {
        val frameSize = width * width shl 2
        frames = (0 until height / width).asSequence().map {
            generateMipMaps(
                    buffer.slice(it * frameSize, frameSize),
                    { ByteBufferNative(it).viewBufferE }, width, width, 4, true)
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
        val old = spin.floorToInt()
        spin = (spin + delta * 20.0) remP frames.size.toDouble()
        val i = spin.floorToInt()
        if (old != i) {
            newFrame.set(i)
        }
    }
}
