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
package org.tobi29.scapes.vanilla.basics.material

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.Material
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
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemCrop
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemGrassBundle
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemSeed

class VanillaMaterial(val plugin: VanillaBasics, val registry: GameRegistry) {
    val air: BlockType
    val anvil: BlockType
    val alloy: BlockType
    val baked: Material
    val bedrock: BlockType
    val bellows: BlockType
    val bloomery: BlockType
    val brick: BlockType
    val bush: BlockType
    val chest: BlockType
    val coal: Material
    val cobblestone: BlockType
    val cobblestoneCracked: BlockType
    val cobblestoneMossy: BlockType
    val cookedMeat: Material
    val craftingTable: BlockType
    val crop: BlockType
    val cropDrop: Material
    val dirt: BlockType
    val dough: Material
    val farmland: BlockType
    val flintAxe: ItemFlintTool
    val flintHammer: ItemFlintTool
    val flintHoe: ItemFlintTool
    val flintPickaxe: ItemFlintTool
    val flintSaw: ItemFlintTool
    val flintShovel: ItemFlintTool
    val flintSword: ItemFlintTool
    val flower: BlockType
    val forge: BlockType
    val fertilizer: Material
    val furnace: BlockType
    val grain: Material
    val glass: BlockType
    val grass: BlockType
    val grassBundle: Material
    val hyperBomb: BlockType
    val ingot: ItemIngot
    val lava: BlockType
    val leaves: BlockType
    val log: BlockType
    val meat: Material
    val metalAxe: ItemMetalTool
    val metalHammer: ItemMetalTool
    val metalHoe: ItemMetalTool
    val metalPickaxe: ItemMetalTool
    val metalSaw: ItemMetalTool
    val metalShovel: ItemMetalTool
    val metalSword: ItemMetalTool
    val mold: Material
    val oreBismuthinite: BlockType
    val oreCassiterite: BlockType
    val oreChalcocite: BlockType
    val oreChunk: Material
    val oreCoal: BlockType
    val oreGold: BlockType
    val oreMagnetite: BlockType
    val orePyrite: BlockType
    val oreSilver: BlockType
    val oreSphalerite: BlockType
    val quern: BlockType
    val researchTable: BlockType
    val sand: BlockType
    val sandstone: BlockType
    val sapling: BlockType
    val snow: BlockType
    val stick: Material
    val stoneRaw: BlockType
    val stoneTotem: BlockType
    val straw: BlockType
    val stoneRock: BlockType
    val seed: Material
    val string: Material
    val torch: BlockType
    val water: BlockType
    val wood: BlockType

    init {
        val treeRegistry = registry.get<TreeType>("VanillaBasics", "TreeType")
        val cropRegistry = registry.get<CropType>("VanillaBasics", "CropType")
        val stoneRegistry = registry.get<StoneType>("VanillaBasics",
                "StoneType")
        air = registry.air()
        anvil = register(BlockAnvil(this))
        alloy = register(BlockAlloy(this))
        baked = register(ItemBaked(this, cropRegistry))
        bedrock = register(BlockBedrock(this))
        bellows = register(BlockBellows(this))
        bloomery = register(BlockBloomery(this))
        brick = register(BlockBrick(this))
        bush = register(BlockBush(this))
        chest = register(BlockChest(this))
        coal = register(ItemCoal(this))
        cobblestone = register(BlockCobblestone(this, stoneRegistry))
        cobblestoneCracked = register(
                BlockCobblestoneCracked(this, stoneRegistry))
        cobblestoneMossy = register(BlockCobblestoneMossy(this, stoneRegistry))
        cookedMeat = register(ItemCookedMeat(this))
        craftingTable = register(BlockCraftingTable(this))
        crop = register(BlockCrop(this, cropRegistry))
        cropDrop = register(ItemCrop(this, cropRegistry))
        dirt = register(BlockDirt(this))
        dough = register(ItemDough(this, cropRegistry))
        farmland = register(BlockFarmland(this))
        flower = register(BlockFlower(this))
        forge = register(BlockForge(this))
        fertilizer = register(ItemFertilizer(this))
        furnace = register(BlockFurnace(this))
        grain = register(ItemGrain(this, cropRegistry))
        glass = register(BlockGlass(this))
        grass = register(BlockGrass(this, cropRegistry))
        grassBundle = register(ItemGrassBundle(this))
        hyperBomb = register(BlockHyperBomb(this))
        ingot = register(ItemIngot(this))
        lava = register(BlockLava(this))
        leaves = register(BlockLeaves(this, treeRegistry))
        log = register(BlockLog(this, treeRegistry))
        meat = register(ItemMeat(this))
        metalAxe = register(ItemMetalAxe(this))
        metalHammer = register(ItemMetalHammer(this))
        metalHoe = register(ItemMetalHoe(this))
        metalPickaxe = register(ItemMetalPickaxe(this))
        metalSaw = register(ItemMetalSaw(this))
        metalShovel = register(ItemMetalShovel(this))
        metalSword = register(ItemMetalSword(this))
        mold = register(ItemMold(this))
        oreBismuthinite = register(BlockOreBismuthinite(this, stoneRegistry))
        oreCassiterite = register(BlockOreCassiterite(this, stoneRegistry))
        oreChalcocite = register(BlockOreChalcocite(this, stoneRegistry))
        oreChunk = register(ItemOreChunk(this))
        oreCoal = register(BlockOreCoal(this, stoneRegistry))
        oreGold = register(BlockOreGold(this, stoneRegistry))
        oreMagnetite = register(BlockOreMagnetite(this, stoneRegistry))
        orePyrite = register(BlockOrePyrite(this, stoneRegistry))
        oreSilver = register(BlockOreSilver(this, stoneRegistry))
        oreSphalerite = register(BlockOreSphalerite(this, stoneRegistry))
        quern = register(BlockQuern(this))
        researchTable = register(BlockResearchTable(this))
        sand = register(BlockSand(this))
        sandstone = register(BlockSandstone(this))
        sapling = register(BlockSapling(this, treeRegistry))
        snow = register(BlockSnow(this))
        stick = register(ItemStick(this))
        straw = register(BlockStraw(this))
        flintAxe = register(ItemFlintAxe(this))
        flintHammer = register(ItemFlintHammer(this))
        flintHoe = register(ItemFlintHoe(this))
        flintPickaxe = register(ItemFlintPickaxe(this))
        stoneRaw = register(BlockStoneRaw(this, stoneRegistry))
        stoneRock = register(BlockStoneRock(this, stoneRegistry))
        stoneTotem = register(BlockStoneTotem(this, stoneRegistry))
        flintSaw = register(ItemFlintSaw(this))
        flintShovel = register(ItemFlintShovel(this))
        flintSword = register(ItemFlintSword(this))
        seed = register(ItemSeed(this, cropRegistry))
        string = register(ItemString(this))
        torch = register(BlockTorch(this))
        water = register(BlockWater(this))
        wood = register(BlockWood(this, treeRegistry))
    }

    private fun <M : Material> register(material: M): M {
        registry.registerMaterial(material)
        return material
    }
}
