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
package org.tobi29.scapes.entity

import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import java.util.*

interface Entity {
    fun getUUID(): UUID

    fun getCurrentPos(): Vector3d

    fun getAABB(): AABB {
        val aabb = AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)
        val pos = getCurrentPos()
        aabb.add(pos.x, pos.y, pos.z)
        return aabb
    }
}