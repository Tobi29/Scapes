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

import org.tobi29.scapes.engine.math.AABB

class AABBElement(val aabb: AABB = AABB(0.0, 0.0, 0.0, 0.0, 0.0,
        0.0),
                  var collision: Collision = BlockType.STANDARD_COLLISION) {

    fun set(minX: Double,
            minY: Double,
            minZ: Double,
            maxX: Double,
            maxY: Double,
            maxZ: Double,
            collision: Collision = BlockType.STANDARD_COLLISION) {
        aabb.minX = minX
        aabb.minY = minY
        aabb.minZ = minZ
        aabb.maxX = maxX
        aabb.maxY = maxY
        aabb.maxZ = maxZ
        this.collision = collision
    }

    val isSolid: Boolean
        get() = collision.isSolid

    fun aabb(): AABB {
        return aabb
    }
}
