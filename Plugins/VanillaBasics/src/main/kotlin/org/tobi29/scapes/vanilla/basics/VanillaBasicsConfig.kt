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

package org.tobi29.scapes.vanilla.basics

import java8.util.stream.Collectors
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.generator.decorator.BiomeDecorator
import org.tobi29.scapes.vanilla.basics.material.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun VanillaBasics.research(name: String,
                           text: String,
                           vararg items: String) {
    val researchRecipe = ResearchRecipe(name, text, arrayOf(*items))
    addResearchRecipe(researchRecipe)
}

fun VanillaBasics.decorator(name: String,
                            overlay: BiomeDecorator.() -> Unit) {
    addBiomeDecoratorOverlay(name, overlay)
}

fun VanillaBasics.decorator(biome: BiomeGenerator.Biome,
                            name: String,
                            weight: Int,
                            decorator: BiomeDecorator.() -> Unit) {
    addBiomeDecorator(biome, name, weight, decorator)
}

fun VanillaBasics.metal(metal: MetalTypeCreator.() -> Unit) {
    val creator = MetalTypeCreator()
    metal(creator)
    val metalType = MetalType(creator.id, creator.name,
            creator.meltingPoint, creator.r, creator.g, creator.b)
    val alloyType = AlloyType(creator.id, creator.name, creator.ingotName,
            Collections.singletonMap(metalType, 1.0), creator.r,
            creator.g, creator.b, creator.toolEfficiency,
            creator.toolStrength, creator.toolDamage,
            creator.toolLevel)
    addMetalType(metalType)
    addAlloyType(alloyType)
}

fun VanillaBasics.alloy(alloy: AlloyTypeCreator.() -> Unit) {
    val creator = AlloyTypeCreator()
    alloy(creator)
    val ingredients = ConcurrentHashMap<MetalType, Double>()
    creator.ingredients.entries.forEach {
        ingredients.put(metalType(it.key), it.value)
    }
    val alloyType = AlloyType(creator.id, creator.name, creator.ingotName,
            ingredients, creator.r, creator.g, creator.b,
            creator.toolEfficiency, creator.toolStrength,
            creator.toolDamage, creator.toolLevel)
    addAlloyType(alloyType)
}

fun VanillaBasics.ore(ore: OreTypeCreator.() -> Unit) {
    val creator = OreTypeCreator(materials)
    ore(creator)
    val stoneTypes = creator.stoneTypes.stream().map { stoneType ->
        stoneType.data(materials.registry)
    }.collect(Collectors.toList<Int>())
    val oreType = OreType(creator.type, creator.rarity, creator.size,
            creator.chance, creator.rockChance,
            creator.rockDistance, stoneTypes)
    addOreType(oreType)
}

fun VanillaBasics.crafting(type: CraftingRecipeType,
                           recipes: CraftingRecipesCreator.() -> Unit) {
    val creator = CraftingRecipesCreator(materials, type)
    recipes(creator)
    addCraftingRecipe(type)
}

class MetalTypeCreator {
    var id = ""
    var name = ""
    var ingotName = ""
    var meltingPoint = 0.0
    var r = 0.0f
    var g = 0.0f
    var b = 0.0f
    var toolEfficiency = 0.0
    var toolStrength = 0.0
    var toolDamage = 0.0
    var toolLevel = 0
}

class AlloyTypeCreator {
    val ingredients = ConcurrentHashMap<String, Double>()
    var id = ""
    var name = ""
    var ingotName = ""
    var r = 0.0f
    var g = 0.0f
    var b = 0.0f
    var toolEfficiency = 0.0
    var toolStrength = 0.0
    var toolDamage = 0.0
    var toolLevel = 0
}

class OreTypeCreator {
    constructor(materials: VanillaMaterial) {
        type = materials.stoneRaw
    }

    val stoneTypes = ArrayList<StoneType>()
    var type: BlockType
    var rarity = 4
    var chance = 4
    var rockChance = 8
    var rockDistance = 48
    var size = 6.0
}

class CraftingRecipeCreator {
    val ingredients = RecipeList()
    val requirements = RecipeList()
    var result: ItemStack? = null
}

class RecipeList {
    internal val list = ArrayList<CraftingRecipe.Ingredient>()

    fun add(ingredient: CraftingRecipe.Ingredient) {
        list.add(ingredient)
    }

    fun add(item: ItemStack) {
        add(CraftingRecipe.IngredientList(item))
    }

    fun add(item: List<ItemStack>) {
        add(CraftingRecipe.IngredientList(item))
    }
}

class CraftingRecipesCreator(val materials: VanillaMaterial, val type: CraftingRecipeType) {
    fun recipe(id: String,
               craftingRecipe: CraftingRecipeCreator.() -> Unit) {
        val craftingRegistry = materials.registry.get<CraftingRecipe>(
                "VanillaBasics", "CraftingRecipe")
        val creator = CraftingRecipeCreator()
        craftingRecipe(creator)
        val result = creator.result ?: throw IllegalArgumentException(
                "Result is not set")
        val recipe = CraftingRecipe(creator.ingredients.list,
                creator.requirements.list, result)
        craftingRegistry.reg(recipe, id)
        type.add(recipe)
    }
}
