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
import org.tobi29.scapes.engine.graphics.TextureAtlasEngineEntry

import java.nio.ByteBuffer

open class TerrainTexture(buffer: ByteBuffer?,
                          width: Int,
                          height: Int,
                          protected val shaderAnimation: ShaderAnimation,
                          texture: () -> Texture) : TextureAtlasEngineEntry(
        buffer, width, height, texture) {

    fun shaderAnimation(): ShaderAnimation {
        return shaderAnimation
    }

    open fun renderAnim(gl: GL) {
    }

    open fun updateAnim(delta: Double) {
    }
}
