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
import org.tobi29.scapes.engine.utils.Checksum
import org.tobi29.scapes.engine.utils.io.ByteViewRO

class ClientSkin(engine: ScapesEngine,
                 buffer: ByteViewRO,
                 val checksum: Checksum) {
    val texture = engine.graphics.createTexture(64, 64, buffer)
    private var unusedTicks = 0

    fun setImage(buffer: ByteViewRO) {
        texture.setBuffer(buffer)
    }

    fun bind(gl: GL) {
        unusedTicks = 0
        texture.bind(gl)
    }

    fun increaseTicks(): Int {
        return unusedTicks++
    }
}
