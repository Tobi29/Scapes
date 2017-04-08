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

package org.tobi29.scapes.vanilla.basics.util

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.vanilla.basics.material.AlloyType
import org.tobi29.scapes.vanilla.basics.material.ItemMetal
import org.tobi29.scapes.vanilla.basics.material.MetalType
import org.tobi29.scapes.vanilla.basics.material.add

fun createIngot(item: ItemStack,
                metalType: MetalType) {
    val alloy = Alloy()
    alloy.add(metalType)
    (item.material() as ItemMetal).setAlloy(item, alloy)
}

fun createIngot(item: ItemStack,
                alloyType: AlloyType) {
    val alloy = Alloy()
    alloy.add(alloyType)
    (item.material() as ItemMetal).setAlloy(item, alloy)
}
