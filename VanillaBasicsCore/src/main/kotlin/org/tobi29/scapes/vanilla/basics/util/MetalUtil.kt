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

import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.engine.utils.reduceOrNull
import org.tobi29.scapes.engine.utils.tag.ReadWriteTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.AlloyType
import org.tobi29.scapes.vanilla.basics.material.MetalType

fun readAlloy(plugin: VanillaBasics,
              map: TagMap): Alloy {
    val alloy = Alloy()
    map.asSequence().forEach { (key, value) ->
        value.toDouble()?.let { number ->
            alloy.add(plugin.metalType(key) ?: plugin.crapMetal, number)
        }
    }
    return alloy
}

fun writeAlloy(alloy: Alloy,
               map: ReadWriteTagMap) {
    alloy.metals.forEach { (key, value) -> map[key.id] = value }
}

class Alloy {
    private val metalsMut = ConcurrentHashMap<MetalType, Double>()
    val metals = metalsMut.readOnly()

    fun add(metal: MetalType,
            amount: Double = 1.0) {
        var containing: Double? = metalsMut[metal]
        if (containing == null) {
            containing = amount
        } else {
            containing += amount
        }
        metalsMut.put(metal, containing)
    }

    fun drain(metal: MetalType,
              maxAmount: Double): Double {
        var containing = metalsMut[metal] ?: 0.0
        val drain: Double
        if (containing <= maxAmount) {
            drain = containing
            containing = 0.0
        } else {
            drain = maxAmount
            containing -= maxAmount
        }
        if (containing < 0.00001) {
            metalsMut.remove(metal)
        } else {
            metalsMut.put(metal, containing)
        }
        return drain
    }

    fun drain(maxAmount: Double): Alloy {
        val amount = amount()
        val alloy = Alloy()
        for ((key, value) in metalsMut) {
            val drain = value / amount * maxAmount
            alloy.add(key, drain(key, drain))
        }
        return alloy
    }

    fun type(plugin: VanillaBasics): AlloyType {
        val amount = amount()
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


    @Suppress("NOTHING_TO_INLINE")
    private inline fun offset(amount: Double,
                              alloyType: AlloyType): Double? {
        var offset = 0.0
        for ((key, value) in metalsMut) {
            val required = alloyType.ingredients[key] ?: return null
            offset += abs(value / amount - required)
        }
        return offset
    }

    fun amount(): Double {
        var amount = 0.0
        for ((_, value) in metalsMut) {
            amount += value
        }
        return amount
    }

    fun meltingPoint(): Double {
        var amount = 0.0
        var temperature = 0.0
        for ((key, metal) in metalsMut) {
            amount += metal
            temperature += metal * key.meltingPoint
        }
        return temperature / amount
    }
}
