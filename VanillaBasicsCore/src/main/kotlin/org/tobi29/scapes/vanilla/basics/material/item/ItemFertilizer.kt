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

package org.tobi29.scapes.vanilla.basics.material.item

import org.tobi29.scapes.block.ItemTypeIconI
import org.tobi29.scapes.block.ItemTypeUseableI
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.chunk.terrain.modify
import org.tobi29.math.Face
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.ItemTypeNamedI
import org.tobi29.scapes.inventory.ItemTypeStackableDefaultI
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.update.UpdateSaplingGrowth

class ItemFertilizer(
        type: VanillaMaterialType
) : VanillaItemBase<ItemFertilizer>(type),
        ItemTypeNamedI<ItemFertilizer>,
        ItemTypeIconI<ItemFertilizer>,
        ItemTypeStackableDefaultI<ItemFertilizer>,
        ItemTypeUseableI<ItemFertilizer> {
    override val textureAsset
        get() = "VanillaBasics:image/terrain/other/Fertilizer"

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<ItemFertilizer>,
                       terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face): Pair<Item?, Double?> =
            run {
                terrain.modify(materials.sapling, x, y, z) { terrain ->
                    terrain.addDelayedUpdate(
                            UpdateSaplingGrowth(entity.world.registry).set(x, y,
                                    z, 3.0))
                }
                item to 0.0
            }

    override fun name(item: TypedItem<ItemFertilizer>) = "Fertilizer (Debug)"

    override fun maxStackSize(item: TypedItem<ItemFertilizer>) = 64
}
