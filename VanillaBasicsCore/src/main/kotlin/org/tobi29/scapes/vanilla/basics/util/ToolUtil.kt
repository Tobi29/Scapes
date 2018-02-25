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

import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toTag
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.copy
import org.tobi29.scapes.vanilla.basics.material.item.tool.ItemFlintTool
import org.tobi29.scapes.vanilla.basics.material.item.tool.ItemMetalTool
import kotlin.collections.set

fun createTool(plugin: VanillaBasics,
               type: String,
               alloy: Alloy,
               temperature: Double = 0.0): TypedItem<ItemMetalTool> =
        createTool(plugin, id(type), alloy, temperature)

fun createTool(plugin: VanillaBasics,
               id: Int,
               alloy: Alloy,
               temperature: Double = 0.0): TypedItem<ItemMetalTool> {
    val materials = plugin.materials
    val alloyType = alloy.type(plugin)
    var efficiency = alloyType.toolEfficiency
    var strength = alloyType.toolStrength
    val damage = alloyType.toolDamage
    val level = alloyType.toolLevel
    val type = when (id) {
        1 -> {
            strength *= 0.1
            materials.metalPickaxe
        }
        2 -> {
            strength *= 1.5
            materials.metalAxe
        }
        3 -> {
            strength *= 0.1
            materials.metalShovel
        }
        4 -> {
            efficiency = 0.0
            strength *= 0.4
            materials.metalHammer
        }
        5 -> {
            strength *= 0.1
            materials.metalSaw
        }
        6 -> {
            strength *= 0.1
            materials.metalHoe
        }
        7 -> {
            efficiency = 0.0
            materials.metalSword
        }
        else -> throw IllegalArgumentException("Invalid tool type: $id")
    }
    val metaData = TagMap {
        this["ToolEfficiency"] = efficiency.toTag()
        this["ToolStrength"] = strength.toTag()
        this["ToolDamageAdd"] = damage.toTag()
        this["ToolLevel"] = level.toTag()
    }
    return TypedItem(type, metaData).copy(
            alloy = alloy,
            temperature = temperature)
}

fun createStoneTool(plugin: VanillaBasics,
                    type: String): TypedItem<ItemFlintTool> =
        createStoneTool(plugin, id(type))

fun createStoneTool(plugin: VanillaBasics,
                    id: Int): TypedItem<ItemFlintTool> {
    val materials = plugin.materials
    var efficiency = 1.0
    var strength = 4.0
    val damage = 0.004
    val level = 10
    val type = when (id) {
        1 -> {
            strength *= 0.1
            materials.flintPickaxe
        }
        2 -> {
            strength *= 1.5
            materials.flintAxe
        }
        3 -> {
            strength *= 0.1
            materials.flintShovel
        }
        4 -> {
            efficiency = 0.0
            strength *= 0.4
            materials.flintHammer
        }
        5 -> {
            strength *= 0.1
            materials.flintSaw
        }
        6 -> {
            strength *= 0.1
            materials.flintHoe
        }
        7 -> {
            efficiency = 0.0
            materials.flintSword
        }
        else -> throw IllegalArgumentException("Invalid tool type: $id")
    }
    val metaData = TagMap {
        this["ToolEfficiency"] = efficiency.toTag()
        this["ToolStrength"] = strength.toTag()
        this["ToolDamageAdd"] = damage.toTag()
        this["ToolLevel"] = level.toTag()
    }
    return TypedItem(type, metaData)
}

fun id(type: String): Int {
    when (type) {
        "Pickaxe" -> return 1
        "Axe" -> return 2
        "Shovel" -> return 3
        "Hammer" -> return 4
        "Saw" -> return 5
        "Hoe" -> return 6
        "Sword" -> return 7
        else -> return -1
    }
}
