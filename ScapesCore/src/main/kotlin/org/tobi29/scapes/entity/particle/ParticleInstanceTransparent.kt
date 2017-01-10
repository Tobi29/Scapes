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
package org.tobi29.scapes.entity.particle

import org.tobi29.scapes.engine.utils.graphics.marginX
import org.tobi29.scapes.engine.utils.graphics.marginY
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d

class ParticleInstanceTransparent : ParticleInstance() {
    val textureOffset = MutableVector2d()
    val textureSize = MutableVector2d()
    var sizeStart = 0.0f
    var sizeEnd = 0.0f
    var gravitationMultiplier = 0.0f
    var airFriction = 0.0f
    var groundFriction = 0.0f
    var waterFriction = 0.0f
    var dir = 0.0f
    var rStart = 0.0f
    var gStart = 0.0f
    var bStart = 0.0f
    var aStart = 0.0f
    var rEnd = 0.0f
    var gEnd = 0.0f
    var bEnd = 0.0f
    var aEnd = 0.0f
    var physics = false
    var timeMax = 0.0f
    var posRender = Vector3d.ZERO

    fun disablePhysics() {
        physics = false
    }

    fun setPhysics(gravitationMultiplier: Float = 1.0f,
                   airFriction: Float = 0.2f,
                   groundFriction: Float = 0.4f,
                   waterFriction: Float = 8.0f) {
        physics = true
        this.gravitationMultiplier = gravitationMultiplier
        this.airFriction = airFriction
        this.groundFriction = groundFriction
        this.waterFriction = waterFriction
    }

    fun setTexture(texture: ParticleTransparentTexture,
                   tMinX: Double = 0.0,
                   tMinY: Double = 0.0,
                   tMaxX: Double = 1.0,
                   tMaxY: Double = 1.0) {
        val texMinX = texture.marginX(tMinX)
        val texMaxX = texture.marginX(tMaxX)
        val texMinY = texture.marginY(tMinY)
        val texMaxY = texture.marginY(tMaxY)
        textureOffset.set(texMinX, texMinY)
        textureSize.set(texMaxX - texMinX, texMaxY - texMinY)
    }
}
