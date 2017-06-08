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
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.tag.toTag
import kotlin.collections.set

interface ItemHeatable {
    fun heat(item: ItemStack,
             temperature: Double,
             transferFactor: Double = 1.0,
             beforeUpdate: (ItemStack) -> Unit = {})

    fun temperature(item: ItemStack): Double {
        return item.metaData("Vanilla")["Temperature"]?.toDouble() ?: 0.0
    }
}

interface ItemDefaultHeatable : ItemHeatable {
    fun heatTransferFactor(item: ItemStack): Double

    fun temperatureUpdated(item: ItemStack)

    override fun heat(item: ItemStack,
                      temperature: Double,
                      transferFactor: Double,
                      beforeUpdate: (ItemStack) -> Unit) {
        val currentTemperature = temperature(item)
        val factor = heatTransferFactor(item).toFloat() * transferFactor
        val newTemperature = currentTemperature * (1.0 - factor) + temperature * factor
        if (newTemperature < 1.0 && newTemperature < currentTemperature) {
            item.metaData("Vanilla")["Temperature"] = 0.0.toTag()
        } else {
            item.metaData("Vanilla")["Temperature"] = newTemperature.toTag()
        }
        beforeUpdate(item)
        temperatureUpdated(item)
    }
}
