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

package scapes.plugin.tobi29.vanilla.basics.entity.server

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.server.MobServer
import scapes.plugin.tobi29.vanilla.basics.material.block.BlockExplosive

class MobBombServer(world: WorldServer, pos: Vector3d = Vector3d.ZERO, speed: Vector3d = Vector3d.ZERO,
                    type: BlockType = world.air, data: Int = 0, private var time: Double = 0.0) : MobServer(
        world, pos, speed, AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)) {
    private val item: ItemStack

    init {
        item = ItemStack(type, data)
        stepHeight = 0.0
    }

    override fun write(): TagStructure {
        val tag = super.write()
        tag.setStructure("Block", item.save())
        tag.setDouble("Time", time)
        return tag
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getStructure("Block")?.let { item.load(it) }
        tagStructure.getDouble("Time")?.let { time = it }
    }

    override fun update(delta: Double) {
        time -= delta
        if (time <= 0.0) {
            world.playSound("Scapes:sound/entity/mob/Explosion1.ogg", this,
                    1.0f, 64.0f)
            world.removeEntity(this)
            val x = pos.intX()
            val y = pos.intY()
            val z = pos.intZ()
            (item.material() as BlockExplosive).explode(world.terrain, x, y, z)
        }
    }
}
