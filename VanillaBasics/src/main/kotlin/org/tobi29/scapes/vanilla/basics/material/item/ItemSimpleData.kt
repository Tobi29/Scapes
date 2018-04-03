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

import org.tobi29.scapes.block.ItemTypeIconKindsI
import org.tobi29.scapes.block.copy
import org.tobi29.scapes.inventory.ItemTypeKindsI
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType

abstract class ItemSimpleData(type: VanillaMaterialType) : VanillaItem(type),
        ItemTypeKindsI<VanillaItem, Int>,
        ItemTypeIconKindsI<VanillaItem, Int> {
    protected abstract fun types(): Int

    protected abstract fun texture(data: Int): String?

    override val kinds: Set<Int> = HashSet<Int>().apply {
        repeat(types()) { add(it) }
    }

    override fun kind(item: TypedItem<VanillaItem>): Int =
            data(item).let { if (it < types()) it else 0 }

    override fun kind(item: TypedItem<VanillaItem>,
                      value: Int): TypedItem<VanillaItem> =
            item.copy(data = value)

    override fun textureAsset(kind: Int): String = texture(kind)
            ?.removeSuffix(".png")
            ?: throw IllegalArgumentException("Invalid kind")
}
