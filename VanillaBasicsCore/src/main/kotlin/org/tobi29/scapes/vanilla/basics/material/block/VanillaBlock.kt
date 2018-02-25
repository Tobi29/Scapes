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

package org.tobi29.scapes.vanilla.basics.material.block

import org.tobi29.math.Face
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.Vector3i
import org.tobi29.math.vector.plus
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStackData
import org.tobi29.scapes.block.data
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.amount
import org.tobi29.scapes.inventory.copy
import org.tobi29.scapes.vanilla.basics.entity.server.EntityBlockBreakServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.util.dropItems

abstract class VanillaBlock(type: VanillaMaterialType) : BlockType(type.type) {
    val materials = type.materials
    val plugin = materials.plugin

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<BlockType>,
                       terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face): Pair<Item?, Double?> =
            (face.delta + Vector3i(x, y, z)).let { place ->
                terrain.modify(place.x - 1, place.y - 1, place.z - 1, 3, 3,
                        3) { terrain ->
                    if (terrain.type(place.x, place.y, place.z).isReplaceable(
                            terrain, place.x, place.y, place.z)) {
                        val aabbs = collision(item.data, place.x, place.y,
                                place.z)
                        var flag = true
                        val coll = entity.getAABB()
                        for (element in aabbs) {
                            if (coll.overlay(
                                    element.aabb) && element.collision.isSolid) {
                                flag = false
                            }
                        }
                        if (flag) {
                            entity.inventories.modify("Container") {
                                terrain.data(place.x, place.y, place.z,
                                        item.data)
                                if (place(terrain, place.x, place.y, place.z,
                                        face, entity)) {
                                    terrain.type(place.x, place.y, place.z,
                                            this)
                                    item.copy(
                                            amount = item.amount - 1).orNull() to 0.0
                                } else item to 0.0
                            }
                        } else item to 0.0
                    } else item to 0.0
                }
            }

    open fun place(terrain: TerrainMutableServer,
                   x: Int,
                   y: Int,
                   z: Int,
                   face: Face,
                   player: MobPlayerServer): Boolean {
        return true
    }

    open fun destroy(terrain: TerrainMutableServer,
                     x: Int,
                     y: Int,
                     z: Int,
                     data: Int,
                     face: Face,
                     player: MobPlayerServer,
                     item: Item?): Boolean {
        player.world.dropItems(drops(item, data), x, y, z)
        return true
    }

    open fun drops(item: Item?,
                   data: Int): List<Item> {
        return listOf(ItemStackData(this, data))
    }

    override fun punch(terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       data: Int,
                       face: Face,
                       player: MobPlayerServer,
                       item: Item?,
                       br: Double,
                       strength: Double) {
        val punch = br / resistance(item, data) * strength * strength
        if (punch > 0) {
            breakSound(item, data)?.let {
                terrain.world.playSound(it,
                        Vector3d(x + 0.5, y + 0.5, z + 0.5),
                        Vector3d.ZERO)
            }
            val entityBreak = terrain.getEntities(x, y, z)
                    .filterIsInstance<EntityBlockBreakServer>()
                    .firstOrNull() ?: run {
                val entityBreak = materials.plugin.entityTypes.blockBreak.createServer(
                        terrain.world).apply {
                    setPos(Vector3d(x + 0.5, y + 0.5, z + 0.5))
                }
                terrain.world.addEntityNew(entityBreak)
                entityBreak
            }
            if (entityBreak.punch(terrain.world, punch)) {
                terrain.modify(x, y, z) { terrain ->
                    if (destroy(terrain, x, y, z, data, face, player, item)) {
                        terrain.typeData(x, y, z, terrain.air, 0)
                    }
                }
            }
        }
    }
}
