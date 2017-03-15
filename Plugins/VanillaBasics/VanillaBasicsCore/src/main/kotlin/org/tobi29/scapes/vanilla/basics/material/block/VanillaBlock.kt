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

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.entity.server.EntityBlockBreakServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.util.dropItems

abstract class VanillaBlock(type: VanillaMaterialType) : BlockType(type.type) {
    val materials = type.materials
    val plugin = materials.plugin

    override fun destroy(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: ItemStack): Boolean {
        val drops = drops(item, data)
        terrain.world.dropItems(drops, x, y, z)
        return true
    }

    override fun punch(terrain: TerrainServer.TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       data: Int,
                       face: Face,
                       player: MobPlayerServer,
                       item: ItemStack,
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
                    .filterMap<EntityBlockBreakServer>()
                    .firstOrNull() ?: run {
                val entityBreak = materials.plugin.entityTypes.blockBreak.createServer(
                        terrain.world).apply {
                    setPos(Vector3d(x + 0.5, y + 0.5, z + 0.5))
                }
                terrain.world.addEntityNew(entityBreak)
                entityBreak
            }
            if (entityBreak.punch(terrain.world, punch)) {
                if (destroy(terrain, x, y, z, data, face, player, item)) {
                    terrain.typeData(x, y, z, terrain.air, 0)
                }
            }
        }
    }
}
