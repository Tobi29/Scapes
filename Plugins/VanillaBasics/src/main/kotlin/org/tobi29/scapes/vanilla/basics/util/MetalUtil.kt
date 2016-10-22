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

import java8.util.stream.Stream
import org.tobi29.scapes.engine.utils.forEach
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.AlloyType
import org.tobi29.scapes.vanilla.basics.material.MetalType
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
    alloy.metals().forEach { entry ->
        tagStructure.setDouble(entry.first.id(), entry.second)
    }
    return tagStructure
}

class Alloy {
    private val metals = ConcurrentHashMap<MetalType, Double>()

    fun add(metal: MetalType,
            amount: Double) {
        var containing: Double? = metals[metal]
        if (containing == null) {
            containing = amount
        } else {
            containing += amount
        }
        metals.put(metal, containing)
    }

    fun drain(metal: MetalType,
              maxAmount: Double): Double {
        var containing = metals[metal] ?: 0.0
        val drain: Double
        if (containing <= maxAmount) {
            drain = containing
            containing = 0.0
        } else {
            drain = maxAmount
            containing -= maxAmount
        }
        if (containing < 0.00001) {
            metals.remove(metal)
        } else {
            metals.put(metal, containing)
        }
        return drain
    }

    fun drain(maxAmount: Double): Alloy {
        val amount = amount()
        val alloy = Alloy()
        for ((key, value) in metals) {
            val drain = value / amount * maxAmount
            alloy.add(key, drain(key, drain))
        }
        return alloy
    }

    fun metals(): Stream<Pair<MetalType, Double>> {
        return metals.entries.stream().map { Pair(it.key, it.value) }
    }

    fun type(plugin: VanillaBasics): AlloyType {
        var bestAlloyType = plugin.alloyType("")
        if (metals.isEmpty()) {
            return bestAlloyType
        }
        val amount = amount()
        var bestOffset = Double.POSITIVE_INFINITY
        val iterator = plugin.alloyTypes().iterator()
        while (iterator.hasNext()) {
            val alloyType = iterator.next()
            var offset = 0.0
            for ((key, value) in metals) {
                val required = alloyType.ingredients()[key]
                if (required == null) {
                    offset = Double.POSITIVE_INFINITY
                    break
                }
                offset += abs(value / amount - required)
            }
            if (offset < bestOffset) {
                bestAlloyType = alloyType
                bestOffset = offset
            }
        }
        return bestAlloyType
    }

    fun amount(): Double {
        var amount = 0.0
        for ((key, value) in metals) {
            amount += value
        }
        return amount
    }

    fun meltingPoint(): Double {
        var amount = 0.0
        var temperature = 0.0
        for ((key, metal) in metals) {
            amount += metal
            temperature += metal * key.meltingPoint()
        }
        return temperature / amount
    }
}
