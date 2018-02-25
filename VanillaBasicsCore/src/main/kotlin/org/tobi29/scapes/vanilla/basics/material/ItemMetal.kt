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

import org.tobi29.scapes.block.copy
import org.tobi29.scapes.block.data
import org.tobi29.utils.substitute
import org.tobi29.scapes.inventory.ItemType
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.amount
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.meltingPoint
import org.tobi29.scapes.vanilla.basics.util.toAlloy
import org.tobi29.io.tag.Tag
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toTag

typealias ItemMetal = ItemMetalI<*>

interface ItemMetalI<I : ItemType> : ItemDefaultHeatableI<I> {
    val plugin: VanillaBasics

    fun alloy(item: TypedItem<I>): Alloy =
            item.metaData["Alloy"]?.toAlloy(plugin) ?: Alloy()

    fun alloy(item: TypedItem<I>,
              alloy: Alloy): TypedItem<I> =
            item.copy(metaData = (item.metaData +
                    ("Alloy" to alloy.toTag())).toTag())

    override fun heatTransferFactor(item: TypedItem<I>) = 0.001
}

inline val TypedItem<ItemMetal>.alloy: Alloy
    get() = @Suppress("UNCHECKED_CAST")
    (type as ItemMetalI<ItemTypeHeatable>).alloy(this)

inline val TypedItem<ItemMetal>.meltingPoint: Double
    get() = alloy.meltingPoint

@Suppress("UNCHECKED_CAST")
fun <T : ItemMetal> TypedItem<T>.copy(
        alloy: Alloy = this.alloy
): TypedItem<T> =
        (type as ItemMetalI<ItemTypeHeatable>).alloy(this,
                alloy) as TypedItem<T>

fun <T : ItemMetal> TypedItem<T>.copy(
        alloy: Alloy,
        temperature: Double? = null,
        data: Int = this.data,
        amount: Int = this.amount,
        metaData: Map<String, Tag> = this.metaData
): TypedItem<T> = copy(
        alloy = alloy,
        temperature = temperature,
        type = type,
        data = data,
        amount = amount,
        metaData = metaData)

fun <T : ItemMetal> TypedItem<ItemMetal>.copy(
        alloy: Alloy,
        temperature: Double? = null,
        type: T,
        data: Int = this.data,
        amount: Int = this.amount,
        metaData: Map<String, Tag> = this.metaData
): TypedItem<T> = copy(
        type = type,
        data = data,
        amount = amount,
        metaData = metaData.substitute("Vanilla") {
            ((it as? TagMap ?: emptyMap<String, Tag>()))
                    .let {
                        if (temperature == null) it
                        else it + ("Temperature" to temperature.toTag())
                    }.toTag()
        }).copy(alloy)
