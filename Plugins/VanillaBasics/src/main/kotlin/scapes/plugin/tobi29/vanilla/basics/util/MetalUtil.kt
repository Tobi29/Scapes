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

package scapes.plugin.tobi29.vanilla.basics.util

import org.tobi29.scapes.engine.utils.forEach
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.engine.utils.reduceOrNull
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.material.AlloyType
import scapes.plugin.tobi29.vanilla.basics.material.MetalType
import java.util.concurrent.ConcurrentHashMap

fun read(plugin: VanillaBasics,
         tagStructure: TagStructure): Alloy {
    val alloy = Alloy()
    tagStructure.tagEntrySet.forEach({ it.value is Number }) {
        alloy.add(plugin.metalType(it.key), it.value as Double)
    }
    return alloy
}

fun write(alloy: Alloy): TagStructure {
    val tagStructure = TagStructure()
    alloy.metals.forEach { entry ->
        tagStructure.setDouble(entry.key.id(), entry.value)
    }
    return tagStructure
}

class Alloy {
    private val metalsMut = ConcurrentHashMap<MetalType, Double>()
    val metals = metalsMut.readOnly()

    fun add(metal: MetalType,
            amount: Double) {
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
            val required = alloyType.ingredients()[key] ?: return null
            offset += abs(value / amount - required)
        }
        return offset
    }

    fun amount(): Double {
        var amount = 0.0
        for ((key, value) in metalsMut) {
            amount += value
        }
        return amount
    }

    fun meltingPoint(): Double {
        var amount = 0.0
        var temperature = 0.0
        for ((key, metal) in metalsMut) {
            amount += metal
            temperature += metal * key.meltingPoint()
        }
        return temperature / amount
    }
}