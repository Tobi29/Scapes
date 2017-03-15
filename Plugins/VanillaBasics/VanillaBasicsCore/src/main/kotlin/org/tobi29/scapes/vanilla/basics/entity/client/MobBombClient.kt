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
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.toDouble
import org.tobi29.scapes.engine.utils.io.tag.toMap
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.entity.model.MobModelBlock
import org.tobi29.scapes.vanilla.basics.material.BlockExplosive

class MobBombClient(type: EntityType<*, *>,
                    world: WorldClient) : MobClient(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)) {
    private val item = ItemStack(world.plugins)
    private var time = 0.0
    private var exploded = false

    override fun read(map: TagMap) {
        super.read(map)
        map["Block"]?.toMap()?.let { item.read(it) }
        map["Time"]?.toDouble()?.let { time = it }
    }

    override fun createModel(): MobModelBlock? {
        return MobModelBlock(this, item)
    }

    override fun update(delta: Double) {
        time -= delta
        if (time < 0.05 && !exploded) {
            // TODO: Replace with proper packet
            (item.material() as BlockExplosive).explodeClient(world, pos.now(),
                    speed.now())
            exploded = true
        }
    }
}
