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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.math.AABB
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.entity.client.attachModel
import org.tobi29.scapes.entity.model.MobModelItem

class MobItemClient(type: EntityType<*, *>,
                    world: WorldClient) : MobClient(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.2, -0.2, -0.2, 0.2, 0.2, 0.2)) {
    private val item = ItemStack(world.plugins)

    init {
        attachModel { MobModelItem(this, item) }
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toMap()?.let { item.read(it) }
    }

    fun item(): ItemStack {
        return item
    }
}
