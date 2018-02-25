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

package org.tobi29.scapes.vanilla.basics.entity.server

import org.tobi29.io.tag.ReadWriteTagMap
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toDouble
import org.tobi29.io.tag.toTag
import org.tobi29.math.AABB
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.data
import org.tobi29.scapes.block.toItem
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.inventory.toTag
import org.tobi29.stdex.math.floorToInt
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.set
import kotlin.math.abs

class MobFlyingBlockServer(type: EntityType<*, *>,
                           world: WorldServer) : MobServer(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)) {
    private val item = AtomicReference<Item?>(null)
    private var time = 0.0

    init {
        stepHeight = 0.0
    }

    fun setType(item: Item?) {
        this.item.set(item)
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Block"] = item.get().toTag()
        map["Time"] = time.toTag()
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Block"]?.toItem(world.plugins)?.let { item.set(it) }
        map["Time"]?.toDouble()?.let { time = it }
    }

    override fun update(delta: Double) {
        if (isOnGround) {
            val x = pos.x.floorToInt()
            val y = pos.y.floorToInt()
            val z = pos.z.floorToInt()
            if (!world.terrain.type(x, y, z).isReplaceable(world.terrain, x, y,
                    z)) {
                var xx = pos.x.floorToInt() + 0.5
                var yy = pos.y.floorToInt() + 0.5
                if (pos.x == xx && pos.y == yy) {
                    val random = threadLocalRandom()
                    xx += random.nextDouble() * delta - delta
                    yy += random.nextDouble() * delta - delta
                }
                push(pos.x - xx, pos.y - yy, 0.0)
                time += delta
                if (time >= 5.0) {
                    pos.addZ(1.0)
                    speed.setZ(1.0)
                }
            } else if (abs(speed.x) < 0.1 && abs(
                    speed.y) < 0.1) {
                world.terrain.modify(x, y, z) { handler ->
                    val item = item.get().kind<BlockType>() ?: return@modify
                    handler.typeData(x, y, z, item.type, item.data)
                }
                world.removeEntity(this)
            }
        }
    }
}
