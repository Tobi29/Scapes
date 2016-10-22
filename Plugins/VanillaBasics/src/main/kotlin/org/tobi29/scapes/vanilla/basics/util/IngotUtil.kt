/*
 * Copyright 2012-2016 Tobi29
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
import org.tobi29.scapes.engine.utils.io.tag.setFloat
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.MetalType
import org.tobi29.scapes.vanilla.basics.material.item.ItemMetal

fun createIngot(plugin: VanillaBasics,
                item: ItemStack,
                metalType: String,
                temperature: Float) {
    createIngot(item, plugin.metalType(metalType), temperature)
}

fun createIngot(item: ItemStack,
                metalType: MetalType,
                temperature: Float) {
    val alloy = Alloy()
    alloy.add(metalType, 1.0)
    (item.material() as ItemMetal).setAlloy(item, alloy)
    item.metaData("Vanilla").setFloat("Temperature", temperature)
}
