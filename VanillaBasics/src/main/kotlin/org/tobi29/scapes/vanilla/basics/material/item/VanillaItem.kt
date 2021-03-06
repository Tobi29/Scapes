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

import org.tobi29.scapes.block.*
import org.tobi29.scapes.inventory.ItemTypeI
import org.tobi29.scapes.inventory.ItemTypeNamedI
import org.tobi29.scapes.inventory.ItemTypeStackableDefaultI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.utils.ComponentStorage

abstract class VanillaItem(
        type: VanillaMaterialType
) : VanillaItemBase<VanillaItem>(type),
        ItemTypeNamedI<VanillaItem>,
        ItemTypeUseableI<VanillaItem>,
        ItemTypeStackableDefaultI<VanillaItem>,
        ItemTypeToolI<VanillaItem>,
        ItemTypeWeaponI<VanillaItem>,
        ItemTypeLightI<VanillaItem>,
        ItemTypeDataI<VanillaItem>,
        ItemTypeModelI<VanillaItem>,
        ItemTypeTexturedI<VanillaItem>

abstract class VanillaItemBase<in I>(type: VanillaMaterialType) : ItemTypeI<I> {
    override val componentStorage = ComponentStorage<Any>()
    val plugins = type.plugins
    override val id = type.id
    override val nameID = type.name

    val materials = type.materials
    val plugin = materials.plugin
}
