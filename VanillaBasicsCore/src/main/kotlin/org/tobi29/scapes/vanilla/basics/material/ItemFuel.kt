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

import org.tobi29.scapes.inventory.ItemType
import org.tobi29.scapes.inventory.ItemTypeI
import org.tobi29.scapes.inventory.TypedItem

typealias ItemFuel = TypedItem<ItemTypeFuel>
typealias ItemTypeFuel = ItemTypeFuelI<*>

interface ItemTypeFuelI<in I : ItemType> : ItemTypeI<I> {
    fun fuelTemperature(item: TypedItem<I>): Double

    fun fuelTime(item: TypedItem<I>): Double

    fun fuelTier(item: TypedItem<I>): Int
}

inline val ItemFuel.fuelTemperature: Double
    get() = @Suppress("UNCHECKED_CAST")
    (type as ItemTypeFuelI<ItemType>).fuelTemperature(this)

inline val ItemFuel.fuelTime: Double
    get() = @Suppress("UNCHECKED_CAST")
    (type as ItemTypeFuelI<ItemType>).fuelTime(this)

inline val ItemFuel.fuelTier: Int
    get() = @Suppress("UNCHECKED_CAST")
    (type as ItemTypeFuelI<ItemType>).fuelTier(this)
