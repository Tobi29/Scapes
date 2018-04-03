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

import org.tobi29.utils.reduceOrNull
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.AlloyType
import org.tobi29.scapes.vanilla.basics.material.MetalType
import org.tobi29.io.tag.*
import org.tobi29.stdex.computeAlways
import kotlin.collections.Map
import kotlin.collections.asSequence
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.emptyMap
import kotlin.collections.iterator
import kotlin.collections.mapKeys
import kotlin.collections.mapValues
import kotlin.collections.set
import kotlin.collections.sum
import kotlin.collections.toMap
import kotlin.collections.toMutableMap
import kotlin.math.abs

class Alloy(
        map: Map<MetalType, Double> = emptyMap()
) : Map<MetalType, Double> by map.toMap(),
        TagMapWrite {
    override fun write(map: ReadWriteTagMap) {
        for ((key, value) in this) {
            map[key.id] = value.toTag()
        }
    }
}

fun MutableTag.toAlloy(plugin: VanillaBasics): Alloy? {
    val map = toMap() ?: return null
    val metals = map.mapKeys {
        plugin.metalType(it.key) ?: return null
    }.mapValues { it.value.toDouble() ?: return null }
    return Alloy(metals)
}

val Map<MetalType, Double>.amount: Double get() = values.sum()

val Map<MetalType, Double>.meltingPoint: Double
    get() {
        var amount = 0.0
        var temperature = 0.0
        for ((key, metal) in this) {
            amount += metal
            temperature += metal * key.meltingPoint
        }
        return temperature / amount
    }

fun Map<MetalType, Double>.normalizeSafe() =
        amount.let { amount ->
            if (amount <= 0.0) Alloy()
            else this / amount
        }

fun Map<MetalType, Double>.split(ratio: Double): Pair<Alloy, Alloy> =
        (this * ratio) to (this * (1.0 - ratio))

operator fun Map<MetalType, Double>.plus(other: Map<MetalType, Double>): Alloy {
    val map = toMutableMap()
    for ((metal, amount) in other) {
        map.computeAlways(metal) { _, it -> (it ?: 0.0) + amount }
    }
    return Alloy(map)
}

operator fun Map<MetalType, Double>.times(factor: Double): Alloy =
        Alloy(mapValues { it.value * factor })

operator fun Map<MetalType, Double>.div(divisor: Double): Alloy =
        Alloy(mapValues { it.value * divisor })

fun Map<MetalType, Double>.drain(amount: Double): Pair<Alloy?, Alloy> =
        this.amount.let { current ->
            if (current < amount - 1.0e-10) null to Alloy(this)
            else split(amount / current).let {
                it.first.normalizeSafe() * amount to it.second
            }
        }

fun Map<MetalType, Double>.type(plugin: VanillaBasics): AlloyType {
    val amount = amount
    return plugin.alloyTypes.values.asSequence().mapNotNull { alloyType ->
        val offset = offset(amount, alloyType) ?: return@mapNotNull null
        Pair(alloyType, offset)
    }.reduceOrNull { first, second ->
        if (first.second <= second.second) {
            first
        } else {
            second
        }
    }?.first ?: plugin.crapAlloy
}

private fun Map<MetalType, Double>.offset(
        amount: Double,
        alloyType: AlloyType
): Double? {
    var offset = 0.0
    for ((key, value) in this) {
        val required = alloyType.ingredients[key] ?: return null
        offset += abs(value / amount - required)
    }
    return offset
}
