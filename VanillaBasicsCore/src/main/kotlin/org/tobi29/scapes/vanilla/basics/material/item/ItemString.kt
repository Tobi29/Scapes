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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType

class ItemString(type: VanillaMaterialType) : ItemSimpleData(type) {
    override fun types(): Int {
        return 2
    }

    override fun texture(data: Int): String {
        when (data) {
            0 -> return "VanillaBasics:image/terrain/other/String.png"
            1 -> return "VanillaBasics:image/terrain/other/Fabric.png"
            else -> throw IllegalArgumentException("Unknown data: {}" + data)
        }
    }

    override fun name(item: ItemStack): String {
        when (item.data()) {
            0 -> return "String"
            1 -> return "Fabric"
            else -> throw IllegalArgumentException(
                    "Unknown data: {}" + item.data())
        }
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 32
    }
}
