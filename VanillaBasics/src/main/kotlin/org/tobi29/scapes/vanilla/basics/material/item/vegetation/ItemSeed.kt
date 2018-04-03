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

import org.tobi29.scapes.block.ItemTypeIconKindsI
import org.tobi29.scapes.block.ItemTypeKindsRegistryI
import org.tobi29.scapes.block.ItemTypeUseableI
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.math.Face
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.*
import org.tobi29.scapes.vanilla.basics.entity.server.EntityFarmlandServer
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.ItemResearchI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItemBase

class ItemSeed(
        type: VanillaMaterialType
) : VanillaItemBase<ItemSeed>(type),
        ItemTypeKindsRegistryI<ItemSeed, CropType>,
        ItemTypeNamedI<ItemSeed>,
        ItemTypeIconKindsI<ItemSeed, CropType>,
        ItemTypeStackableDefaultI<ItemSeed>,
        ItemTypeUseableI<ItemSeed>,
        ItemResearchI<ItemSeed> {
    override val registry =
            plugins.registry.get<CropType>("VanillaBasics", kindTag)
    override val kindTag get() = "CropType"

    override fun textureAsset(kind: CropType) =
            "${kind.texture}/Seed"

    override fun name(item: TypedItem<ItemSeed>) = "${kind(item).name} Seeds"

    override fun maxStackSize(item: TypedItem<ItemSeed>) = 128

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<ItemSeed>,
                       terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face): Pair<Item?, Double?> =
            if (face == Face.UP) {
                entity.world.getEntities(x, y,
                        z).filterIsInstance<EntityFarmlandServer>().forEach { farmland ->
                    farmland.seed(materials.plugin.cropTypes.WHEAT)
                }
                item.copy(amount = item.amount - 1).orNull() to 0.0
            } else item to 0.0

    override fun identifiers(item: TypedItem<ItemSeed>): Array<String> {
        return arrayOf("vanilla.basics.item.Seed",
                "vanilla.basics.item.Seed." + kind(item).name)
    }
}
