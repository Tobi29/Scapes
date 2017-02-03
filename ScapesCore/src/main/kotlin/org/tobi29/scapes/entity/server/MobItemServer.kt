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
package org.tobi29.scapes.entity.server

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.forEach
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.getEntities

class MobItemServer(world: WorldServer,
                    pos: Vector3d = Vector3d.ZERO,
                    speed: Vector3d = Vector3d.ZERO,
                    item: ItemStack = ItemStack(
                            world.registry),
                    private var despawntime: Double = Double.NaN) : MobServer(
        world, pos, speed, AABB(-0.2, -0.2, -0.2, 0.2, 0.2, 0.2)) {
    private val item: ItemStack
    private var pickupwait = 1.0
    private var stackwait = 0.0

    init {
        this.item = ItemStack(item)
        stepHeight = 0.0
    }

    override fun write(): TagStructure {
        val tagStructure = super.write()
        item.save()
        tagStructure.setStructure("Inventory", item.save())
        tagStructure.setDouble("Pickupwait", pickupwait)
        tagStructure.setDouble("Despawntime", despawntime)
        return tagStructure
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getStructure("Inventory")?.let { item.load(it) }
        tagStructure.getDouble("Pickupwait")?.let { pickupwait = it }
        tagStructure.getDouble("Despawntime")?.let { despawntime = it }
    }

    fun item(): ItemStack {
        return item
    }

    override fun update(delta: Double) {
        if (pickupwait <= 0) {
            pickupwait = 0.0
            val aabb = getAABB().grow(0.8, 0.8, 0.4)
            world.players().forEach({ aabb.overlay(it.getAABB()) }) { entity ->
                world.playSound("Scapes:sound/entity/mob/Item.ogg", this)
                entity.inventories().modify("Container") { inventory ->
                    item.setAmount(item.amount() - inventory.add(item))
                }
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
        if (item.amount() <= 0 || item.material() === registry.air()) {
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
