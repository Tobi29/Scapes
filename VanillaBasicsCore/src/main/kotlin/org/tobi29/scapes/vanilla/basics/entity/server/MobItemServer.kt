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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.MobServer

class MobItemServer(type: EntityType<*, *>,
                    world: WorldServer) : MobServer(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.2, -0.2, -0.2, 0.2, 0.2, 0.2)) {
    val item = ItemStack(world.plugins)
    private var pickupwait = 1.0
    private var stackwait = 0.0
    var despawntime = Double.NaN

    init {
        stepHeight = 0.0
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Inventory"] = TagMap { item.write(this) }
        map["Pickupwait"] = pickupwait
        map["Despawntime"] = despawntime
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toMap()?.let { item.read(it) }
        map["Pickupwait"]?.toDouble()?.let { pickupwait = it }
        map["Despawntime"]?.toDouble()?.let { despawntime = it }
    }

    override fun update(delta: Double) {
        if (pickupwait <= 0) {
            pickupwait = 0.0
            val aabb = getAABB().grow(0.8, 0.8, 0.4)
            world.players().asSequence().filter {
                aabb.overlay(it.getAABB())
            }.forEach { entity ->
                world.playSound("Scapes:sound/entity/mob/Item.ogg", this)
                entity.inventories().modify("Container") { it.add(item) }
            }
            stackwait -= delta
            if (stackwait <= 0) {
                stackwait += 1.0
                world.getEntities(pos.now(),
                        1.0).filterMap<MobItemServer>().filter { it != this }.forEach {
                    item.setAmount(item.amount() - it.item.stack(item))
                }
            }
        } else {
            pickupwait -= delta
        }
        if (item.amount() <= 0 || item.material() == world.plugins.air) {
            world.removeEntity(this)
        }
        if (!despawntime.isNaN()) {
            despawntime -= delta
            if (despawntime <= 0) {
                world.removeEntity(this)
            }
        }
    }
}
