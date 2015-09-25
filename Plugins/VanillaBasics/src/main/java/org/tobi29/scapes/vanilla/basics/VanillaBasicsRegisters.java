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

import org.tobi29.scapes.block.*;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketUpdateInventory;
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

import java.util.*;
import java.util.stream.Collectors;

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
                            Command.require(server.worldFormat()::world,
                                    worldName);
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
                            Command.require(server.worldFormat()::world,
                                    worldName);
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
            options.add("s", "saturation", true, "Saturation value (0-1)");
            options.add("t", "thirst", true, "Thirst value (0-1)");
        }, (args, executor, commands) -> {
            String playerName = args.requireOption('p', executor.playerName());
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
                MetalType metalType = plugin.getMetalType(metal);
                ItemStack item = new ItemStack(materials.ingot, data, amount);
                IngotUtil.createIngot(item, metalType, temperature);
                player.mob().inventory("Container").add(item);
                player.mob().world()
                        .send(new PacketUpdateInventory(player.mob(),
                                "Container"));
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
                MetalType metalType = plugin.getMetalType(metal);
                ItemStack item = new ItemStack(materials.ingot, data, amount);
                IngotUtil.createIngot(item, metalType, temperature);
                if (!ToolUtil.createTool(plugin, item, kind)) {
                    Command.error("Unknown tool kind: " + kind);
                }
                player.mob().inventory("Container").add(item);
                player.mob().world()
                        .send(new PacketUpdateInventory(player.mob(),
                                "Container"));
            });
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
        pr.regS(PacketOpenCrafting::new, "vanilla.basics.packet.OpenCrafting");
        pr.regS(PacketAnvil::new, "vanilla.basics.packet.Anvil");
        pr.regS(PacketLightning::new, "vanilla.basics.packet.Lightning");
        pr.regS(PacketNotification::new, "vanilla.basics.packet.Notification");
        pr.regS(PacketResearch::new, "vanilla.basics.packet.Research");
        pr.regS(PacketQuern::new, "vanilla.basics.packet.Quern");
    }

    static void registerRecipes(GameRegistry registry,
            VanillaMaterial materials) {
        GameRegistry.Registry<CropType> cropRegistry =
                registry.<CropType>get("VanillaBasics", "CropType");
        GameRegistry.Registry<TreeType> treeRegistry =
                registry.<TreeType>get("VanillaBasics", "TreeType");
        GameRegistry.Registry<StoneType> stoneRegistry =
                registry.<StoneType>get("VanillaBasics", "StoneType");
        registerRecipesBasics(registry, materials, treeRegistry, stoneRegistry);
        registerRecipesStone(registry, materials, stoneRegistry);
        registerRecipesFood(registry, materials, cropRegistry, stoneRegistry);
        registerRecipesMetal(registry, materials, stoneRegistry);
        registerRecipesIron(registry, materials);
    }

    static void registerRecipesBasics(GameRegistry registry,
            VanillaMaterial materials,
            GameRegistry.Registry<TreeType> treeRegistry,
            GameRegistry.Registry<StoneType> stoneRegistry) {
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
        ItemStack ingotItem = new ItemStack(materials.ingot, (short) 1);
        IngotUtil.createIngot(materials.plugin, ingotItem, "Stone", 0.0f);
        ItemStack hammerItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, hammerItem, "Hammer");
        hammerItem.setData(1);
        CraftingRecipe.Ingredient hammer =
                new CraftingRecipe.IngredientList(hammerItem);
        ItemStack sawItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, sawItem, "Saw");
        sawItem.setData(1);
        List<StoneType> stoneTypes = stoneRegistry.values();
        CraftingRecipe.Ingredient saw =
                new CraftingRecipe.IngredientList(sawItem);
        CraftingRecipe.Ingredient plank = new CraftingRecipe.IngredientList(
                treeRegistry.values().stream()
                        .map(tree -> new ItemStack(materials.wood,
                                tree.data(registry)))
                        .collect(Collectors.toList()));

        recipeType.recipes()
                .add(new CraftingRecipe(Collections.singletonList(plank),
                        Arrays.asList(saw, hammer),
                        new ItemStack(materials.craftingTable, (short) 0)));
        recipeType.recipes()
                .add(new CraftingRecipe(Collections.singletonList(plank),
                        Collections.singletonList(saw),
                        new ItemStack(materials.chest, (short) 0)));
        recipeType.recipes().add(new CraftingRecipe(Arrays.asList(plank,
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.string, (short) 1))),
                Collections.singletonList(saw),
                new ItemStack(materials.researchTable, (short) 0)));
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

        registry.registerCraftingRecipe(recipeType);
    }

    static void registerRecipesStone(GameRegistry registry,
            VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
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
        ItemStack ingotItem = new ItemStack(materials.ingot, 1);
        IngotUtil.createIngot(materials.plugin, ingotItem, "Stone", 0.0f);
        CraftingRecipe.Ingredient ingot =
                new CraftingRecipe.IngredientList(ingotItem);
        List<StoneType> stoneTypes = stoneRegistry.values();
        List<ItemStack> rockItems = new ArrayList<>();
        for (int i = 0; i < stoneTypes.size(); i++) {
            if (stoneTypes.get(i).resistance() > 0.1) {
                rockItems.add(new ItemStack(materials.stoneRock, i, 2));
            }
        }
        CraftingRecipe.Ingredient rocks =
                new CraftingRecipe.IngredientList(rockItems);
        recipeType.recipes().add(new CraftingRecipe(ingotItem, rocks));
        ItemStack hoeItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, hoeItem, "Hoe");
        ItemStack hoeHeadItem = new ItemStack(hoeItem).setData(0);
        ItemStack hammerItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, hammerItem, "Hammer");
        ItemStack hammerHeadItem = new ItemStack(hammerItem).setData(0);
        ItemStack sawItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, sawItem, "Saw");
        ItemStack sawHeadItem = new ItemStack(sawItem).setData(0);
        ItemStack axeItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, axeItem, "Axe");
        ItemStack axeHeadItem = new ItemStack(axeItem).setData(0);
        ItemStack shovelItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, shovelItem, "Shovel");
        ItemStack shovelHeadItem = new ItemStack(shovelItem).setData(0);
        ItemStack pickaxeItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, pickaxeItem, "Pickaxe");
        ItemStack pickaxeHeadItem = new ItemStack(pickaxeItem).setData(0);
        ItemStack swordItem = new ItemStack(ingotItem);
        ToolUtil.createTool(materials.plugin, swordItem, "Sword");
        ItemStack swordHeadItem = new ItemStack(swordItem).setData(0);

        recipeType.recipes().add(new CraftingRecipe(hoeHeadItem, ingot));
        recipeType.recipes().add(new CraftingRecipe(hammerHeadItem, ingot));
        recipeType.recipes().add(new CraftingRecipe(sawHeadItem, ingot));
        recipeType.recipes().add(new CraftingRecipe(axeHeadItem, ingot));
        recipeType.recipes().add(new CraftingRecipe(shovelHeadItem, ingot));
        recipeType.recipes().add(new CraftingRecipe(pickaxeHeadItem, ingot));
        recipeType.recipes().add(new CraftingRecipe(swordHeadItem, ingot));
        recipeType.recipes().add(new CraftingRecipe(Collections.singletonList(
                new CraftingRecipe.IngredientList(hoeItem, hammerItem, sawItem,
                        axeItem, shovelItem, pickaxeItem, swordItem,
                        hoeHeadItem, hammerHeadItem, sawHeadItem, axeHeadItem,
                        shovelHeadItem, pickaxeHeadItem, swordHeadItem)),
                ingotItem));
        recipeType.recipes().add(new CraftingRecipe(Collections.singletonList(
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.grassBundle, (short) 0, 2))),
                new ItemStack(materials.string, (short) 0)));
        recipeType.recipes().add(new CraftingRecipe(Collections.singletonList(
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.string, (short) 0, 8))),
                new ItemStack(materials.string, (short) 1)));
        recipeType.recipes().add(new CraftingRecipe(Collections.singletonList(
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.grassBundle, (short) 0, 2))),
                new ItemStack(materials.straw, (short) 0)));
        recipeType.recipes().add(new CraftingRecipe(Collections.singletonList(
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.grassBundle, (short) 1, 2))),
                new ItemStack(materials.straw, (short) 1)));

        registry.registerCraftingRecipe(recipeType);
    }

    static void registerRecipesFood(GameRegistry registry,
            VanillaMaterial materials,
            GameRegistry.Registry<CropType> cropRegistry,
            GameRegistry.Registry<StoneType> stoneRegistry) {
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
                new ItemStack(materials.pickaxe, (short) 1));
        CraftingRecipe.Ingredient hammer = new CraftingRecipe.IngredientList(
                new ItemStack(materials.hammer, (short) 1));

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

        registry.registerCraftingRecipe(recipeType);
    }

    static void registerRecipesMetal(GameRegistry registry,
            VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
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
                new ItemStack(materials.pickaxe, (short) 1));
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

        registry.registerCraftingRecipe(recipeType);
    }

    static void registerRecipesIron(GameRegistry registry,
            VanillaMaterial materials) {
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
                        new ItemStack(materials.saw, (short) 1))),
                new ItemStack(materials.bellows, (short) 0)));

        registry.registerCraftingRecipe(recipeType);
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
        plugin.c.ore(materials.oreCoal, 8, 16.0, 3, 20, StoneType.DIRT_STONE);
        plugin.c.ore(materials.oreCoal, 5, 12.0, 3, 20, StoneType.CHALK,
                StoneType.CHERT, StoneType.CLAYSTONE, StoneType.CONGLOMERATE,
                StoneType.MARBLE, StoneType.ANDESITE, StoneType.BASALT,
                StoneType.DACITE, StoneType.RHYOLITE, StoneType.DIORITE,
                StoneType.GABBRO, StoneType.GRANITE);
        plugin.c.ore(materials.oreCassiterite, 3, 6.0, 4, 6, StoneType.ANDESITE,
                StoneType.BASALT, StoneType.DACITE, StoneType.RHYOLITE,
                StoneType.GRANITE);
        plugin.c.ore(materials.oreSphalerite, 3, 6.0, 3, 4, StoneType.MARBLE);
        plugin.c.ore(materials.oreBismuthinite, 2, 3.0, 8, 9, StoneType.CHALK,
                StoneType.CHERT, StoneType.CLAYSTONE, StoneType.CONGLOMERATE,
                StoneType.MARBLE, StoneType.DIORITE, StoneType.GABBRO,
                StoneType.GRANITE);
        plugin.c.ore(materials.oreChalcocite, 6, 8.0, 2, 1, StoneType.CHALK,
                StoneType.CHERT, StoneType.CLAYSTONE, StoneType.CONGLOMERATE);
        plugin.c.ore(materials.oreMagnetite, 4, 5.0, 9, 10, StoneType.CHALK,
                StoneType.CHERT, StoneType.CLAYSTONE, StoneType.CONGLOMERATE);
        plugin.c.ore(materials.orePyrite, 3, 4.0, 11, 13, StoneType.CHALK,
                StoneType.CHERT, StoneType.CLAYSTONE, StoneType.CONGLOMERATE,
                StoneType.MARBLE);
        plugin.c.ore(materials.oreSilver, 2, 3.0, 4, 8, StoneType.GRANITE);
        plugin.c.ore(materials.oreGold, 1, 2.0, 4, 32, StoneType.ANDESITE,
                StoneType.BASALT, StoneType.DACITE, StoneType.RHYOLITE,
                StoneType.DIORITE, StoneType.GABBRO, StoneType.GRANITE);
    }

    @SuppressWarnings("CodeBlock2Expr")
    static void registerMetals(VanillaBasics plugin) {
        plugin.c.metal("Stone", "Stone", "Worked Stone", 0.5f, 0.5f, 0.5f,
                1200.0f, 1.0, 4.0, 0.004, 10, i -> {
                    i.put("Stone", 1.0);
                });
        plugin.c.metal("Tin", "Tin", 1.0f, 1.0f, 1.0f, 231.0f, 0.1, 2.0, 0.01,
                0, i -> {
                    i.put("Tin", 1.0);
                });
        plugin.c.metal("Zinc", "Zinc", 1.0f, 0.9f, 0.9f, 419.0f, 1.0, 4.0,
                0.004, 10, i -> {
                    i.put("Zinc", 1.0);
                });
        plugin.c.metal("Bismuth", "Bismuth", 0.8f, 0.9f, 0.9f, 271.0f, 1.0, 4.0,
                0.004, 10, i -> {
                    i.put("Bismuth", 1.0);
                });
        plugin.c.metal("Copper", "Copper", 0.8f, 0.2f, 0.0f, 1084.0f, 6.0, 8.0,
                0.001, 20, i -> {
                    i.put("Copper", 1.0);
                });
        plugin.c.metal("Iron", "Iron", 0.7f, 0.7f, 0.7f, 1538.0f, 30.0, 12.0,
                0.0001, 40, i -> {
                    i.put("Iron", 1.0);
                });
        plugin.c.metal("Silver", "Silver", 0.9f, 0.9f, 1.0f, 961.0f, 1.0, 4.0,
                0.004, 10, i -> {
                    i.put("Silver", 1.0);
                });
        plugin.c.metal("Gold", "Gold", 1.0f, 0.3f, 0.0f, 1064.0f, 0.1, 2.0,
                0.01, 0, i -> {
                    i.put("Gold", 1.0);
                });
        plugin.c.metal("Bronze", "Bronze", 0.6f, 0.4f, 0.0f, 800.0f, 10.0, 10.0,
                0.0005, 30, i -> {
                    i.put("Tin", 0.25);
                    i.put("Copper", 0.75);
                });
        plugin.c.metal("BismuthBronze", "Bismuth Bronze", 1.0f, 0.8f, 0.8f,
                800.0f, 10.0, 10.0, 0.0005, 30, i -> {
                    i.put("Bismuth", 0.2);
                    i.put("Zinc", 0.2);
                    i.put("Copper", 0.6);
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
