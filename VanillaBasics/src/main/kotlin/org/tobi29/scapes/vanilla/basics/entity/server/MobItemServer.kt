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
import org.tobi29.math.AABB3
import org.tobi29.math.grow
import org.tobi29.math.overlaps
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.block.toItem
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.isEmpty
import org.tobi29.scapes.inventory.stack
import org.tobi29.scapes.inventory.toTag
import kotlin.collections.set

class MobItemServer(
    type: EntityType<*, *>,
    world: WorldServer
) : MobServer(
    type, world, Vector3d.ZERO, Vector3d.ZERO,
    AABB3(-0.2, -0.2, -0.2, 0.2, 0.2, 0.2)
) {
    var item: Item? = null
    private var pickupwait = 1.0
    private var stackwait = 0.0
    var despawntime = Double.NaN

    init {
        stepHeight = 0.0
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Inventory"] = item.toTag()
        map["Pickupwait"] = pickupwait.toTag()
        map["Despawntime"] = despawntime.toTag()
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toItem(world.plugins)?.let { item = it }
        map["Pickupwait"]?.toDouble()?.let { pickupwait = it }
        map["Despawntime"]?.toDouble()?.let { despawntime = it }
    }

    override fun update(delta: Double) {
        if (pickupwait <= 0) {
            pickupwait = 0.0
            val aabb = currentAABB().apply { grow(0.8, 0.8, 0.4) }
            world.players().asSequence().filter {
                aabb overlaps it.currentAABB()
            }.forEach { entity ->
                world.playSound("Scapes:sound/entity/mob/Item.ogg", this)
                entity.inventories.modify("Container") { item = it.add(item) }
            }
            stackwait -= delta
            if (stackwait <= 0) {
                stackwait += 1.0
                world.getEntities(
                    pos.now(),
                    1.0
                ).filterIsInstance<MobItemServer>()
                    .filter { it != this && !item.isEmpty() }.forEach {
                        val (new, remaining) = it.item.stack(item)
                        item = remaining
                        it.item = new
                    }
            }
        } else {
            pickupwait -= delta
        }
        if (item.isEmpty()) world.removeEntity(this)
        if (!despawntime.isNaN()) {
            despawntime -= delta
            if (despawntime <= 0) {
                world.removeEntity(this)
            }
        }
    }
}
