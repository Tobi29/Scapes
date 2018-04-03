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
import org.tobi29.scapes.inventory.ItemTypeNamedI
import org.tobi29.scapes.inventory.ItemTypeStackableDefaultI
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.ItemTypeFuelI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType

class ItemCoal(
        type: VanillaMaterialType
) : VanillaItemBase<ItemCoal>(type),
        ItemTypeIconI<ItemCoal>,
        ItemTypeNamedI<ItemCoal>,
        ItemTypeStackableDefaultI<ItemCoal>,
        ItemTypeFuelI<ItemCoal> {
    override val textureAsset
        get() = "VanillaBasics:image/terrain/ore/coal/Coal"

    override fun name(item: TypedItem<ItemCoal>) = "Coal"

    override fun maxStackSize(item: TypedItem<ItemCoal>) = 128

    override fun fuelTemperature(item: TypedItem<ItemCoal>) = 0.8

    override fun fuelTime(item: TypedItem<ItemCoal>) = 20.0

    override fun fuelTier(item: TypedItem<ItemCoal>) = 50
}
