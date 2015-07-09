/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.vanilla.basics.material;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.block.BlockLava;
import org.tobi29.scapes.vanilla.basics.material.block.BlockSnow;
import org.tobi29.scapes.vanilla.basics.material.block.BlockWater;
import org.tobi29.scapes.vanilla.basics.material.block.device.*;
import org.tobi29.scapes.vanilla.basics.material.block.rock.*;
import org.tobi29.scapes.vanilla.basics.material.block.soil.BlockDirt;
import org.tobi29.scapes.vanilla.basics.material.block.soil.BlockFarmland;
import org.tobi29.scapes.vanilla.basics.material.block.soil.BlockGrass;
import org.tobi29.scapes.vanilla.basics.material.block.soil.BlockSand;
import org.tobi29.scapes.vanilla.basics.material.block.structural.*;
import org.tobi29.scapes.vanilla.basics.material.block.vegetation.*;
import org.tobi29.scapes.vanilla.basics.material.item.*;
import org.tobi29.scapes.vanilla.basics.material.item.food.*;
import org.tobi29.scapes.vanilla.basics.material.item.tool.*;
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemCrop;
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemGrassBundle;
import org.tobi29.scapes.vanilla.basics.material.item.vegetation.ItemSeed;

public class VanillaMaterial {
    public final VanillaBasics plugin;
    public final GameRegistry registry;
    public final BlockType air;
    public final BlockType anvil;
    public final BlockType alloy;
    public final Material baked;
    public final BlockType bedrock;
    public final BlockType bellows;
    public final BlockType bloomery;
    public final BlockType brick;
    public final BlockType bush;
    public final BlockType chest;
    public final BlockType cobblestone;
    public final BlockType cobblestoneCracked;
    public final BlockType cobblestoneMossy;
    public final Material cookedMeat;
    public final BlockType craftingTable;
    public final BlockType crop;
    public final Material cropDrop;
    public final BlockType dirt;
    public final Material dough;
    public final BlockType farmland;
    public final BlockType flower;
    public final BlockType forge;
    public final Material fertilizer;
    public final BlockType furnace;
    public final Material grain;
    public final BlockType glass;
    public final BlockType grass;
    public final Material grassBundle;
    public final BlockType hyperBomb;
    public final ItemIngot ingot;
    public final BlockType lava;
    public final BlockType leaves;
    public final BlockType log;
    public final Material meat;
    public final Material mold;
    public final BlockType oreBismuthinite;
    public final BlockType oreCassiterite;
    public final BlockType oreChalcocite;
    public final Material oreChunk;
    public final BlockType oreCoal;
    public final BlockType oreGold;
    public final BlockType oreMagnetite;
    public final BlockType orePyrite;
    public final BlockType oreSilver;
    public final BlockType oreSphalerite;
    public final BlockType quern;
    public final BlockType researchTable;
    public final BlockType sand;
    public final BlockType sandstone;
    public final BlockType sapling;
    public final BlockType snow;
    public final Material stick;
    public final BlockType stoneRaw;
    public final BlockType stoneTotem;
    public final BlockType straw;
    public final Material axe;
    public final BlockType stoneRock;
    public final Material hammer;
    public final Material hoe;
    public final Material pickaxe;
    public final Material saw;
    public final Material seed;
    public final Material shovel;
    public final Material sword;
    public final Material string;
    public final BlockType torch;
    public final BlockType water;
    public final BlockType wood;

    public VanillaMaterial(VanillaBasics plugin, GameRegistry registry,
            GameRegistry.Registry<TreeType> treeRegistry,
            GameRegistry.Registry<CropType> cropRegistry,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        this.plugin = plugin;
        this.registry = registry;
        air = registry.getAir();
        anvil = register(new BlockAnvil(this));
        alloy = register(new BlockAlloy(this));
        baked = register(new ItemBaked(this, cropRegistry));
        bedrock = register(new BlockBedrock(this));
        bellows = register(new BlockBellows(this));
        bloomery = register(new BlockBloomery(this));
        brick = register(new BlockBrick(this));
        bush = register(new BlockBush(this));
        chest = register(new BlockChest(this));
        cobblestone = register(new BlockCobblestone(this, stoneRegistry));
        cobblestoneCracked =
                register(new BlockCobblestoneCracked(this, stoneRegistry));
        cobblestoneMossy =
                register(new BlockCobblestoneMossy(this, stoneRegistry));
        cookedMeat = register(new ItemCookedMeat(this));
        craftingTable = register(new BlockCraftingTable(this));
        crop = register(new BlockCrop(this, cropRegistry));
        cropDrop = register(new ItemCrop(this, cropRegistry));
        dirt = register(new BlockDirt(this));
        dough = register(new ItemDough(this, cropRegistry));
        farmland = register(new BlockFarmland(this));
        flower = register(new BlockFlower(this));
        forge = register(new BlockForge(this));
        fertilizer = register(new ItemFertilizer(this));
        furnace = register(new BlockFurnace(this));
        grain = register(new ItemGrain(this, cropRegistry));
        glass = register(new BlockGlass(this));
        grass = register(new BlockGrass(this, cropRegistry));
        grassBundle = register(new ItemGrassBundle(this));
        hyperBomb = register(new BlockHyperBomb(this));
        ingot = register(new ItemIngot(this));
        lava = register(new BlockLava(this));
        leaves = register(new BlockLeaves(this, treeRegistry));
        log = register(new BlockLog(this, treeRegistry));
        meat = register(new ItemMeat(this));
        mold = register(new ItemMold(this));
        oreBismuthinite =
                register(new BlockOreBismuthinite(this, stoneRegistry));
        oreCassiterite = register(new BlockOreCassiterite(this, stoneRegistry));
        oreChalcocite = register(new BlockOreChalcocite(this, stoneRegistry));
        oreChunk = register(new ItemOreChunk(this));
        oreCoal = register(new BlockOreCoal(this, stoneRegistry));
        oreGold = register(new BlockOreGold(this, stoneRegistry));
        oreMagnetite = register(new BlockOreMagnetite(this, stoneRegistry));
        orePyrite = register(new BlockOrePyrite(this, stoneRegistry));
        oreSilver = register(new BlockOreSilver(this, stoneRegistry));
        oreSphalerite = register(new BlockOreSphalerite(this, stoneRegistry));
        quern = register(new BlockQuern(this));
        researchTable = register(new BlockResearchTable(this));
        sand = register(new BlockSand(this));
        sandstone = register(new BlockSandstone(this));
        sapling = register(new BlockSapling(this, treeRegistry));
        snow = register(new BlockSnow(this));
        stick = register(new ItemStick(this));
        stoneRaw = register(new BlockStoneRaw(this, stoneRegistry));
        stoneTotem = register(new BlockStoneTotem(this, stoneRegistry));
        straw = register(new BlockStraw(this));
        axe = register(new ItemAxe(this));
        stoneRock = register(new BlockStoneRock(this, stoneRegistry));
        hammer = register(new ItemHammer(this));
        hoe = register(new ItemHoe(this));
        pickaxe = register(new ItemPickaxe(this));
        saw = register(new ItemSaw(this));
        seed = register(new ItemSeed(this, cropRegistry));
        shovel = register(new ItemShovel(this));
        sword = register(new ItemSword(this));
        string = register(new ItemString(this));
        torch = register(new BlockTorch(this));
        water = register(new BlockWater(this));
        wood = register(new BlockWood(this, treeRegistry));
    }

    private <M extends Material> M register(M material) {
        registry.registerMaterial(material);
        return material;
    }
}
