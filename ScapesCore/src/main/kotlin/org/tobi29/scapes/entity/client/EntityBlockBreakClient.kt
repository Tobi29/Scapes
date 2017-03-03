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

package org.tobi29.scapes.entity.client

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.toDouble
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.entity.model.EntityModelBlockBreak

class EntityBlockBreakClient(world: WorldClient,
                             pos: Vector3d = Vector3d.ZERO) : EntityClient(
        "core.entity.BlockBreak", world, pos) {
    private var progress = 0.0

    override fun read(map: TagMap) {
        super.read(map)
        map["Progress"]?.toDouble()?.let { progress = it }
    }

    override fun createModel(): EntityModel? {
        return EntityModelBlockBreak(world.game.modelBlockBreakShared(), this)
    }

    fun progress(): Double {
        return progress
    }
}
