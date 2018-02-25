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
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType

class ItemString(
        type: VanillaMaterialType
) : VanillaItemBase<ItemString>(type),
        ItemTypeNamedI<ItemString>,
        ItemTypeIconI<ItemString>,
        ItemTypeStackableDefaultI<ItemString> {
    override val textureAsset
        get() = "VanillaBasics:image/terrain/other/String"

    override fun name(item: TypedItem<ItemString>) = "String"

    override fun maxStackSize(item: TypedItem<ItemString>) = 8
}
