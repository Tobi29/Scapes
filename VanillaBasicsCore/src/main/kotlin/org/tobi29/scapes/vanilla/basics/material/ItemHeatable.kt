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

import org.tobi29.scapes.block.ItemTypeDataI
import org.tobi29.scapes.block.copy
import org.tobi29.scapes.inventory.*
import org.tobi29.io.tag.Tag
import org.tobi29.io.tag.toDouble
import org.tobi29.io.tag.toTag

typealias ItemHeatable = TypedItem<ItemTypeHeatable>
typealias ItemTypeHeatable = ItemTypeHeatableI<*>

interface ItemTypeHeatableI<I : ItemType> : ItemTypeI<I>,
        ItemTypeStackableI<I>,
        ItemTypeDataI<I> {
    fun heat(item: TypedItem<I>,
             temperature: Double,
             transferFactor: Double = 1.0,
             beforeUpdate: (TypedItem<I>) -> Item? = { it }): Item?

    fun temperature(item: TypedItem<I>): Double {
        return item.metaData["Temperature"]?.toDouble() ?: 0.0
    }
}

inline fun <T : ItemTypeHeatable> TypedItem<T>.heat(
        temperature: Double,
        transferFactor: Double = 1.0,
        noinline beforeUpdate: (TypedItem<T>) -> Item? = { it }
): Item? = @Suppress("UNCHECKED_CAST")
(type as ItemTypeHeatableI<ItemType>).heat(this, temperature, transferFactor,
        beforeUpdate as (TypedItem<ItemType>) -> Item?)

inline val ItemHeatable.temperature: Double
    get() = @Suppress("UNCHECKED_CAST")
    (type as ItemTypeHeatableI<ItemType>).temperature(this)

fun <T : ItemTypeHeatable> TypedItem<T>.copy(
        temperature: Double,
        metaData: Map<String, Tag> = this.metaData
): TypedItem<T> = copy(
        temperature = temperature,
        type = this.type,
        metaData = metaData)

fun <T : ItemTypeHeatable, T2 : ItemTypeHeatable> TypedItem<T>.copy(
        temperature: Double,
        type: T2,
        metaData: Map<String, Tag> = this.metaData
): TypedItem<T2> = copy(
        type = type,
        metaData = (metaData + mapOf("Temperature" to temperature.toTag())))

interface ItemDefaultHeatableI<I : ItemType> : ItemTypeHeatableI<I> {
    fun heatTransferFactor(item: TypedItem<I>): Double

    fun temperatureUpdated(item: TypedItem<I>): Item?

    override fun heat(item: TypedItem<I>,
                      temperature: Double,
                      transferFactor: Double,
                      beforeUpdate: (TypedItem<I>) -> Item?): Item? {
        val currentTemperature = temperature(item)
        val factor = heatTransferFactor(item).toFloat() * transferFactor
        val newTemperature = currentTemperature * (1.0 - factor) + temperature * factor
        val heatedItem = (item as TypedItem<ItemTypeHeatable>).copy(
                temperature = newTemperature) as TypedItem<I>
        val updateItem = beforeUpdate(heatedItem).let {
            it.kind<ItemDefaultHeatableI<*>>() ?: return it
        }
        (updateItem.type as ItemDefaultHeatableI<ItemTypeHeatable>)
                .temperatureUpdated(updateItem)
        return updateItem
    }
}
