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

package org.tobi29.scapes.entity.skin

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.utils.Checksum

import java.nio.ByteBuffer

class ClientSkin(engine: ScapesEngine,
                 buffer: ByteBuffer,
                 private val checksum: Checksum) {
    private val texture: Resource<Texture>
    private var unusedTicks = 0

    init {
        texture = Resource(engine.graphics.createTexture(64, 64, buffer))
    }

    fun setImage(buffer: ByteBuffer) {
        texture.get().setBuffer(buffer)
    }

    fun bind(gl: GL) {
        unusedTicks = 0
        texture.get().bind(gl)
    }

    fun texture(): Resource<Texture> {
        return texture
    }

    fun checksum(): Checksum {
        return checksum
    }

    fun increaseTicks(): Int {
        return unusedTicks++
    }
}
