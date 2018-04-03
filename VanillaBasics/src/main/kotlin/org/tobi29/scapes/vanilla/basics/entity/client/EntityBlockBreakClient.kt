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

package org.tobi29.scapes.vanilla.basics.entity.client

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.EntityAbstractClient
import org.tobi29.scapes.entity.client.attachModel
import org.tobi29.scapes.vanilla.basics.entity.model.EntityModelBlockBreak
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toDouble

class EntityBlockBreakClient(type: EntityType<*, *>,
                             world: WorldClient) : EntityAbstractClient(
        type, world, Vector3d.ZERO) {
    private var progress = 0.0

    init {
        attachModel {
            EntityModelBlockBreak(world.game.modelBlockBreakShared().await(),
                    this)
        }
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Progress"]?.toDouble()?.let { progress = it }
    }

    fun progress(): Double {
        return progress
    }
}
