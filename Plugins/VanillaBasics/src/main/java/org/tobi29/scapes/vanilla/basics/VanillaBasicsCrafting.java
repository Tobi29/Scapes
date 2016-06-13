package org.tobi29.scapes.vanilla.basics;

import java8.util.stream.Collectors;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.*;
import org.tobi29.scapes.vanilla.basics.util.ToolUtil;

import java.util.Arrays;
import java.util.List;

class VanillaBasicsCrafting {
    static void registerRecipes(VanillaBasics plugin, GameRegistry registry) {
        GameRegistry.Registry<CropType> cropRegistry =
                registry.get("VanillaBasics", "CropType");
        GameRegistry.Registry<TreeType> treeRegistry =
                registry.get("VanillaBasics", "TreeType");
        GameRegistry.Registry<StoneType> stoneRegistry =
                registry.get("VanillaBasics", "StoneType");
        registerRecipesBasics(plugin, registry, treeRegistry, stoneRegistry);
        registerRecipesStone(plugin, registry);
        registerRecipesFood(plugin, registry, cropRegistry, stoneRegistry);
        registerRecipesMetal(plugin, registry, stoneRegistry);
        registerRecipesIron(plugin, registry, treeRegistry);
    }

    static void registerRecipesBasics(VanillaBasics plugin,
            GameRegistry registry, GameRegistry.Registry<TreeType> treeRegistry,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        VanillaMaterial materials = plugin.materials;
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String name() {
                return "Basics";
            }

            @Override
            public boolean table() {
                return false;
            }

            @Override
            public boolean availableFor(MobPlayerServer player) {
                return true;
            }

            @Override
            public boolean availableFor(MobPlayerClientMain player) {
                return true;
            }
        };
        List<ItemStack> hammer = Arrays.asList(materials.stoneHammer.example(1),
                materials.metalHammer.example(1));
        List<ItemStack> saw = Arrays.asList(materials.stoneSaw.example(1),
                materials.metalSaw.example(1));
        List<ItemStack> plank = Streams.of(treeRegistry.values())
                .map(tree -> new ItemStack(materials.wood, tree.data(registry)))
                .collect(Collectors.toList());

        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(plank);
            c.ingredients.add(hammer);
            c.requirements.add(saw);
            c.result = new ItemStack(materials.craftingTable, 0);
        }, "vanilla.basics.crafting.basics.CraftingTable");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(plank);
            c.requirements.add(saw);
            c.result = new ItemStack(materials.chest, 0);
        }, "vanilla.basics.crafting.basics.Chest");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(plank);
            c.ingredients.add(new ItemStack(materials.string, 1));
            c.requirements.add(saw);
            c.result = new ItemStack(materials.researchTable, 0);
        }, "vanilla.basics.crafting.basics.ResearchTable");
        Streams.of(stoneRegistry.values())
                .mapToInt(stoneType -> stoneType.data(registry))
                .forEach(data -> plugin.c.craftingRecipe(recipeType, c -> {
                    c.ingredients
                            .add(new ItemStack(materials.stoneRock, data, 9));
                    c.requirements.add(hammer);
                    c.result = new ItemStack(materials.cobblestone, data);
                }, "vanilla.basics.crafting.basics.Cobblestone" + data));
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.stick, 0));
            c.ingredients.add(new ItemStack(materials.coal, 0));
            c.result = new ItemStack(materials.torch, 0);
        }, "vanilla.basics.crafting.basics.Torch");

        plugin.addCraftingRecipe(recipeType);
    }

    static void registerRecipesStone(VanillaBasics plugin,
            GameRegistry registry) {
        VanillaMaterial materials = plugin.materials;
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String name() {
                return "Stone";
            }

            @Override
            public boolean table() {
                return false;
            }

            @Override
            public boolean availableFor(MobPlayerServer player) {
                return true;
            }

            @Override
            public boolean availableFor(MobPlayerClientMain player) {
                return true;
            }
        };
        ItemStack flint = new ItemStack(materials.stoneRock,
                StoneType.FLINT.data(registry));
        ItemStack hoe = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, hoe, "Hoe");
        ItemStack hoeHead = new ItemStack(hoe).setData(0);
        ItemStack hammer = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, hammer, "Hammer");
        ItemStack hammerHead = new ItemStack(hammer).setData(0);
        ItemStack saw = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, saw, "Saw");
        ItemStack sawHead = new ItemStack(saw).setData(0);
        ItemStack axe = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, axe, "Axe");
        ItemStack axeHead = new ItemStack(axe).setData(0);
        ItemStack shovel = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, shovel, "Shovel");
        ItemStack shovelHead = new ItemStack(shovel).setData(0);
        ItemStack pickaxe = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, pickaxe, "Pickaxe");
        ItemStack pickaxeHead = new ItemStack(pickaxe).setData(0);
        ItemStack sword = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, sword, "Sword");
        ItemStack swordHead = new ItemStack(sword).setData(0);

        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(flint);
            c.requirements.add(flint);
            c.result = hoeHead;
        }, "vanilla.basics.crafting.stone.HoeHead");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(flint);
            c.requirements.add(flint);
            c.result = hammerHead;
        }, "vanilla.basics.crafting.stone.HammerHead");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(flint);
            c.requirements.add(flint);
            c.result = sawHead;
        }, "vanilla.basics.crafting.stone.SawHead");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(flint);
            c.requirements.add(flint);
            c.result = axeHead;
        }, "vanilla.basics.crafting.stone.AxeHead");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(flint);
            c.requirements.add(flint);
            c.result = shovelHead;
        }, "vanilla.basics.crafting.stone.ShovelHead");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(flint);
            c.requirements.add(flint);
            c.result = pickaxeHead;
        }, "vanilla.basics.crafting.stone.PickaxeHead");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(flint);
            c.requirements.add(flint);
            c.result = swordHead;
        }, "vanilla.basics.crafting.stone.SwordHead");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(hoe);
            c.ingredients.add(hoeHead);
            c.ingredients.add(hammer);
            c.ingredients.add(hammerHead);
            c.ingredients.add(saw);
            c.ingredients.add(sawHead);
            c.ingredients.add(axe);
            c.ingredients.add(axeHead);
            c.ingredients.add(shovel);
            c.ingredients.add(shovelHead);
            c.ingredients.add(pickaxe);
            c.ingredients.add(pickaxeHead);
            c.ingredients.add(sword);
            c.ingredients.add(swordHead);
            c.result = flint;
        }, "vanilla.basics.crafting.stone.BreakTool");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.grassBundle, 0, 2));
            c.result = new ItemStack(materials.string, 0);
        }, "vanilla.basics.crafting.stone.String");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.string, 0, 8));
            c.result = new ItemStack(materials.string, 1);
        }, "vanilla.basics.crafting.stone.Fabric");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.grassBundle, 0, 2));
            c.result = new ItemStack(materials.straw, 0);
        }, "vanilla.basics.crafting.stone.WetStraw");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.grassBundle, 1, 2));
            c.result = new ItemStack(materials.straw, 1);
        }, "vanilla.basics.crafting.stone.Straw");

        plugin.addCraftingRecipe(recipeType);
    }

    static void registerRecipesFood(VanillaBasics plugin, GameRegistry registry,
            GameRegistry.Registry<CropType> cropRegistry,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        VanillaMaterial materials = plugin.materials;
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String name() {
                return "Food";
            }

            @Override
            public boolean table() {
                return true;
            }

            @Override
            public boolean availableFor(MobPlayerServer player) {
                return player.metaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Food");
            }

            @Override
            public boolean availableFor(MobPlayerClientMain player) {
                return player.metaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Food");
            }
        };
        List<ItemStack> cobblestones = Streams.of(stoneRegistry.values())
                .filter(stoneType -> stoneType.resistance() > 0.1)
                .mapToInt(stoneType -> stoneType.data(registry))
                .mapToObj(data -> new ItemStack(materials.cobblestone, data, 2))
                .collect(Collectors.toList());
        List<ItemStack> pickaxe =
                Arrays.asList(materials.stonePickaxe.example(1),
                        materials.metalPickaxe.example(1));
        List<ItemStack> hammer = Arrays.asList(materials.stoneHammer.example(1),
                materials.metalHammer.example(1));

        // TODO: Replace with oven
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(cobblestones);
            c.ingredients.add(new ItemStack(materials.stick, 0, 4));
            c.requirements.add(pickaxe);
            c.result = new ItemStack(materials.furnace, 0);
        }, "vanilla.basics.crafting.food.Furnace");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(cobblestones);
            c.requirements.add(hammer);
            c.result = new ItemStack(materials.quern, 0);
        }, "vanilla.basics.crafting.food.Quern");
        Streams.of(cropRegistry.values())
                .mapToInt(cropType -> cropType.data(registry))
                .forEach(data -> plugin.c.craftingRecipe(recipeType, c -> {
                    c.ingredients.add(new ItemStack(materials.grain, data, 8));
                    c.result = new ItemStack(materials.dough, data);
                }, "vanilla.basics.crafting.food.Dough" + data));
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(cobblestones);
            c.requirements.add(hammer);
            c.result = new ItemStack(materials.quern, 0);
        }, "vanilla.basics.crafting.");

        plugin.addCraftingRecipe(recipeType);
    }

    static void registerRecipesMetal(VanillaBasics plugin,
            GameRegistry registry,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        VanillaMaterial materials = plugin.materials;
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String name() {
                return "Metal";
            }

            @Override
            public boolean table() {
                return true;
            }

            @Override
            public boolean availableFor(MobPlayerServer player) {
                return player.metaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Metal");
            }

            @Override
            public boolean availableFor(MobPlayerClientMain player) {
                return player.metaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Metal");
            }
        };
        List<ItemStack> cobblestones = Streams.of(stoneRegistry.values())
                .filter(stoneType -> stoneType.resistance() > 0.1)
                .mapToInt(stoneType -> stoneType.data(registry))
                .mapToObj(data -> new ItemStack(materials.cobblestone, data, 2))
                .collect(Collectors.toList());

        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.sand, 2));
            c.result = new ItemStack(materials.mold, 0);
        }, "vanilla.basics.crafting.metal.Mold");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.ingot, 0, 5));
            c.result = new ItemStack(materials.anvil, 0);
        }, "vanilla.basics.crafting.metal.Anvil");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.coal, 0, 8));
            c.result = new ItemStack(materials.forge, 0);
        }, "vanilla.basics.crafting.metal.Forge");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(cobblestones);
            c.requirements.add(new ItemStack(materials.metalPickaxe, 1));
            c.result = new ItemStack(materials.alloy, 0);
        }, "vanilla.basics.crafting.metal.Alloy");

        plugin.addCraftingRecipe(recipeType);
    }

    static void registerRecipesIron(VanillaBasics plugin, GameRegistry registry,
            GameRegistry.Registry<TreeType> treeRegistry) {
        VanillaMaterial materials = plugin.materials;
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String name() {
                return "Iron";
            }

            @Override
            public boolean table() {
                return true;
            }

            @Override
            public boolean availableFor(MobPlayerServer player) {
                return player.metaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Iron");
            }

            @Override
            public boolean availableFor(MobPlayerClientMain player) {
                return player.metaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Iron");
            }
        };
        List<ItemStack> plank = Streams.of(treeRegistry.values())
                .map(tree -> new ItemStack(materials.wood, tree.data(registry)))
                .collect(Collectors.toList());

        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(new ItemStack(materials.sand, 2));
            c.result = new ItemStack(materials.bloomery, 0);
        }, "vanilla.basics.crafting.iron.Bloomery");
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(plank);
            c.ingredients.add(new ItemStack(materials.string, 1, 4));
            c.requirements.add(materials.metalSaw.example(1));
            c.result = new ItemStack(materials.bellows, 0);
        }, "vanilla.basics.crafting.iron.Bellows");

        plugin.addCraftingRecipe(recipeType);
    }
}
