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

package scapes.plugin.tobi29.vanilla.basics.util

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.io.tag.set
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemIngot

fun createTool(plugin: VanillaBasics,
               item: ItemStack,
               type: String): Boolean {
    return createTool(plugin, item, id(type))
}

fun createTool(plugin: VanillaBasics,
               item: ItemStack,
               id: Int): Boolean {
    val materials = plugin.materials
    val ingot = item.material() as ItemIngot
    val alloy = ingot.alloy(item)
    val alloyType = alloy.type(plugin)
    var efficiency = alloyType.baseToolEfficiency()
    var strength = alloyType.baseToolStrength()
    val damage = alloyType.baseToolDamage()
    val level = alloyType.baseToolLevel()
    when (id) {
        1 -> {
            item.setMaterial(materials.metalPickaxe)
            strength *= 0.1
        }
        2 -> {
            item.setMaterial(materials.metalAxe)
            strength *= 1.5
        }
        3 -> {
            item.setMaterial(materials.metalShovel)
            strength *= 0.1
        }
        4 -> {
            item.setMaterial(materials.metalHammer)
            efficiency = 0.0
            strength *= 0.4
        }
        5 -> {
            item.setMaterial(materials.metalSaw)
            strength *= 0.1
        }
        6 -> {
            item.setMaterial(materials.metalHoe)
            strength *= 0.1
        }
        7 -> {
            item.setMaterial(materials.metalSword)
            efficiency = 0.0
        }
        else -> return false
    }
    item.metaData("Vanilla")["ToolEfficiency"] = efficiency
    item.metaData("Vanilla")["ToolStrength"] = strength
    item.metaData("Vanilla")["ToolDamageAdd"] = damage
    item.metaData("Vanilla")["ToolLevel"] = level
    return true
}

fun createStoneTool(plugin: VanillaBasics,
                    item: ItemStack,
                    type: String): Boolean {
    return createStoneTool(plugin, item, id(type))
}

fun createStoneTool(plugin: VanillaBasics,
                    item: ItemStack,
                    id: Int): Boolean {
    val materials = plugin.materials
    var efficiency = 1.0
    var strength = 4.0
    val damage = 0.004
    val level = 10
    when (id) {
        1 -> {
            item.setMaterial(materials.flintPickaxe)
            strength *= 0.1
        }
        2 -> {
            item.setMaterial(materials.flintAxe)
            strength *= 1.5
        }
        3 -> {
            item.setMaterial(materials.flintShovel)
            strength *= 0.1
        }
        4 -> {
            item.setMaterial(materials.flintHammer)
            efficiency = 0.0
            strength *= 0.4
        }
        5 -> {
            item.setMaterial(materials.flintSaw)
            strength *= 0.1
        }
        6 -> {
            item.setMaterial(materials.flintHoe)
            strength *= 0.1
        }
        7 -> {
            item.setMaterial(materials.flintSword)
            efficiency = 0.0
        }
        else -> return false
    }
    item.metaData("Vanilla")["ToolEfficiency"] = efficiency
    item.metaData("Vanilla")["ToolStrength"] = strength
    item.metaData("Vanilla")["ToolDamageAdd"] = damage
    item.metaData("Vanilla")["ToolLevel"] = level
    return true
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
