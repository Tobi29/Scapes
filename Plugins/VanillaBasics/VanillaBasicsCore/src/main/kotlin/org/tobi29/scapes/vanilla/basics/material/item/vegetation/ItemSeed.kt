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
package org.tobi29.scapes.vanilla.basics.material.item.vegetation

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.entity.server.EntityFarmlandServer
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.item.ItemSimpleData
import java.util.concurrent.ThreadLocalRandom

class ItemSeed(materials: VanillaMaterial,
               private val cropRegistry: GameRegistry.Registry<CropType>) : ItemSimpleData(
        materials, "vanilla.basics.item.Seed") {
    override fun click(entity: MobPlayerServer,
                       item: ItemStack,
                       terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face): Double {
        if (face == Face.UP) {
            item.setAmount(item.amount() - 1)
            val random = ThreadLocalRandom.current()
            if (random.nextInt(1) == 0) {
                terrain.world.getEntities(x, y,
                        z).filterMap<EntityFarmlandServer>().forEach { farmland ->
                    farmland.seed(materials.plugin.cropTypes.WHEAT)
                }
            }
        }
        return 0.0
    }

    override fun types(): Int {
        return cropRegistry.values().size
    }

    override fun texture(data: Int): String {
        return "${cropRegistry[data].texture}/Seed.png"
    }

    override fun name(item: ItemStack): String {
        return materials.crop.name(item) + " Seeds"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 128
    }
}
