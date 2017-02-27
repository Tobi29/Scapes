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

package org.tobi29.scapes.vanilla.basics

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.io.tag.map
import org.tobi29.scapes.engine.utils.io.tag.toBoolean
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipeType
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.StoneType
import org.tobi29.scapes.vanilla.basics.material.TreeType
import org.tobi29.scapes.vanilla.basics.util.createStoneTool

internal fun VanillaBasics.registerRecipes(registry: GameRegistry) {
    val cropRegistry = registry.get<CropType>("VanillaBasics", "CropType")
    val treeRegistry = registry.get<TreeType>("VanillaBasics", "TreeType")
    val stoneRegistry = registry.get<StoneType>("VanillaBasics", "StoneType")
    registerRecipesBasics(registry, treeRegistry, stoneRegistry)
    registerRecipesStone(registry)
    registerRecipesFood(registry, cropRegistry, stoneRegistry)
    registerRecipesMetal(registry, stoneRegistry)
    registerRecipesIron(registry, treeRegistry)
}

private fun VanillaBasics.registerRecipesBasics(registry: GameRegistry,
                                                treeRegistry: GameRegistry.Registry<TreeType>,
                                                stoneRegistry: GameRegistry.Registry<StoneType>) {
    val recipeType = object : CraftingRecipeType() {
        override fun name(): String {
            return "Basics"
        }

        override fun table(): Boolean {
            return false
        }

        override fun availableFor(player: MobPlayerServer): Boolean {
            return true
        }

        override fun availableFor(player: MobPlayerClientMain): Boolean {
            return true
        }
    }

    val hammer = listOf(materials.flintHammer.example(1),
            materials.metalHammer.example(1))
    val saw = listOf(materials.flintSaw.example(1),
            materials.metalSaw.example(1))
    val plank = treeRegistry.values().asSequence().filterNotNull().map {
        ItemStack(materials.wood, it.data(registry))
    }.toList()

    crafting(recipeType) {
        recipe("vanilla.basics.crafting.basics.CraftingTable") {
            ingredients.add(plank)
            ingredients.add(hammer)
            requirements.add(saw)
            result = ItemStack(materials.craftingTable, 0)
        }
        recipe("vanilla.basics.crafting.basics.Chest") {
            ingredients.add(plank)
            requirements.add(saw)
            result = ItemStack(materials.chest, 0)
        }
        recipe("vanilla.basics.crafting.basics.ResearchTable") {
            ingredients.add(plank)
            ingredients.add(ItemStack(materials.string, 1))
            requirements.add(saw)
            result = ItemStack(materials.researchTable, 0)
        }
        stoneRegistry.values().asSequence().filterNotNull()
                .map { it.data(registry) }.forEach {
            recipe("vanilla.basics.crafting.basics.Cobblestone" + it, {
                ingredients.add(ItemStack(materials.stoneRock, it, 9))
                requirements.add(hammer)
                result = ItemStack(materials.cobblestone, it)
            })
        }
        recipe("vanilla.basics.crafting.basics.Torch") {
            ingredients.add(ItemStack(materials.stick, 0))
            ingredients.add(ItemStack(materials.coal, 0))
            result = ItemStack(materials.torch, 0)
        }
    }
}

private fun VanillaBasics.registerRecipesStone(registry: GameRegistry) {
    val recipeType = object : CraftingRecipeType() {
        override fun name(): String {
            return "Stone"
        }

        override fun table(): Boolean {
            return false
        }

        override fun availableFor(player: MobPlayerServer): Boolean {
            return true
        }

        override fun availableFor(player: MobPlayerClientMain): Boolean {
            return true
        }
    }

    val flint = ItemStack(materials.stoneRock,
            StoneType.FLINT.data(registry))
    val hoe = ItemStack(flint)
    createStoneTool(this, hoe, "Hoe")
    val hoeHead = ItemStack(hoe).setData(0)
    val hammer = ItemStack(flint)
    createStoneTool(this, hammer, "Hammer")
    val hammerHead = ItemStack(hammer).setData(0)
    val saw = ItemStack(flint)
    createStoneTool(this, saw, "Saw")
    val sawHead = ItemStack(saw).setData(0)
    val axe = ItemStack(flint)
    createStoneTool(this, axe, "Axe")
    val axeHead = ItemStack(axe).setData(0)
    val shovel = ItemStack(flint)
    createStoneTool(this, shovel, "Shovel")
    val shovelHead = ItemStack(shovel).setData(0)
    val pickaxe = ItemStack(flint)
    createStoneTool(this, pickaxe, "Pickaxe")
    val pickaxeHead = ItemStack(pickaxe).setData(0)
    val sword = ItemStack(flint)
    createStoneTool(this, sword, "Sword")
    val swordHead = ItemStack(sword).setData(0)

    crafting(recipeType) {
        recipe("vanilla.basics.crafting.stone.HoeHead") {
            ingredients.add(flint)
            requirements.add(flint)
            result = hoeHead
        }
        recipe("vanilla.basics.crafting.stone.HammerHead") {
            ingredients.add(flint)
            requirements.add(flint)
            result = hammerHead
        }
        recipe("vanilla.basics.crafting.stone.SawHead") {
            ingredients.add(flint)
            requirements.add(flint)
            result = sawHead
        }
        recipe("vanilla.basics.crafting.stone.AxeHead") {
            ingredients.add(flint)
            requirements.add(flint)
            result = axeHead
        }
        recipe("vanilla.basics.crafting.stone.ShovelHead") {
            ingredients.add(flint)
            requirements.add(flint)
            result = shovelHead
        }
        recipe("vanilla.basics.crafting.stone.PickaxeHead") {
            ingredients.add(flint)
            requirements.add(flint)
            result = pickaxeHead
        }
        recipe("vanilla.basics.crafting.stone.SwordHead") {
            ingredients.add(flint)
            requirements.add(flint)
            result = swordHead
        }
        recipe("vanilla.basics.crafting.stone.BreakTool") {
            ingredients.add(listOf(hoe, hoeHead, hammer, hammerHead, saw,
                    sawHead, axe,
                    axeHead, shovel, shovelHead, pickaxe, pickaxeHead,
                    sword, swordHead))
            result = flint
        }
        recipe("vanilla.basics.crafting.stone.String") {
            ingredients.add(ItemStack(materials.grassBundle, 0, 2))
            result = ItemStack(materials.string, 0)
        }
        recipe("vanilla.basics.crafting.stone.Fabric") {
            ingredients.add(ItemStack(materials.string, 0, 8))
            result = ItemStack(materials.string, 1)
        }
        recipe("vanilla.basics.crafting.stone.WetStraw") {
            ingredients.add(ItemStack(materials.grassBundle, 0, 2))
            result = ItemStack(materials.straw, 0)
        }
        recipe("vanilla.basics.crafting.stone.Straw") {
            ingredients.add(ItemStack(materials.grassBundle, 1, 2))
            result = ItemStack(materials.straw, 1)
        }
    }
}

private fun VanillaBasics.registerRecipesFood(registry: GameRegistry,
                                              cropRegistry: GameRegistry.Registry<CropType>,
                                              stoneRegistry: GameRegistry.Registry<StoneType>) {
    val recipeType = object : CraftingRecipeType() {
        override fun name(): String {
            return "Food"
        }

        override fun table(): Boolean {
            return true
        }

        override fun availableFor(player: MobPlayerServer): Boolean {
            return player.metaData("Vanilla").map("Research")?.map(
                    "Finished")?.get("Food")?.toBoolean() ?: false
        }

        override fun availableFor(player: MobPlayerClientMain): Boolean {
            return player.metaData("Vanilla").map("Research")?.map(
                    "Finished")?.get("Food")?.toBoolean() ?: false
        }
    }

    val cobblestones = stoneRegistry.values().asSequence().filterNotNull()
            .filter { it.resistance() > 0.1 }.map { it.data(registry) }
            .map { ItemStack(materials.cobblestone, it, 2) }.toList()
    val pickaxe = listOf(materials.flintPickaxe.example(1),
            materials.metalPickaxe.example(1))
    val hammer = listOf(materials.flintHammer.example(1),
            materials.metalHammer.example(1))

    crafting(recipeType) {
        // TODO: Replace with oven
        recipe("vanilla.basics.crafting.food.Furnace") {
            ingredients.add(cobblestones)
            ingredients.add(ItemStack(materials.stick, 0, 4))
            requirements.add(pickaxe)
            result = ItemStack(materials.furnace, 0)
        }
        recipe("vanilla.basics.crafting.food.Quern") {
            ingredients.add(cobblestones)
            requirements.add(hammer)
            result = ItemStack(materials.quern, 0)
        }
        cropRegistry.values().asSequence().filterNotNull()
                .map { it.data(registry) }.forEach {
            recipe("vanilla.basics.crafting.food.Dough" + it, {
                ingredients.add(ItemStack(materials.grain, it, 8))
                result = ItemStack(materials.dough, it)
            })
        }
        recipe("vanilla.basics.crafting.") {
            ingredients.add(cobblestones)
            requirements.add(hammer)
            result = ItemStack(materials.quern, 0)
        }
    }
}

private fun VanillaBasics.registerRecipesMetal(registry: GameRegistry,
                                               stoneRegistry: GameRegistry.Registry<StoneType>) {
    val recipeType = object : CraftingRecipeType() {
        override fun name(): String {
            return "Metal"
        }

        override fun table(): Boolean {
            return true
        }

        override fun availableFor(player: MobPlayerServer): Boolean {
            return player.metaData("Vanilla").map("Research")?.map(
                    "Finished")?.get("Metal")?.toBoolean() ?: false
        }

        override fun availableFor(player: MobPlayerClientMain): Boolean {
            return player.metaData("Vanilla").map("Research")?.map(
                    "Finished")?.get("Metal")?.toBoolean() ?: false
        }
    }
    val cobblestones = stoneRegistry.values().asSequence().filterNotNull()
            .filter { it.resistance() > 0.1 }.map { it.data(registry) }
            .map { ItemStack(materials.cobblestone, it, 2) }.toList()

    crafting(recipeType) {
        recipe("vanilla.basics.crafting.metal.Mold") {
            ingredients.add(ItemStack(materials.sand, 2))
            result = ItemStack(materials.mold, 0)
        }
        recipe("vanilla.basics.crafting.metal.Anvil") {
            ingredients.add(ItemStack(materials.ingot, 0, 5))
            result = ItemStack(materials.anvil, 0)
        }
        recipe("vanilla.basics.crafting.metal.Forge") {
            ingredients.add(ItemStack(materials.coal, 0, 8))
            result = ItemStack(materials.forge, 0)
        }
        recipe("vanilla.basics.crafting.metal.Alloy") {
            ingredients.add(cobblestones)
            requirements.add(ItemStack(materials.metalPickaxe, 1))
            result = ItemStack(materials.alloy, 0)
        }
    }
}

private fun VanillaBasics.registerRecipesIron(registry: GameRegistry,
                                              treeRegistry: GameRegistry.Registry<TreeType>) {
    val recipeType = object : CraftingRecipeType() {
        override fun name(): String {
            return "Iron"
        }

        override fun table(): Boolean {
            return true
        }

        override fun availableFor(player: MobPlayerServer): Boolean {
            return player.metaData("Vanilla").map("Research")?.map(
                    "Finished")?.get("Iron")?.toBoolean() ?: false
        }

        override fun availableFor(player: MobPlayerClientMain): Boolean {
            return player.metaData("Vanilla").map("Research")?.map(
                    "Finished")?.get("Iron")?.toBoolean() ?: false
        }
    }

    val plank = treeRegistry.values().asSequence().filterNotNull()
            .map { ItemStack(materials.wood, it.data(registry)) }
            .toList()

    crafting(recipeType) {
        recipe("vanilla.basics.crafting.iron.Bloomery") {
            ingredients.add(ItemStack(materials.sand, 2))
            result = ItemStack(materials.bloomery, 0)
        }
        recipe("vanilla.basics.crafting.iron.Bellows") {
            ingredients.add(plank)
            ingredients.add(ItemStack(materials.string, 1, 4))
            requirements.add(materials.metalSaw.example(1))
            result = ItemStack(materials.bellows, 0)
        }
    }
}
