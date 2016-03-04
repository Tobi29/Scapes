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
package org.tobi29.scapes.vanilla.basics;

import java8.util.Optional;
import java8.util.stream.Collectors;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.Update;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.vanilla.basics.entity.client.*;
import org.tobi29.scapes.vanilla.basics.entity.server.*;
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator;
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldServer;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerGround;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerPatch;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerRock;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerTree;
import org.tobi29.scapes.vanilla.basics.generator.tree.*;
import org.tobi29.scapes.vanilla.basics.material.*;
import org.tobi29.scapes.vanilla.basics.material.update.*;
import org.tobi29.scapes.vanilla.basics.packet.*;
import org.tobi29.scapes.vanilla.basics.util.IngotUtil;
import org.tobi29.scapes.vanilla.basics.util.ToolUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class VanillaBasicsRegisters {
    static void registerCommands(ScapesServer server, VanillaBasics plugin) {
        VanillaMaterial materials = plugin.getMaterials();
        CommandRegistry registry = server.commandRegistry();
        ServerConnection connection = server.connection();

        registry.register("time", 8, options -> {
            options.add("w", "world", true, "World that is targeted");
            options.add("d", "day", true, "Day that time will be set to");
            options.add("t", "time", true,
                    "Time of day that time will be set to");
            options.add("r", "relative", false,
                    "Add time instead of setting it");
        }, (args, executor, commands) -> {
            String worldName = args.requireOption('w');
            boolean relative = args.hasOption('r');
            Optional<String> dayOption = args.option('d');
            if (dayOption.isPresent()) {
                long day = Command.getLong(dayOption.get());
                commands.add(() -> {
                    WorldServer world =
                            Command.require(server::world, worldName);
                    EnvironmentServer environment = world.environment();
                    if (environment instanceof EnvironmentOverworldServer) {
                        EnvironmentOverworldServer environmentOverworld =
                                (EnvironmentOverworldServer) environment;
                        ClimateGenerator climateGenerator =
                                environmentOverworld.climate();
                        climateGenerator.setDay(day +
                                (relative ? climateGenerator.day() : 0));
                    } else {
                        throw new Command.CommandException(20,
                                "Unsupported environment");
                    }
                });
            }
            Optional<String> dayTimeOption = args.option('t');
            if (dayTimeOption.isPresent()) {
                float dayTime = Command.getFloat(dayTimeOption.get());
                commands.add(() -> {
                    WorldServer world =
                            Command.require(server::world, worldName);
                    EnvironmentServer environment = world.environment();
                    if (environment instanceof EnvironmentOverworldServer) {
                        EnvironmentOverworldServer environmentOverworld =
                                (EnvironmentOverworldServer) environment;
                        ClimateGenerator climateGenerator =
                                environmentOverworld.climate();
                        climateGenerator.setDayTime(dayTime +
                                (relative ? climateGenerator.dayTime() : 0.0f));
                    } else {
                        throw new Command.CommandException(20,
                                "Unsupported environment");
                    }
                });
            }
        });

        registry.register("hunger", 8, options -> {
            options.add("p", "player", true,
                    "Player whose hunger values will be changed");
            options.add("w", "wake", true, "Wake value (0.0-1.0)");
            options.add("s", "saturation", true, "Saturation value (0.0-1.0)");
            options.add("t", "thirst", true, "Thirst value (0.0-1.0)");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
            Optional<String> wakeOption = args.option('w');
            if (wakeOption.isPresent()) {
                double wake = Command.getDouble(wakeOption.get());
                commands.add(() -> {
                    PlayerConnection player =
                            Command.require(connection::playerByName,
                                    playerName);
                    TagStructure conditionTag = player.mob().metaData("Vanilla")
                            .getStructure("Condition");
                    synchronized (conditionTag) {
                        conditionTag.setDouble("Wake", wake);
                    }
                });
            }
            Optional<String> saturationOption = args.option('s');
            if (saturationOption.isPresent()) {
                double saturation = Command.getDouble(saturationOption.get());
                commands.add(() -> {
                    PlayerConnection player =
                            Command.require(connection::playerByName,
                                    playerName);
                    TagStructure conditionTag = player.mob().metaData("Vanilla")
                            .getStructure("Condition");
                    synchronized (conditionTag) {
                        conditionTag.setDouble("Hunger", saturation);
                    }
                });
            }
            Optional<String> thirstOption = args.option('t');
            if (thirstOption.isPresent()) {
                double thirst = Command.getDouble(thirstOption.get());
                commands.add(() -> {
                    PlayerConnection player =
                            Command.require(connection::playerByName,
                                    playerName);
                    TagStructure conditionTag = player.mob().metaData("Vanilla")
                            .getStructure("Condition");
                    synchronized (conditionTag) {
                        conditionTag.setDouble("Thirst", thirst);
                    }
                });
            }
        });

        registry.register("giveingot", 8, options -> {
            options.add("p", "player", true,
                    "Player that the item will be given to");
            options.add("m", "metal", true, "Metal type");
            options.add("d", "data", true, "Data value of item");
            options.add("a", "amount", true, "Amount of item in stack");
            options.add("t", "temperature", true, "Temperature of metal");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
            String metal = args.requireOption('m');
            int data = Command.getInt(args.option('d', "0"));
            int amount = Command.getInt(args.option('a', "1"));
            float temperature = Command.getFloat(args.option('t', "0.0"));
            commands.add(() -> {
                PlayerConnection player =
                        Command.require(connection::playerByName, playerName);
                MetalType metalType = plugin.metalType(metal);
                ItemStack item = new ItemStack(materials.ingot, data, amount);
                IngotUtil.createIngot(item, metalType, temperature);
                player.mob().inventories()
                        .modify("Container", inventory -> inventory.add(item));
            });
        });

        registry.register("givetool", 8, options -> {
            options.add("p", "player", true,
                    "Player that the item will be given to");
            options.add("m", "metal", true, "Metal type");
            options.add("d", "data", true, "Data value of item");
            options.add("a", "amount", true, "Amount of item in stack");
            options.add("t", "temperature", true, "Temperature of metal");
            options.add("k", "kind", true, "Kind of tool");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
            String metal = args.requireOption('m');
            String kind = args.requireOption('k');
            int data = Command.getInt(args.option('d', "0"));
            int amount = Command.getInt(args.option('a', "1"));
            float temperature = Command.getFloat(args.option('t', "0.0"));
            commands.add(() -> {
                PlayerConnection player =
                        Command.require(connection::playerByName, playerName);
                MetalType metalType = plugin.metalType(metal);
                ItemStack item = new ItemStack(materials.ingot, data, amount);
                IngotUtil.createIngot(item, metalType, temperature);
                if (!ToolUtil.createTool(plugin, item, kind)) {
                    Command.error("Unknown tool kind: " + kind);
                }
                player.mob().inventories()
                        .modify("Container", inventory -> inventory.add(item));
            });
        });

        CommandRegistry worldGroup = registry.group("world");

        worldGroup.register("new NAME", 9, options -> {
        }, (args, executor, commands) -> {
            String name = Command.require(args.arg(0), "name");
            commands.add(() -> server.registerWorld(
                    world -> new EnvironmentOverworldServer(world, plugin),
                    name, StringUtil.hash(name, server.seed())));
        });

        worldGroup.register("remove NAME", 9, options -> {
        }, (args, executor, commands) -> {
            String name = Command.require(args.arg(0), "name");
            commands.add(() -> {
                if (!server.removeWorld(name)) {
                    Command.error("World not loaded: " + name);
                }
            });
        });

        worldGroup.register("delete NAME", 9, options -> {
        }, (args, executor, commands) -> {
            String name = Command.require(args.arg(0), "name");
            commands.add(() -> server.deleteWorld(name));
        });
    }

    static void registerEntities(GameRegistry registry) {
        GameRegistry.AsymSupplierRegistry<WorldServer, EntityServer, WorldClient, EntityClient>
                er = registry.getAsymSupplier("Core", "Entity");
        er.reg(MobPigServer::new, MobPigClient::new, MobPigServer.class,
                "vanilla.basics.mob.Pig");
        er.reg(MobZombieServer::new, MobZombieClient::new,
                MobZombieServer.class, "vanilla.basics.mob.Zombie");
        er.reg(MobSkeletonServer::new, MobSkeletonClient::new,
                MobSkeletonServer.class, "vanilla.basics.mob.Skeleton");
        er.reg(EntityTornadoServer::new, EntityTornadoClient::new,
                EntityTornadoServer.class, "vanilla.basics.entity.Tornado");
        er.reg(EntityAlloyServer::new, EntityAlloyClient::new,
                EntityAlloyServer.class, "vanilla.basics.entity.Alloy");
        er.reg(EntityAnvilServer::new, EntityAnvilClient::new,
                EntityAnvilServer.class, "vanilla.basics.entity.Anvil");
        er.reg(EntityBellowsServer::new, EntityBellowsClient::new,
                EntityBellowsServer.class, "vanilla.basics.entity.Bellows");
        er.reg(EntityBloomeryServer::new, EntityBloomeryClient::new,
                EntityBloomeryServer.class, "vanilla.basics.entity.Bloomery");
        er.reg(EntityChestServer::new, EntityChestClient::new,
                EntityChestServer.class, "vanilla.basics.entity.Chest");
        er.reg(EntityForgeServer::new, EntityForgeClient::new,
                EntityForgeServer.class, "vanilla.basics.entity.Forge");
        er.reg(EntityFurnaceServer::new, EntityFurnaceClient::new,
                EntityFurnaceServer.class, "vanilla.basics.entity.Furnace");
        er.reg(EntityQuernServer::new, EntityQuernClient::new,
                EntityQuernServer.class, "vanilla.basics.entity.Quern");
        er.reg(EntityResearchTableServer::new, EntityResearchTableClient::new,
                EntityResearchTableServer.class,
                "vanilla.basics.entity.ResearchTable");
        er.reg(EntityFarmlandServer::new, EntityFarmlandClient::new,
                EntityFarmlandServer.class, "vanilla.basics.entity.Farmland");
    }

    static void registerUpdates(GameRegistry registry) {
        GameRegistry.SupplierRegistry<GameRegistry, Update> ur =
                registry.getSupplier("Core", "Update");
        ur.regS(UpdateWaterFlow::new, "vanilla.basics.update.WaterFlow");
        ur.regS(UpdateLavaFlow::new, "vanilla.basics.update.LavaFlow");
        ur.regS(UpdateGrassGrowth::new, "vanilla.basics.update.GrassGrowth");
        ur.regS(UpdateFlowerGrowth::new, "vanilla.basics.update.FlowerGrowth");
        ur.regS(UpdateSaplingGrowth::new,
                "vanilla.basics.update.SaplingGrowth");
        ur.regS(UpdateStrawDry::new, "vanilla.basics.update.StrawDry");
    }

    static void registerPackets(GameRegistry registry) {
        GameRegistry.SupplierRegistry<GameRegistry, Packet> pr =
                registry.getSupplier("Core", "Packet");
        pr.regS(PacketDayTimeSync::new, "vanilla.basics.packet.DayTimeSync");
        pr.regS(PacketCrafting::new, "vanilla.basics.packet.Crafting");
        pr.regS(PacketOpenCrafting::new, "vanilla.basics.packet.OpenCrafting");
        pr.regS(PacketAnvil::new, "vanilla.basics.packet.Anvil");
        pr.regS(PacketLightning::new, "vanilla.basics.packet.Lightning");
        pr.regS(PacketNotification::new, "vanilla.basics.packet.Notification");
        pr.regS(PacketResearch::new, "vanilla.basics.packet.Research");
        pr.regS(PacketQuern::new, "vanilla.basics.packet.Quern");
    }

    static void registerRecipes(VanillaBasics plugin, GameRegistry registry) {
        GameRegistry.Registry<CropType> cropRegistry =
                registry.<CropType>get("VanillaBasics", "CropType");
        GameRegistry.Registry<TreeType> treeRegistry =
                registry.<TreeType>get("VanillaBasics", "TreeType");
        GameRegistry.Registry<StoneType> stoneRegistry =
                registry.<StoneType>get("VanillaBasics", "StoneType");
        registerRecipesBasics(plugin, registry, treeRegistry, stoneRegistry);
        registerRecipesStone(plugin, registry);
        registerRecipesFood(plugin, cropRegistry, stoneRegistry);
        registerRecipesMetal(plugin, stoneRegistry);
        registerRecipesIron(plugin);
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
        CraftingRecipe.Ingredient hammer = new CraftingRecipe.IngredientList(
                materials.stoneHammer.example(1),
                materials.metalHammer.example(1));
        CraftingRecipe.Ingredient saw =
                new CraftingRecipe.IngredientList(materials.stoneSaw.example(1),
                        materials.metalSaw.example(1));
        CraftingRecipe.Ingredient plank = new CraftingRecipe.IngredientList(
                Streams.of(treeRegistry.values())
                        .map(tree -> new ItemStack(materials.wood,
                                tree.data(registry)))
                        .collect(Collectors.toList()));
        List<StoneType> stoneTypes = stoneRegistry.values();

        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(plank);
            c.ingredients.add(hammer);
            c.requirements.add(saw);
            c.result = new ItemStack(materials.craftingTable, 0);
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(plank);
            c.requirements.add(saw);
            c.result = new ItemStack(materials.chest, 0);
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients.add(plank);
            c.ingredient(new ItemStack(materials.string, 1));
            c.requirements.add(saw);
            c.result = new ItemStack(materials.researchTable, 0);
        });
        for (StoneType stoneType : stoneTypes) {
            int data = stoneType.data(registry);
            recipeType.recipes().add(new CraftingRecipe(
                    new ItemStack(materials.cobblestone, data),
                    new CraftingRecipe.IngredientList(
                            new ItemStack(materials.stoneRock, data, 9))));
        }
        recipeType.recipes().add(new CraftingRecipe(
                new ItemStack(materials.torch, (short) 0),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.stick, (short) 0)),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.coal, (short) 0))));

        plugin.registerCraftingRecipe(recipeType);
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
        ItemStack hoeItem = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, hoeItem, "Hoe");
        ItemStack hoeHeadItem = new ItemStack(hoeItem).setData(0);
        ItemStack hammerItem = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, hammerItem, "Hammer");
        ItemStack hammerHeadItem = new ItemStack(hammerItem).setData(0);
        ItemStack sawItem = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, sawItem, "Saw");
        ItemStack sawHeadItem = new ItemStack(sawItem).setData(0);
        ItemStack axeItem = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, axeItem, "Axe");
        ItemStack axeHeadItem = new ItemStack(axeItem).setData(0);
        ItemStack shovelItem = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, shovelItem, "Shovel");
        ItemStack shovelHeadItem = new ItemStack(shovelItem).setData(0);
        ItemStack pickaxeItem = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, pickaxeItem, "Pickaxe");
        ItemStack pickaxeHeadItem = new ItemStack(pickaxeItem).setData(0);
        ItemStack swordItem = new ItemStack(flint);
        ToolUtil.createStoneTool(materials.plugin, swordItem, "Sword");
        ItemStack swordHeadItem = new ItemStack(swordItem).setData(0);

        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(flint);
            c.requirement(flint);
            c.result = hoeHeadItem;
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(flint);
            c.requirement(flint);
            c.result = hammerHeadItem;
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(flint);
            c.requirement(flint);
            c.result = sawHeadItem;
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(flint);
            c.requirement(flint);
            c.result = axeHeadItem;
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(flint);
            c.requirement(flint);
            c.result = shovelHeadItem;
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(flint);
            c.requirement(flint);
            c.result = pickaxeHeadItem;
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(flint);
            c.requirement(flint);
            c.result = swordHeadItem;
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredients(i -> {
                i.add(hoeItem);
                i.add(hoeHeadItem);
                i.add(hammerItem);
                i.add(hammerHeadItem);
                i.add(sawItem);
                i.add(sawHeadItem);
                i.add(axeItem);
                i.add(axeHeadItem);
                i.add(shovelItem);
                i.add(shovelHeadItem);
                i.add(pickaxeItem);
                i.add(pickaxeHeadItem);
                i.add(swordItem);
                i.add(swordHeadItem);
            });
            c.result = flint;
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(new ItemStack(materials.grassBundle, 0, 2));
            c.result = new ItemStack(materials.string, 0);
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(new ItemStack(materials.string, 0, 8));
            c.result = new ItemStack(materials.string, 1);
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(new ItemStack(materials.grassBundle, 0, 2));
            c.result = new ItemStack(materials.straw, 0);
        });
        plugin.c.craftingRecipe(recipeType, c -> {
            c.ingredient(new ItemStack(materials.grassBundle, 1, 2));
            c.result = new ItemStack(materials.straw, 1);
        });

        plugin.registerCraftingRecipe(recipeType);
    }

    static void registerRecipesFood(VanillaBasics plugin,
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
        List<StoneType> stoneTypes = stoneRegistry.values();
        List<ItemStack> cobblestoneItems = new ArrayList<>();
        for (int i = 0; i < stoneTypes.size(); i++) {
            if (stoneTypes.get(i).resistance() > 0.1) {
                cobblestoneItems
                        .add(new ItemStack(materials.cobblestone, i, 2));
            }
        }
        List<CropType> cropTypes = cropRegistry.values();
        CraftingRecipe.Ingredient cobblestones =
                new CraftingRecipe.IngredientList(cobblestoneItems);
        CraftingRecipe.Ingredient pickaxe = new CraftingRecipe.IngredientList(
                new ItemStack(materials.metalPickaxe, (short) 1));
        CraftingRecipe.Ingredient hammer = new CraftingRecipe.IngredientList(
                new ItemStack(materials.metalHammer, (short) 1));

        recipeType.recipes().add(new CraftingRecipe(Arrays.asList(cobblestones,
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.stick, (short) 0, 4))),
                Collections.singletonList(pickaxe),
                new ItemStack(materials.furnace, (short) 0)));
        recipeType.recipes()
                .add(new CraftingRecipe(Collections.singletonList(cobblestones),
                        Collections.singletonList(hammer),
                        new ItemStack(materials.quern, (short) 0)));
        for (int i = 0; i < cropTypes.size(); i++) {
            recipeType.recipes().add(new CraftingRecipe(Collections
                    .singletonList(new CraftingRecipe.IngredientList(
                            new ItemStack(materials.grain, i, 8))),
                    new ItemStack(materials.dough, i)));
        }

        plugin.registerCraftingRecipe(recipeType);
    }

    static void registerRecipesMetal(VanillaBasics plugin,
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
        List<StoneType> stoneTypes = stoneRegistry.values();
        List<ItemStack> cobblestoneItems = new ArrayList<>();
        for (int i = 0; i < stoneTypes.size(); i++) {
            if (stoneTypes.get(i).resistance() > 0.1) {
                cobblestoneItems
                        .add(new ItemStack(materials.cobblestone, i, 2));
            }
        }
        CraftingRecipe.Ingredient cobblestones =
                new CraftingRecipe.IngredientList(cobblestoneItems);
        CraftingRecipe.Ingredient ingot = new CraftingRecipe.IngredientList(
                new ItemStack(materials.ingot, (short) 0, 5));
        CraftingRecipe.Ingredient pickaxe = new CraftingRecipe.IngredientList(
                new ItemStack(materials.metalPickaxe, (short) 1));
        recipeType.recipes().add(new CraftingRecipe(
                new ItemStack(materials.mold, (short) 0),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.sand, (short) 2))));
        recipeType.recipes().add(new CraftingRecipe(
                new ItemStack(materials.anvil, (short) 0), ingot));
        recipeType.recipes().add(new CraftingRecipe(
                new ItemStack(materials.forge, (short) 0),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.coal, (short) 0, 8))));
        recipeType.recipes()
                .add(new CraftingRecipe(Collections.singletonList(cobblestones),
                        Collections.singletonList(pickaxe),
                        new ItemStack(materials.alloy, (short) 0)));

        plugin.registerCraftingRecipe(recipeType);
    }

    static void registerRecipesIron(VanillaBasics plugin) {
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
        CraftingRecipe.Ingredient plank = new CraftingRecipe.IngredientList(
                new ItemStack(materials.wood, (short) 0, 1),
                new ItemStack(materials.wood, (short) 1, 1),
                new ItemStack(materials.wood, (short) 2, 1));

        recipeType.recipes().add(new CraftingRecipe(
                new ItemStack(materials.bloomery, (short) 0),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.sand, (short) 2))));
        recipeType.recipes().add(new CraftingRecipe(Arrays.asList(plank,
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.string, (short) 1, 4))),
                Collections.singletonList(new CraftingRecipe.IngredientList(
                        new ItemStack(materials.metalSaw, (short) 1))),
                new ItemStack(materials.bellows, (short) 0)));

        plugin.registerCraftingRecipe(recipeType);
    }

    static void registerResearch(VanillaBasics plugin) {
        plugin.c.research("Food",
                "Using a Quern, you can\ncreate grain out of this.",
                "vanilla.basics.item.Crop");
        plugin.c.research("Food", "Try making dough out of this?",
                "vanilla.basics.item.Grain");
        plugin.c.research("Metal",
                "You could try heating this on\na forge and let it melt\ninto a ceramic mold.\nMaybe you can find a way\nto shape it, to create\nhandy tools?",
                "vanilla.basics.item.OreChunk.Chalcocite");
        plugin.c.research("Iron",
                "Maybe you can figure out\na way to create a strong\nmetal out of this ore...",
                "vanilla.basics.item.OreChunk.Magnetite");
    }

    static GameRegistry.Registry<TreeType> registerTreeTypes(
            GameRegistry registry) {
        GameRegistry.Registry<TreeType> treeRegistry =
                registry.add("VanillaBasics", "TreeType", 0, Short.MAX_VALUE);
        treeRegistry.reg(TreeType.OAK, "vanilla.basics.tree.Oak");
        treeRegistry.reg(TreeType.BIRCH, "vanilla.basics.tree.Birch");
        treeRegistry.reg(TreeType.SPRUCE, "vanilla.basics.tree.Spruce");
        treeRegistry.reg(TreeType.PALM, "vanilla.basics.tree.Palm");
        treeRegistry.reg(TreeType.MAPLE, "vanilla.basics.tree.Maple");
        treeRegistry.reg(TreeType.SEQUOIA, "vanilla.basics.tree.Sequoia");
        treeRegistry.reg(TreeType.WILLOW, "vanilla.basics.tree.Willow");
        return treeRegistry;
    }

    static GameRegistry.Registry<CropType> registerCropTypes(
            GameRegistry registry) {
        GameRegistry.Registry<CropType> cropRegistry =
                registry.add("VanillaBasics", "CropType", 0, Short.MAX_VALUE);
        cropRegistry.reg(CropType.WHEAT, "vanilla.basics.crop.Wheat");
        return cropRegistry;
    }

    static GameRegistry.Registry<StoneType> registerStoneTypes(
            GameRegistry registry) {
        GameRegistry.Registry<StoneType> stoneRegistry =
                registry.add("VanillaBasics", "StoneType", 0, Short.MAX_VALUE);
        stoneRegistry
                .reg(StoneType.DIRT_STONE, "vanilla.basics.stone.DirtStone");
        stoneRegistry.reg(StoneType.FLINT, "vanilla.basics.stone.Flint");
        stoneRegistry.reg(StoneType.CHALK, "vanilla.basics.stone.Chalk");
        stoneRegistry.reg(StoneType.CHERT, "vanilla.basics.stone.Chert");
        stoneRegistry
                .reg(StoneType.CLAYSTONE, "vanilla.basics.stone.Claystone");
        stoneRegistry.reg(StoneType.CONGLOMERATE,
                "vanilla.basics.stone.Conglomerate");
        stoneRegistry.reg(StoneType.MARBLE, "vanilla.basics.stone.Marble");
        stoneRegistry.reg(StoneType.ANDESITE, "vanilla.basics.stone.Andesite");
        stoneRegistry.reg(StoneType.BASALT, "vanilla.basics.stone.Basalt");
        stoneRegistry.reg(StoneType.DACITE, "vanilla.basics.stone.Dacite");
        stoneRegistry.reg(StoneType.RHYOLITE, "vanilla.basics.stone.Rhyolite");
        stoneRegistry.reg(StoneType.DIORITE, "vanilla.basics.stone.Diorite");
        stoneRegistry.reg(StoneType.GABBRO, "vanilla.basics.stone.Gabbro");
        stoneRegistry.reg(StoneType.GRANITE, "vanilla.basics.stone.Granite");
        return stoneRegistry;
    }

    static void registerOres(VanillaBasics plugin) {
        VanillaMaterial materials = plugin.getMaterials();
        plugin.c.ore(o -> {
            o.type = materials.oreCoal;
            o.rarity = 9;
            o.size = 16.0;
            o.chance = 3;
            o.rockChance = 20;
            o.rockDistance = 16;
            o.stoneTypes.add(StoneType.DIRT_STONE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreCoal;
            o.rarity = 3;
            o.size = 12.0;
            o.chance = 3;
            o.rockChance = 20;
            o.rockDistance = 32;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
            o.stoneTypes.add(StoneType.MARBLE);
            o.stoneTypes.add(StoneType.ANDESITE);
            o.stoneTypes.add(StoneType.BASALT);
            o.stoneTypes.add(StoneType.DACITE);
            o.stoneTypes.add(StoneType.RHYOLITE);
            o.stoneTypes.add(StoneType.DIORITE);
            o.stoneTypes.add(StoneType.GABBRO);
            o.stoneTypes.add(StoneType.GRANITE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreCassiterite;
            o.rarity = 2;
            o.size = 24.0;
            o.chance = 64;
            o.rockChance = 12;
            o.rockDistance = 32;
            o.stoneTypes.add(StoneType.ANDESITE);
            o.stoneTypes.add(StoneType.BASALT);
            o.stoneTypes.add(StoneType.DACITE);
            o.stoneTypes.add(StoneType.RHYOLITE);
            o.stoneTypes.add(StoneType.GRANITE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreSphalerite;
            o.rarity = 6;
            o.size = 6.0;
            o.chance = 3;
            o.rockChance = 4;
            o.rockDistance = 16;
            o.stoneTypes.add(StoneType.MARBLE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreBismuthinite;
            o.rarity = 2;
            o.size = 3.0;
            o.chance = 8;
            o.rockChance = 9;
            o.rockDistance = 128;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
            o.stoneTypes.add(StoneType.MARBLE);
            o.stoneTypes.add(StoneType.DIORITE);
            o.stoneTypes.add(StoneType.GABBRO);
            o.stoneTypes.add(StoneType.GRANITE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreChalcocite;
            o.rarity = 12;
            o.size = 8.0;
            o.chance = 2;
            o.rockChance = 1;
            o.rockDistance = 24;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreMagnetite;
            o.rarity = 4;
            o.size = 64.0;
            o.chance = 12;
            o.rockChance = 10;
            o.rockDistance = 96;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
        });
        plugin.c.ore(o -> {
            o.type = materials.orePyrite;
            o.rarity = 3;
            o.size = 4.0;
            o.chance = 11;
            o.rockChance = 13;
            o.rockDistance = 48;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
            o.stoneTypes.add(StoneType.MARBLE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreSilver;
            o.rarity = 2;
            o.size = 3.0;
            o.chance = 4;
            o.rockChance = 8;
            o.rockDistance = 64;
            o.stoneTypes.add(StoneType.GRANITE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreGold;
            o.rarity = 1;
            o.size = 2.0;
            o.chance = 4;
            o.rockChance = 96;
            o.rockDistance = 256;
            o.stoneTypes.add(StoneType.ANDESITE);
            o.stoneTypes.add(StoneType.BASALT);
            o.stoneTypes.add(StoneType.DACITE);
            o.stoneTypes.add(StoneType.RHYOLITE);
            o.stoneTypes.add(StoneType.DIORITE);
            o.stoneTypes.add(StoneType.GABBRO);
            o.stoneTypes.add(StoneType.GRANITE);
        });
    }

    static void registerMetals(VanillaBasics plugin) {
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Tin";
            m.meltingPoint = 231.0;
            m.toolEfficiency = 0.1;
            m.toolStrength = 2.0;
            m.toolDamage = 0.01;
            m.toolLevel = 10;
            m.r = 1.0f;
            m.g = 1.0f;
            m.b = 1.0f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Zinc";
            m.meltingPoint = 419.0;
            m.toolEfficiency = 1.0;
            m.toolStrength = 4.0;
            m.toolDamage = 0.004;
            m.toolLevel = 10;
            m.r = 1.0f;
            m.g = 0.9f;
            m.b = 0.9f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Bismuth";
            m.meltingPoint = 271.0;
            m.toolEfficiency = 1.0;
            m.toolStrength = 4.0;
            m.toolDamage = 0.004;
            m.toolLevel = 10;
            m.r = 0.8f;
            m.g = 0.9f;
            m.b = 0.9f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Copper";
            m.meltingPoint = 1084.0;
            m.toolEfficiency = 6.0;
            m.toolStrength = 8.0;
            m.toolDamage = 0.001;
            m.toolLevel = 20;
            m.r = 0.8f;
            m.g = 0.2f;
            m.b = 0.0f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Iron";
            m.meltingPoint = 1538.0;
            m.toolEfficiency = 30.0;
            m.toolStrength = 12.0;
            m.toolDamage = 0.0001;
            m.toolLevel = 40;
            m.r = 0.7f;
            m.g = 0.7f;
            m.b = 0.7f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Silver";
            m.meltingPoint = 961.0;
            m.toolEfficiency = 1.0;
            m.toolStrength = 4.0;
            m.toolDamage = 0.004;
            m.toolLevel = 10;
            m.r = 0.9f;
            m.g = 0.9f;
            m.b = 1.0f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Gold";
            m.meltingPoint = 1064.0;
            m.toolEfficiency = 0.1;
            m.toolStrength = 2.0;
            m.toolDamage = 0.01;
            m.toolLevel = 10;
            m.r = 0.9f;
            m.g = 0.9f;
            m.b = 1.0f;
        });
        plugin.c.alloy(m -> {
            m.id = m.name = m.ingotName = "Bronze";
            m.toolEfficiency = 10.0;
            m.toolStrength = 10.0;
            m.toolDamage = 0.0005;
            m.toolLevel = 20;
            m.r = 0.6f;
            m.g = 0.4f;
            m.b = 0.0f;
            m.ingredients.put("Tin", 0.25);
            m.ingredients.put("Copper", 0.75);
        });
        plugin.c.alloy(m -> {
            m.name = m.ingotName = "Bismuth Bronze";
            m.id = "BismuthBronze";
            m.toolEfficiency = 10.0;
            m.toolStrength = 10.0;
            m.toolDamage = 0.0005;
            m.toolLevel = 20;
            m.r = 0.6f;
            m.g = 0.4f;
            m.b = 0.0f;
            m.ingredients.put("Bismuth", 0.2);
            m.ingredients.put("Zinc", 0.2);
            m.ingredients.put("Copper", 0.6);
        });
    }

    @SuppressWarnings("CodeBlock2Expr")
    static void registerVegetation(VanillaBasics plugin) {
        VanillaMaterial m = plugin.getMaterials();
        // Overlays
        plugin.c.decorator("Rocks", d -> {
            d.addLayer(new LayerRock(m.stoneRock, m.stoneRaw, 256,
                    (terrain, x, y, z) ->
                            terrain.type(x, y, z - 1) == m.grass &&
                                    terrain.type(x, y, z) == m.air));
        });
        plugin.c.decorator("Flint", d -> {
            int data = StoneType.FLINT.data(m.registry);
            d.addLayer(new LayerGround(m.stoneRock,
                    (terrain, x, y, z, random) -> data, 1024,
                    (terrain, x, y, z) ->
                            terrain.type(x, y, z - 1) == m.grass &&
                                    terrain.type(x, y, z) == m.air));
        });
        plugin.c.decorator("Gravel", d -> {
            int data = StoneType.DIRT_STONE.data(m.registry);
            d.addLayer(new LayerRock(m.stoneRock, m.stoneRaw, 8,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) == m.sand &&
                            terrain.data(x, y, z - 1) == 1 &&
                            terrain.type(x, y, z) == m.air));
            d.addLayer(new LayerGround(m.stoneRock,
                    (terrain, x, y, z, random) -> data, 4,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) == m.sand &&
                            terrain.data(x, y, z - 1) == 1 &&
                            terrain.type(x, y, z) == m.air));
        });

        // Polar
        plugin.c.decorator(BiomeGenerator.Biome.POLAR, "Waste", 10, d -> {
        });

        // Tundra
        plugin.c.decorator(BiomeGenerator.Biome.TUNDRA, "Waste", 10, d -> {
        });
        plugin.c.decorator(BiomeGenerator.Biome.TUNDRA, "Spruce", 1, d -> {
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 256));
        });

        // Taiga
        plugin.c.decorator(BiomeGenerator.Biome.TAIGA, "Spruce", 10, d -> {
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 32));
        });
        plugin.c.decorator(BiomeGenerator.Biome.TAIGA, "Sequoia", 2, d -> {
            d.addLayer(new LayerTree(TreeSequoia.INSTANCE, 256));
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 128));
        });

        // Wasteland
        plugin.c.decorator(BiomeGenerator.Biome.WASTELAND, "Waste", 10, d -> {
        });

        // Steppe
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "Waste", 10, d -> {
        });
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "Birch", 5, d -> {
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 1024));
            d.addLayer(new LayerPatch(m.bush, 0, 16, 64, 1 << 16,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "Spruce", 10, d -> {
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 512));
            d.addLayer(new LayerPatch(m.bush, 0, 16, 64, 1 << 17,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });

        // Forest
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Deciduous", 10, d -> {
            d.addLayer(new LayerTree(TreeOak.INSTANCE, 64));
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 128));
            d.addLayer(new LayerTree(TreeMaple.INSTANCE, 32));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 14,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
            d.addLayer(new LayerPatch(m.bush, 0, 6, 8, 2048,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Birch", 10, d -> {
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 64));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 15,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
            d.addLayer(new LayerPatch(m.bush, 0, 16, 64, 1 << 13,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Spruce", 10, d -> {
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 32));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 14,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
            d.addLayer(new LayerPatch(m.bush, 0, 4, 8, 4096,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Willow", 10, d -> {
            d.addLayer(new LayerTree(TreeWillow.INSTANCE, 64));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 14,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
        });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Sequoia", 2, d -> {
            d.addLayer(new LayerTree(TreeSequoia.INSTANCE, 256));
        });

        // Desert
        plugin.c.decorator(BiomeGenerator.Biome.DESERT, "Waste", 10, d -> {
        });

        // Waste
        plugin.c.decorator(BiomeGenerator.Biome.SAVANNA, "Waste", 10, d -> {
        });

        // Oasis
        plugin.c.decorator(BiomeGenerator.Biome.OASIS, "Palm", 10, d -> {
            d.addLayer(new LayerTree(TreePalm.INSTANCE, 128));
        });

        // Rainforest
        plugin.c.decorator(BiomeGenerator.Biome.RAINFOREST, "Deciduous", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeOak.INSTANCE, 64));
                    d.addLayer(new LayerTree(TreeBirch.INSTANCE, 96));
                    d.addLayer(new LayerTree(TreePalm.INSTANCE, 256));
                    d.addLayer(new LayerTree(TreeMaple.INSTANCE, 32));
                    for (int i = 0; i < 20; i++) {
                        d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 13,
                                (terrain, x, y, z) ->
                                        terrain.type(x, y, z - 1) == m.grass));
                    }
                    d.addLayer(new LayerPatch(m.bush, 0, 4, 8, 1024,
                            (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                    m.grass));
                });
        plugin.c.decorator(BiomeGenerator.Biome.RAINFOREST, "Willow", 4, d -> {
            d.addLayer(new LayerTree(TreeWillow.INSTANCE, 48));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 13,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
            d.addLayer(new LayerPatch(m.bush, 0, 4, 8, 1024,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
    }
}
