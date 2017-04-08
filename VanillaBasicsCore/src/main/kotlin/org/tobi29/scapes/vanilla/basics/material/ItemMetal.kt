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

package org.tobi29.scapes.vanilla.basics.material

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.readAlloy
import org.tobi29.scapes.vanilla.basics.util.writeAlloy

interface ItemMetal : ItemDefaultHeatable {
    val plugin: VanillaBasics

    fun alloy(item: ItemStack): Alloy {
        return readAlloy(plugin,
                item.metaData("Vanilla")["Alloy"]?.toMap() ?: TagMap())
    }

    fun setAlloy(item: ItemStack,
                 alloy: Alloy) {
        item.metaData("Vanilla")["Alloy"] = TagMap { writeAlloy(alloy, this) }
    }

    fun meltingPoint(item: ItemStack): Float {
        return alloy(item).meltingPoint().toFloat()
    }

    override fun heatTransferFactor(item: ItemStack) = 0.001
}
