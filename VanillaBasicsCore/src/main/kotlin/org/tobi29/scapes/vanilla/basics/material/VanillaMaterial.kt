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

import org.tobi29.scapes.block.MaterialType
import org.tobi29.scapes.inventory.ItemType
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.plugins.reg
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.block.BlockLava
import org.tobi29.scapes.vanilla.basics.material.block.BlockSnow
import org.tobi29.scapes.vanilla.basics.material.block.BlockWater
import org.tobi29.scapes.vanilla.basics.material.block.device.*
import org.tobi29.scapes.vanilla.basics.material.block.rock.*
import org.tobi29.scapes.vanilla.basics.material.block.soil.BlockDirt
import org.tobi29.scapes.vanilla.basics.material.block.soil.BlockFarmland
import org.tobi29.scapes.vanilla.basics.material.block.soil.BlockGrass
import org.tobi29.scapes.vanilla.basics.material.block.soil.BlockSand
import org.tobi29.scapes.vanilla.basics.material.block.structural.*
import org.tobi29.scapes.vanilla.basics.material.block.vegetation.*
import org.tobi29.scapes.vanilla.basics.material.item.*
import org.tobi29.scapes.vanilla.basics.material.item.food.*
import org.tobi29.scapes.vanilla.basics.material.item.tool.*
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemCropDrop
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemGrassBundle
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemSeed
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemStraw

class VanillaMaterial(val plugin: VanillaBasics,
                      val plugins: Plugins) {
    val air = plugins.air
    val anvil = reg("vanilla.basics.block.Anvil", ::BlockAnvil)
    val alloy = reg("vanilla.basics.block.Alloy", ::BlockAlloy)
    val baked = reg("vanilla.basics.item.Baked", ::ItemBaked)
    val bedrock = reg("vanilla.basics.block.Bedrock", ::BlockBedrock)
    val bellows = reg("vanilla.basics.block.Bellows", ::BlockBellows)
    val bloomery = reg("vanilla.basics.block.Bloomery", ::BlockBloomery)
    val brick = reg("vanilla.basics.block.Brick", ::BlockBrick)
    val bush = reg("vanilla.basics.block.Bush", ::BlockBush)
    val chest = reg("vanilla.basics.block.Chest", ::BlockChest)
    val coal = reg("vanilla.basics.item.Coal", ::ItemCoal)
    val cobblestone = reg("vanilla.basics.block.Cobblestone",
            ::BlockCobblestone)
    val cobblestoneCracked = reg("vanilla.basics.block.",
            ::BlockCobblestoneCracked)
    val cobblestoneMossy = reg("vanilla.basics.block.CobblestoneMossy",
            ::BlockCobblestoneMossy)
    val cookedMeat = reg("vanilla.basics.item.CookedMeat", ::ItemCookedMeat)
    val craftingTable = reg("vanilla.basics.block.CraftingTable",
            ::BlockCraftingTable)
    val crop = reg("vanilla.basics.block.Crop", ::BlockCrop)
    val cropDrop = reg("vanilla.basics.item.CropDrop", ::ItemCropDrop)
    val dirt = reg("vanilla.basics.block.Dirt", ::BlockDirt)
    val dough = reg("vanilla.basics.item.Dough", ::ItemDough)
    val fabric = reg("vanilla.basics.item.Fabric", ::ItemFabric)
    val farmland = reg("vanilla.basics.block.Farmland", ::BlockFarmland)
    val flintAxe = reg("vanilla.basics.item.FlintAxe", ::ItemFlintAxe)
    val flintHammer = reg("vanilla.basics.item.FlintHammer", ::ItemFlintHammer)
    val flintHoe = reg("vanilla.basics.item.FlintHoe", ::ItemFlintHoe)
    val flintPickaxe = reg("vanilla.basics.item.FlintPickaxe",
            ::ItemFlintPickaxe)
    val flintSaw = reg("vanilla.basics.item.FlintSaw", ::ItemFlintSaw)
    val flintShovel = reg("vanilla.basics.item.FlintShovel", ::ItemFlintShovel)
    val flintSword = reg("vanilla.basics.item.FlintSword", ::ItemFlintSword)
    val flower = reg("vanilla.basics.block.Flower", ::BlockFlower)
    val forge = reg("vanilla.basics.block.Forge", ::BlockForge)
    val fertilizer = reg("vanilla.basics.item.Fertilizer", ::ItemFertilizer)
    val furnace = reg("vanilla.basics.block.Furnace", ::BlockFurnace)
    val grain = reg("vanilla.basics.item.Grain", ::ItemGrain)
    val glass = reg("vanilla.basics.block.Glass", ::BlockGlass)
    val grass = reg("vanilla.basics.block.Grass", ::BlockGrass)
    val grassBundle = reg("vanilla.basics.item.GrassBundle", ::ItemGrassBundle)
    val hyperBomb = reg("vanilla.basics.block.HyperBomb", ::BlockHyperBomb)
    val ingot = reg("vanilla.basics.item.Ingot", ::ItemIngot)
    val lava = reg("vanilla.basics.block.Lava", ::BlockLava)
    val leaves = reg("vanilla.basics.block.Leaves", ::BlockLeaves)
    val log = reg("vanilla.basics.block.Log", ::BlockLog)
    val meat = reg("vanilla.basics.item.Meat", ::ItemMeat)
    val metalAxe = reg("vanilla.basics.item.MetalAxe", ::ItemMetalAxe)
    val metalHammer = reg("vanilla.basics.item.MetalHammer", ::ItemMetalHammer)
    val metalHoe = reg("vanilla.basics.item.MetalHoe", ::ItemMetalHoe)
    val metalPickaxe = reg("vanilla.basics.item.MetalPickaxe",
            ::ItemMetalPickaxe)
    val metalSaw = reg("vanilla.basics.item.MetalSaw", ::ItemMetalSaw)
    val metalShovel = reg("vanilla.basics.item.MetalShovel", ::ItemMetalShovel)
    val metalSword = reg("vanilla.basics.item.MetalSword", ::ItemMetalSword)
    val mold = reg("vanilla.basics.item.Mold", ::ItemMold)
    val oreBismuthinite = reg("vanilla.basics.block.OreBismuthinite",
            ::BlockOreBismuthinite)
    val oreCassiterite = reg("vanilla.basics.block.OreCassiterite",
            ::BlockOreCassiterite)
    val oreChalcocite = reg("vanilla.basics.block.OreChalcocite",
            ::BlockOreChalcocite)
    val oreChunk = reg("vanilla.basics.item.OreChunk", ::ItemOreChunk)
    val oreCoal = reg("vanilla.basics.block.OreCoal", ::BlockOreCoal)
    val oreGold = reg("vanilla.basics.block.OreGold", ::BlockOreGold)
    val oreMagnetite = reg("vanilla.basics.block.OreMagnetite",
            ::BlockOreMagnetite)
    val orePyrite = reg("vanilla.basics.block.OrePyrite", ::BlockOrePyrite)
    val oreSilver = reg("vanilla.basics.block.OreSilver", ::BlockOreSilver)
    val oreSphalerite = reg("vanilla.basics.block.OreSphalerite",
            ::BlockOreSphalerite)
    val quern = reg("vanilla.basics.block.Quern", ::BlockQuern)
    val researchTable = reg("vanilla.basics.block.ResearchTable",
            ::BlockResearchTable)
    val sand = reg("vanilla.basics.block.Sand", ::BlockSand)
    val sandstone = reg("vanilla.basics.block.Sandstone", ::BlockSandstone)
    val sapling = reg("vanilla.basics.block.Sapling", ::BlockSapling)
    val snow = reg("vanilla.basics.block.Snow", ::BlockSnow)
    val stick = reg("vanilla.basics.item.Stick", ::ItemStick)
    val stoneRaw = reg("vanilla.basics.block.StoneRaw", ::BlockStoneRaw)
    val stoneTotem = reg("vanilla.basics.block.StoneTotem", ::BlockStoneTotem)
    val strawBlock = reg("vanilla.basics.block.Straw", ::BlockStraw)
    val stoneRock = reg("vanilla.basics.block.StoneRock", ::BlockStoneRock)
    val seed = reg("vanilla.basics.item.Seed", ::ItemSeed)
    val straw = reg("vanilla.basics.item.Straw", ::ItemStraw)
    val string = reg("vanilla.basics.item.String", ::ItemString)
    val torch = reg("vanilla.basics.block.Torch", ::BlockTorch)
    val water = reg("vanilla.basics.block.Water", ::BlockWater)
    val wood = reg("vanilla.basics.block.Wood", ::BlockWood)

    private fun <M : ItemType> reg(name: String,
                                   block: (VanillaMaterialType) -> M) =
            plugins.registry.get<ItemType>("Core", "ItemType").reg(name,
                    plugins) { block(VanillaMaterialType(it, this)) }
}

data class VanillaMaterialType(val type: MaterialType,
                               val materials: VanillaMaterial) {
    val plugins get() = type.plugins
    val id get() = type.id
    val name get() = type.name
}
