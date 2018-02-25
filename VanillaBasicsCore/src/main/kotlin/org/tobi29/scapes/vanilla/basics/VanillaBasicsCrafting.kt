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

import org.tobi29.io.tag.map
import org.tobi29.io.tag.toBoolean
import org.tobi29.scapes.block.ItemStackData
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.block.copy
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.ItemStack
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.generator.StoneType
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipe
import org.tobi29.scapes.vanilla.basics.material.CraftingRecipeType
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.TreeType
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.createStoneTool
import org.tobi29.scapes.vanilla.basics.util.createTool

internal fun VanillaBasics.registerRecipes(registry: Registries) {
    val cropRegistry = registry.get<CropType>("VanillaBasics", "CropType")
    val treeRegistry = registry.get<TreeType>("VanillaBasics", "TreeType")
    val stoneRegistry = registry.get<StoneType>("VanillaBasics", "StoneType")
    registerRecipesBasics(registry, treeRegistry, stoneRegistry)
    registerRecipesStone(registry)
    registerRecipesFood(registry, cropRegistry, stoneRegistry)
    registerRecipesMetal(registry, stoneRegistry)
    registerRecipesIron(registry, treeRegistry)
}

private fun VanillaBasics.registerRecipesBasics(registry: Registries,
                                                treeRegistry: Registries.Registry<TreeType>,
                                                stoneRegistry: Registries.Registry<StoneType>) {
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

    val exampleAlloy = Alloy(
            mapOf((metalType("Iron") ?: crapMetal) to 1.0))
    val exampleSaws = listOf(
            TypedItem(materials.flintSaw).copy(data = 1),
            createTool(this@registerRecipesBasics, "Saw", exampleAlloy)
                    .copy(data = 1))
    val exampleHammers = listOf(
            TypedItem(materials.flintHammer).copy(data = 1),
            createTool(this@registerRecipesBasics, "Hammer", exampleAlloy)
                    .copy(data = 1))
    val plank = treeRegistry.values().asSequence().filterNotNull().map {
        ItemStackData(materials.wood, it.id)
    }.toList()

    crafting(recipeType) {
        recipe("vanilla.basics.crafting.basics.CraftingTable") {
            ingredients.add(plank)
            requirements.add(CraftingRecipe.IngredientPredicate(
                    {
                        it.type == materials.flintSaw
                                || it.type == materials.metalSaw
                    }, exampleSaws))
            requirements.add(CraftingRecipe.IngredientPredicate(
                    {
                        it.type == materials.flintHammer
                                || it.type == materials.metalHammer
                    }, exampleHammers))
            result = ItemStackData(materials.craftingTable, 0)
        }
        recipe("vanilla.basics.crafting.basics.Chest") {
            ingredients.add(plank)
            requirements.add(CraftingRecipe.IngredientPredicate(
                    {
                        it.type == materials.flintSaw
                                || it.type == materials.metalSaw
                    }, exampleSaws))
            result = ItemStackData(materials.chest, 0)
        }
        recipe("vanilla.basics.crafting.basics.ResearchTable") {
            ingredients.add(plank)
            ingredients.add(TypedItem(materials.fabric))
            requirements.add(CraftingRecipe.IngredientPredicate(
                    {
                        it.type == materials.flintSaw
                                || it.type == materials.metalSaw
                    }, exampleSaws))
            result = ItemStackData(materials.researchTable, 0)
        }
        stoneRegistry.values().asSequence().filterNotNull().map { it.id }.forEach {
            recipe("vanilla.basics.crafting.basics.Cobblestone" + it, {
                ingredients.add(ItemStackData(materials.stoneRock, it, 9))
                requirements.add(CraftingRecipe.IngredientPredicate(
                        {
                            it.type == materials.flintHammer
                                    || it.type == materials.metalHammer
                        }, exampleHammers))
                result = ItemStackData(materials.cobblestone, it)
            })
        }
        recipe("vanilla.basics.crafting.basics.Torch") {
            ingredients.add(TypedItem(materials.stick))
            ingredients.add(TypedItem(materials.coal))
            result = ItemStackData(materials.torch, 0)
        }
    }
}

private fun VanillaBasics.registerRecipesStone(registry: Registries) {
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

    val flint = ItemStackData(materials.stoneRock, stoneTypes.FLINT.id)
    val hoeHead = createStoneTool(this, "Hoe")
    val hoe = hoeHead.copy(data = 1)
    val hammerHead = createStoneTool(this, "Hammer")
    val hammer = hammerHead.copy(data = 1)
    val sawHead = createStoneTool(this, "Saw")
    val saw = sawHead.copy(data = 1)
    val axeHead = createStoneTool(this, "Axe")
    val axe = axeHead.copy(data = 1)
    val shovelHead = createStoneTool(this, "Shovel")
    val shovel = shovelHead.copy(data = 1)
    val pickaxeHead = createStoneTool(this, "Pickaxe")
    val pickaxe = pickaxeHead.copy(data = 1)
    val swordHead = createStoneTool(this, "Sword")
    val sword = swordHead.copy(data = 1)

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
            ingredients.add(
                    listOf(hoe, hoeHead, hammer, hammerHead, saw, sawHead, axe,
                            axeHead, shovel, shovelHead, pickaxe, pickaxeHead,
                            sword, swordHead))
            result = flint
        }
        recipe("vanilla.basics.crafting.stone.String") {
            ingredients.add(ItemStack(materials.grassBundle, 2))
            result = TypedItem(materials.string)
        }
        recipe("vanilla.basics.crafting.stone.Fabric") {
            ingredients.add(ItemStack(materials.string, 8))
            result = TypedItem(materials.fabric)
        }
        recipe("vanilla.basics.crafting.stone.WetStraw") {
            ingredients.add(ItemStack(materials.grassBundle, 2))
            result = TypedItem(materials.strawBlock)
        }
        recipe("vanilla.basics.crafting.stone.Straw") {
            ingredients.add(ItemStack(materials.grassBundle, 2))
            result = ItemStackData(materials.strawBlock, 1)
        }
    }
}

private fun VanillaBasics.registerRecipesFood(registry: Registries,
                                              cropRegistry: Registries.Registry<CropType>,
                                              stoneRegistry: Registries.Registry<StoneType>) {
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
            .filter { it.resistance > 0.1 }.map { it.id }
            .map { ItemStackData(materials.cobblestone, it, 2) }.toList()
    val exampleAlloy = Alloy(
            mapOf((metalType("Iron") ?: crapMetal) to 1.0))
    val examplePickaxes = listOf(
            TypedItem(materials.flintPickaxe).copy(data = 1),
            createTool(this@registerRecipesFood, "Pickaxe", exampleAlloy)
                    .copy(data = 1))
    val exampleHammers = listOf(
            TypedItem(materials.flintHammer).copy(data = 1),
            createTool(this@registerRecipesFood, "Hammer", exampleAlloy)
                    .copy(data = 1))

    crafting(recipeType) {
        // TODO: Replace with oven
        recipe("vanilla.basics.crafting.food.Furnace") {
            ingredients.add(cobblestones)
            ingredients.add(ItemStack(materials.stick, 4))
            requirements.add(CraftingRecipe.IngredientPredicate(
                    {
                        it.type == materials.flintPickaxe
                                || it.type == materials.metalPickaxe
                    }, examplePickaxes))
            result = ItemStackData(materials.furnace, 0)
        }
        recipe("vanilla.basics.crafting.food.Quern") {
            ingredients.add(cobblestones)
            requirements.add(CraftingRecipe.IngredientPredicate(
                    {
                        it.type == materials.flintPickaxe
                                || it.type == materials.metalPickaxe
                    }, examplePickaxes))
            result = ItemStackData(materials.quern, 0)
        }
        cropRegistry.values().asSequence().filterNotNull().forEach {
            recipe("vanilla.basics.crafting.food.Dough" + it.id, {
                ingredients.add(ItemStack(materials.grain, it, 8))
                result = Item(materials.dough, it)
            })
        }
        recipe("vanilla.basics.crafting.") {
            ingredients.add(cobblestones)
            requirements.add(CraftingRecipe.IngredientPredicate(
                    {
                        it.type == materials.flintHammer
                                || it.type == materials.metalHammer
                    }, exampleHammers))
            result = ItemStackData(materials.quern, 0)
        }
    }
}

private fun VanillaBasics.registerRecipesMetal(registry: Registries,
                                               stoneRegistry: Registries.Registry<StoneType>) {
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
            .filter { it.resistance > 0.1 }.map { it.id }
            .map { ItemStackData(materials.cobblestone, it, 2) }.toList()

    crafting(recipeType) {
        recipe("vanilla.basics.crafting.metal.Mold") {
            ingredients.add(ItemStackData(materials.sand, 2))
            result = ItemStackData(materials.mold, 0)
        }
        recipe("vanilla.basics.crafting.metal.Anvil") {
            ingredients.add(ItemStackData(materials.ingot, 0, 5))
            result = ItemStackData(materials.anvil, 0)
        }
        recipe("vanilla.basics.crafting.metal.Forge") {
            ingredients.add(ItemStack(materials.coal, 8))
            result = ItemStackData(materials.forge, 0)
        }
        recipe("vanilla.basics.crafting.metal.Alloy") {
            ingredients.add(cobblestones)
            requirements.add(ItemStackData(materials.metalPickaxe, 1))
            result = ItemStackData(materials.alloy, 0)
        }
    }
}

private fun VanillaBasics.registerRecipesIron(registry: Registries,
                                              treeRegistry: Registries.Registry<TreeType>) {
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
            .map { ItemStackData(materials.wood, it.id) }.toList()

    crafting(recipeType) {
        recipe("vanilla.basics.crafting.iron.Bloomery") {
            ingredients.add(ItemStackData(materials.sand, 2))
            result = ItemStackData(materials.bloomery, 0)
        }
        recipe("vanilla.basics.crafting.iron.Bellows") {
            ingredients.add(plank)
            ingredients.add(ItemStack(materials.fabric, 4))

            val exampleAlloy = Alloy(
                    mapOf((metalType("Iron") ?: crapMetal) to 1.0))
            val exampleSaws = listOf(
                    TypedItem(materials.flintSaw).copy(data = 1),
                    createTool(this@registerRecipesIron, "Saw", exampleAlloy)
                            .copy(data = 1))
            requirements.add(CraftingRecipe.IngredientPredicate(
                    {
                        it.type == materials.flintPickaxe
                                || it.type == materials.metalSaw
                    }, exampleSaws))
            result = ItemStackData(materials.bellows, 0)
        }
    }
}
