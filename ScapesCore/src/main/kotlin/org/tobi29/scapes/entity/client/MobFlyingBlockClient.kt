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

package org.tobi29.scapes.entity.client

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.model.MobModelBlock

class MobFlyingBlockClient(world: WorldClient, pos: Vector3d = Vector3d.ZERO, speed: Vector3d = Vector3d.ZERO,
                           type: BlockType = world.air, data: Int = 0) : MobClient(
        world, pos, speed, AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)) {
    private val item: ItemStack

    init {
        item = ItemStack(type, data)
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getStructure("Block")?.let { item.load(it) }
    }

    override fun createModel(): MobModelBlock? {
        return MobModelBlock(this, item)
    }
}
