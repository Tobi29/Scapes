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

package org.tobi29.scapes.chunk

import org.tobi29.math.vector.Vector3d
import org.tobi29.io.tag.TagMap

interface EnvironmentClient {
    val type: EnvironmentType

    fun read(map: TagMap)

    fun tick(delta: Double)

    fun sunLightReduction(x: Double,
                          y: Double): Float

    fun sunLightNormal(x: Double,
                       y: Double): Vector3d

    fun createSkybox(world: WorldClient): WorldSkybox

    companion object {
        fun make(world: WorldClient,
                 id: Int) = Environment.of(world.registry, id).createClient(
                world)
    }
}
