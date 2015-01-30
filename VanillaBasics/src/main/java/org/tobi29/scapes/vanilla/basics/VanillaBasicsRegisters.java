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
import org.tobi29.scapes.chunk.WorldEnvironment;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.entity.client.*;
import org.tobi29.scapes.vanilla.basics.entity.server.*;
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator;
import org.tobi29.scapes.vanilla.basics.generator.WorldEnvironmentOverworld;
import org.tobi29.scapes.vanilla.basics.generator.decorator.BiomeDecorator;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerPatch;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerTree;
import org.tobi29.scapes.vanilla.basics.generator.tree.*;
import org.tobi29.scapes.vanilla.basics.material.*;
import org.tobi29.scapes.vanilla.basics.material.update.*;
import org.tobi29.scapes.vanilla.basics.packet.*;
import org.tobi29.scapes.vanilla.basics.util.IngotUtil;
import org.tobi29.scapes.vanilla.basics.util.ToolUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator.Biome.*;

class VanillaBasicsRegisters {
    static void registerCommands(ScapesServer server, VanillaBasics plugin) {
        VanillaMaterial materials = plugin.getMaterials();
        CommandRegistry registry = server.getCommandRegistry();
        registry.register("time", 8, options -> {
            options.add("w", "world", true, "World that is targeted");
            options.add("d", "day", true, "Day that time will be set to");
            options.add("t", "time", true,
                    "Time of day that time will be set to");
            options.add("r", "relative", false,
                    "Add time instead of setting it");
        }, (args, executor, commands) -> {
            String worldName = args.getOption('w');
            Command.require(worldName, 'w');
            boolean relative = args.hasOption('r');
            if (args.hasOption('d')) {
                long day = Command.getLong(args.getOption('d'));
                commands.add(() -> {
                    WorldServer world =
                            server.getWorldFormat().getWorld(worldName);
                    WorldEnvironment environment = world.getEnvironment();
                    if (environment instanceof WorldEnvironmentOverworld) {
                        WorldEnvironmentOverworld environmentOverworld =
                                (WorldEnvironmentOverworld) environment;
                        ClimateGenerator climateGenerator =
                                environmentOverworld.getClimateGenerator();
                        climateGenerator.setDay(day +
                                (relative ? climateGenerator.getDay() : 0));
                    } else {
                        throw new Command.CommandException(20,
                                "Unsupported environment");
                    }
                });
            }
            if (args.hasOption('t')) {
                float dayTime = Command.getFloat(args.getOption('t'));
                commands.add(() -> {
                    WorldServer world =
                            server.getWorldFormat().getWorld(worldName);
                    WorldEnvironment environment = world.getEnvironment();
                    if (environment instanceof WorldEnvironmentOverworld) {
                        WorldEnvironmentOverworld environmentOverworld =
                                (WorldEnvironmentOverworld) environment;
                        ClimateGenerator climateGenerator =
                                environmentOverworld.getClimateGenerator();
                        climateGenerator.setDayTime(dayTime +
                                (relative ? climateGenerator.getDayTime() :
                                        0.0f));
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
            String playerName = args.getOption('p', executor.getPlayerName());
            Command.require(playerName, 'p');
            if (args.hasOption('s')) {
                double saturation = Command.getDouble(args.getOption('s'));
                commands.add(() -> {
                    PlayerConnection player =
                            server.getConnection().getPlayerByName(playerName);
                    Command.require(player, playerName);
                    TagStructure conditionTag =
                            player.getMob().getMetaData("Vanilla")
                                    .getStructure("Condition");
                    synchronized (conditionTag) {
                        conditionTag.setDouble("Hunger", saturation);
                    }
                });
            }
            if (args.hasOption('t')) {
                double thirst = Command.getDouble(args.getOption('t'));
                commands.add(() -> {
                    PlayerConnection player =
                            server.getConnection().getPlayerByName(playerName);
                    Command.require(player, playerName);
                    TagStructure conditionTag =
                            player.getMob().getMetaData("Vanilla")
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
            String playerName = args.getOption('p', executor.getPlayerName());
            Command.require(playerName, 'p');
            String metal = args.getOption('m');
            Command.require(metal, 'm');
            int data = Command.getInt(args.getOption('d', "0"));
            int amount = Command.getInt(args.getOption('a', "1"));
            float temperature = Command.getFloat(args.getOption('t', "0.0"));
            commands.add(() -> {
                MetalType metalType = plugin.getMetalType(metal);
                Command.require(metalType, metal);
                PlayerConnection player =
                        server.getConnection().getPlayerByName(playerName);
                ItemStack item = new ItemStack(materials.ingot, data, amount);
                IngotUtil.createIngot(item, metalType, temperature);
                player.getMob().getInventory().add(item);
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
            String playerName = args.getOption('p', executor.getPlayerName());
            Command.require(playerName, 'p');
            String metal = args.getOption('m');
            Command.require(metal, 'm');
            String kind = args.getOption('k');
            Command.require(kind, 'k');
            int data = Command.getInt(args.getOption('d', "0"));
            int amount = Command.getInt(args.getOption('a', "1"));
            float temperature = Command.getFloat(args.getOption('t', "0.0"));
            commands.add(() -> {
                MetalType metalType = plugin.getMetalType(metal);
                Command.require(metalType, metal);
                PlayerConnection player =
                        server.getConnection().getPlayerByName(playerName);
                ItemStack item = new ItemStack(materials.ingot, data, amount);
                IngotUtil.createIngot(item, metalType, temperature);
                ToolUtil.createTool(plugin, item, kind);
                player.getMob().getInventory().add(item);
            });
        });
    }

    static void registerEntities(GameRegistry registry) {
        registry.registerEntity(MobPigServer::new, MobPigClient::new,
                MobPigServer.class, "vanilla.basics.mob.Pig");
        registry.registerEntity(MobZombieServer::new, MobZombieClient::new,
                MobZombieServer.class, "vanilla.basics.mob.Zombie");
        registry.registerEntity(MobSkeletonServer::new, MobSkeletonClient::new,
                MobSkeletonServer.class, "vanilla.basics.mob.Skeleton");
        registry.registerEntity(EntityTornadoServer::new,
                EntityTornadoClient::new, EntityTornadoServer.class,
                "vanilla.basics.entity.Tornado");
        registry.registerEntity(EntityAlloyServer::new, EntityAlloyClient::new,
                EntityAlloyServer.class, "vanilla.basics.entity.Alloy");
        registry.registerEntity(EntityAnvilServer::new, EntityAnvilClient::new,
                EntityAnvilServer.class, "vanilla.basics.entity.Anvil");
        registry.registerEntity(EntityBellowsServer::new,
                EntityBellowsClient::new, EntityBellowsServer.class,
                "vanilla.basics.entity.Bellows");
        registry.registerEntity(EntityBloomeryServer::new,
                EntityBloomeryClient::new, EntityBloomeryServer.class,
                "vanilla.basics.entity.Bloomery");
        registry.registerEntity(EntityChestServer::new, EntityChestClient::new,
                EntityChestServer.class, "vanilla.basics.entity.Chest");
        registry.registerEntity(EntityForgeServer::new, EntityForgeClient::new,
                EntityForgeServer.class, "vanilla.basics.entity.Forge");
        registry.registerEntity(EntityFurnaceServer::new,
                EntityFurnaceClient::new, EntityFurnaceServer.class,
                "vanilla.basics.entity.Furnace");
        registry.registerEntity(EntityQuernServer::new, EntityQuernClient::new,
                EntityQuernServer.class, "vanilla.basics.entity.Quern");
        registry.registerEntity(EntityResearchTableServer::new,
                EntityResearchTableClient::new, EntityResearchTableServer.class,
                "vanilla.basics.entity.ResearchTable");
    }

    static void registerUpdates(GameRegistry registry) {
        GameRegistry.SupplierRegistry<Update> updateRegistry =
                registry.getSupplier("Core", "Update");
        updateRegistry.register(UpdateWaterFlow::new,
                "vanilla.basics.update.WaterFlow");
        updateRegistry.register(UpdateLavaFlow::new,
                "vanilla.basics.update.LavaFlow");
        updateRegistry.register(UpdateGrassGrowth::new,
                "vanilla.basics.update.GrassGrowth");
        updateRegistry.register(UpdateFlowerGrowth::new,
                "vanilla.basics.update.FlowerGrowth");
        updateRegistry.register(UpdateSaplingGrowth::new,
                "vanilla.basics.update.SaplingGrowth");
        updateRegistry.register(UpdateStrawDry::new,
                "vanilla.basics.update.StrawDry");
    }

    static void registerPackets(GameRegistry registry) {
        GameRegistry.SupplierRegistry<Packet> packetRegistry =
                registry.getSupplier("Core", "Packet");
        packetRegistry.register(PacketDayTimeSync::new,
                "vanilla.basics.packet.DayTimeSync");
        packetRegistry
                .register(PacketAnvil::new, "vanilla.basics.packet.Anvil");
        packetRegistry.register(PacketLightning::new,
                "vanilla.basics.packet.Lightning");
        packetRegistry.register(PacketNotification::new,
                "vanilla.basics.packet.Notification");
        packetRegistry.register(PacketResearch::new,
                "vanilla.basics.packet.Research");
        packetRegistry
                .register(PacketQuern::new, "vanilla.basics.packet.Quern");
    }

    static void registerRecipes(GameRegistry registry,
            VanillaMaterial materials) {
        GameRegistry.Registry<CropType> cropRegistry =
                registry.<CropType>get("VanillaBasics", "CropType");
        GameRegistry.Registry<StoneType> stoneRegistry =
                registry.<StoneType>get("VanillaBasics", "StoneType");
        registerRecipesBasics(registry, materials, stoneRegistry);
        registerRecipesStone(registry, materials, stoneRegistry);
        registerRecipesFood(registry, materials, cropRegistry, stoneRegistry);
        registerRecipesMetal(registry, materials, stoneRegistry);
        registerRecipesIron(registry, materials);
    }

    static void registerRecipesBasics(GameRegistry registry,
            VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String getName() {
                return "Basics";
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
        ItemStack hammerItem = new ItemStack(materials.hammer, (short) 1);
        hammerItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        hammerItem.getMetaData("Vanilla").setDouble("ToolEfficiency", 1.0d);
        hammerItem.getMetaData("Vanilla").setDouble("ToolStrength", 1.0d);
        hammerItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);
        CraftingRecipe.Ingredient hammer =
                new CraftingRecipe.IngredientList(hammerItem);
        ItemStack sawItem = new ItemStack(materials.saw, (short) 1);
        sawItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        sawItem.getMetaData("Vanilla").setDouble("ToolEfficiency", 1.0d);
        sawItem.getMetaData("Vanilla").setDouble("ToolStrength", 1.0d);
        sawItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);
        List<StoneType> stoneTypes = stoneRegistry.values();
        CraftingRecipe.Ingredient saw =
                new CraftingRecipe.IngredientList(sawItem);
        CraftingRecipe.Ingredient plank = new CraftingRecipe.IngredientList(
                new ItemStack(materials.wood, (short) 0, 1),
                new ItemStack(materials.wood, (short) 1, 1),
                new ItemStack(materials.wood, (short) 2, 1));

        recipeType.getRecipes()
                .add(new CraftingRecipe(Collections.singletonList(plank),
                        Arrays.asList(saw, hammer),
                        new ItemStack(materials.craftingTable, (short) 0)));
        recipeType.getRecipes()
                .add(new CraftingRecipe(Collections.singletonList(plank),
                        Collections.singletonList(saw),
                        new ItemStack(materials.chest, (short) 0)));
        recipeType.getRecipes().add(new CraftingRecipe(Arrays.asList(plank,
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.string, (short) 1))),
                Collections.singletonList(saw),
                new ItemStack(materials.researchTable, (short) 0)));
        for (int i = 0; i < stoneTypes.size(); i++) {
            recipeType.getRecipes().add(new CraftingRecipe(
                    new ItemStack(materials.cobblestone, i),
                    new CraftingRecipe.IngredientList(
                            new ItemStack(materials.stoneRock, i, 9))));
        }
        recipeType.getRecipes().add(new CraftingRecipe(
                new ItemStack(materials.torch, (short) 0),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.stick, (short) 0)),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.oreChunk, (short) 0))));

        registry.registerCraftingRecipe(recipeType, false);
    }

    static void registerRecipesStone(GameRegistry registry,
            VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String getName() {
                return "Stone";
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
        ingotItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        CraftingRecipe.Ingredient ingot =
                new CraftingRecipe.IngredientList(ingotItem);
        List<StoneType> stoneTypes = stoneRegistry.values();
        List<ItemStack> rockItems = new ArrayList<>();
        for (int i = 0; i < stoneTypes.size(); i++) {
            if (stoneTypes.get(i).getResistance() > 0.1) {
                rockItems.add(new ItemStack(materials.stoneRock, i, 2));
            }
        }
        CraftingRecipe.Ingredient rocks =
                new CraftingRecipe.IngredientList(rockItems);
        recipeType.getRecipes().add(new CraftingRecipe(ingotItem, rocks));
        ItemStack hoeItem = new ItemStack(materials.hoe, (short) 0);
        hoeItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        hoeItem.getMetaData("Vanilla").setDouble("ToolEfficiency", 1.0d);
        hoeItem.getMetaData("Vanilla").setDouble("ToolStrength", 1.0d);
        hoeItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);
        ItemStack hammerItem = new ItemStack(materials.hammer, (short) 0);
        hammerItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        hammerItem.getMetaData("Vanilla").setDouble("ToolEfficiency", 1.0d);
        hammerItem.getMetaData("Vanilla").setDouble("ToolStrength", 1.0d);
        hammerItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);
        ItemStack sawItem = new ItemStack(materials.saw, (short) 0);
        sawItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        sawItem.getMetaData("Vanilla").setDouble("ToolEfficiency", 1.0d);
        sawItem.getMetaData("Vanilla").setDouble("ToolStrength", 1.0d);
        sawItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);
        ItemStack axeItem = new ItemStack(materials.axe, (short) 0);
        axeItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        axeItem.getMetaData("Vanilla").setDouble("ToolEfficiency", 1.0d);
        axeItem.getMetaData("Vanilla").setDouble("ToolStrength", 3.0d);
        axeItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);
        ItemStack shovelItem = new ItemStack(materials.shovel, (short) 0);
        shovelItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        shovelItem.getMetaData("Vanilla").setDouble("ToolEfficiency", 1.0d);
        shovelItem.getMetaData("Vanilla").setDouble("ToolStrength", 1.0d);
        shovelItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);
        ItemStack pickaxeItem = new ItemStack(materials.pickaxe, (short) 0);
        pickaxeItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        pickaxeItem.getMetaData("Vanilla").setDouble("ToolEfficiency", 1.0d);
        pickaxeItem.getMetaData("Vanilla").setDouble("ToolStrength", 1.0d);
        pickaxeItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);
        ItemStack swordItem = new ItemStack(materials.sword, (short) 0);
        swordItem.getMetaData("Vanilla").setString("MetalType", "Stone");
        swordItem.getMetaData("Vanilla").setDouble("ToolStrength", 2.0d);
        swordItem.getMetaData("Vanilla").setDouble("ToolDamageAdd", 0.01);

        recipeType.getRecipes().add(new CraftingRecipe(hoeItem, ingot));
        recipeType.getRecipes().add(new CraftingRecipe(hammerItem, ingot));
        recipeType.getRecipes().add(new CraftingRecipe(sawItem, ingot));
        recipeType.getRecipes().add(new CraftingRecipe(axeItem, ingot));
        recipeType.getRecipes().add(new CraftingRecipe(shovelItem, ingot));
        recipeType.getRecipes().add(new CraftingRecipe(pickaxeItem, ingot));
        recipeType.getRecipes().add(new CraftingRecipe(swordItem, ingot));
        recipeType.getRecipes().add(new CraftingRecipe(Collections
                .singletonList(new CraftingRecipe.IngredientList(
                        new ItemStack(materials.grassBundle, (short) 0, 2))),
                new ItemStack(materials.string, (short) 0)));
        recipeType.getRecipes().add(new CraftingRecipe(Collections
                .singletonList(new CraftingRecipe.IngredientList(
                        new ItemStack(materials.string, (short) 0, 8))),
                new ItemStack(materials.string, (short) 1)));
        recipeType.getRecipes().add(new CraftingRecipe(Collections
                .singletonList(new CraftingRecipe.IngredientList(
                        new ItemStack(materials.grassBundle, (short) 0, 2))),
                new ItemStack(materials.straw, (short) 0)));
        recipeType.getRecipes().add(new CraftingRecipe(Collections
                .singletonList(new CraftingRecipe.IngredientList(
                        new ItemStack(materials.grassBundle, (short) 1, 2))),
                new ItemStack(materials.straw, (short) 1)));

        registry.registerCraftingRecipe(recipeType, false);
    }

    static void registerRecipesFood(GameRegistry registry,
            VanillaMaterial materials,
            GameRegistry.Registry<CropType> cropRegistry,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String getName() {
                return "Food";
            }

            @Override
            public boolean availableFor(MobPlayerServer player) {
                return player.getMetaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Food");
            }

            @Override
            public boolean availableFor(MobPlayerClientMain player) {
                return player.getMetaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Food");
            }
        };
        List<StoneType> stoneTypes = stoneRegistry.values();
        List<ItemStack> cobblestoneItems = new ArrayList<>();
        for (int i = 0; i < stoneTypes.size(); i++) {
            if (stoneTypes.get(i).getResistance() > 0.1) {
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

        recipeType.getRecipes().add(new CraftingRecipe(
                Arrays.asList(cobblestones, new CraftingRecipe.IngredientList(
                        new ItemStack(materials.stick, (short) 0, 4))),
                Collections.singletonList(pickaxe),
                new ItemStack(materials.furnace, (short) 0)));
        recipeType.getRecipes()
                .add(new CraftingRecipe(Collections.singletonList(cobblestones),
                        Collections.singletonList(hammer),
                        new ItemStack(materials.quern, (short) 0)));
        for (int i = 0; i < cropTypes.size(); i++) {
            recipeType.getRecipes().add(new CraftingRecipe(Collections
                    .singletonList(new CraftingRecipe.IngredientList(
                            new ItemStack(materials.grain, i, 8))),
                    new ItemStack(materials.dough, i)));
        }

        registry.registerCraftingRecipe(recipeType, true);
    }

    static void registerRecipesMetal(GameRegistry registry,
            VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String getName() {
                return "Metal";
            }

            @Override
            public boolean availableFor(MobPlayerServer player) {
                return player.getMetaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Metal");
            }

            @Override
            public boolean availableFor(MobPlayerClientMain player) {
                return player.getMetaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Metal");
            }
        };
        List<StoneType> stoneTypes = stoneRegistry.values();
        List<ItemStack> cobblestoneItems = new ArrayList<>();
        for (int i = 0; i < stoneTypes.size(); i++) {
            if (stoneTypes.get(i).getResistance() > 0.1) {
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
        recipeType.getRecipes().add(new CraftingRecipe(
                new ItemStack(materials.mold, (short) 0),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.sand, (short) 2))));
        recipeType.getRecipes().add(new CraftingRecipe(
                new ItemStack(materials.anvil, (short) 0), ingot));
        recipeType.getRecipes().add(new CraftingRecipe(
                new ItemStack(materials.forge, (short) 0),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.oreChunk, (short) 0, 8))));
        recipeType.getRecipes()
                .add(new CraftingRecipe(Collections.singletonList(cobblestones),
                        Collections.singletonList(pickaxe),
                        new ItemStack(materials.alloy, (short) 0)));
        registry.registerCraftingRecipe(recipeType, true);
    }

    static void registerRecipesIron(GameRegistry registry,
            VanillaMaterial materials) {
        CraftingRecipeType recipeType = new CraftingRecipeType() {
            @Override
            public String getName() {
                return "Iron";
            }

            @Override
            public boolean availableFor(MobPlayerServer player) {
                return player.getMetaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Iron");
            }

            @Override
            public boolean availableFor(MobPlayerClientMain player) {
                return player.getMetaData("Vanilla").getStructure("Research")
                        .getStructure("Finished").getBoolean("Iron");
            }
        };
        CraftingRecipe.Ingredient plank = new CraftingRecipe.IngredientList(
                new ItemStack(materials.wood, (short) 0, 1),
                new ItemStack(materials.wood, (short) 1, 1),
                new ItemStack(materials.wood, (short) 2, 1));

        recipeType.getRecipes().add(new CraftingRecipe(
                new ItemStack(materials.bloomery, (short) 0),
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.sand, (short) 2))));
        recipeType.getRecipes().add(new CraftingRecipe(Arrays.asList(plank,
                new CraftingRecipe.IngredientList(
                        new ItemStack(materials.string, (short) 1, 4))),
                Collections.singletonList(new CraftingRecipe.IngredientList(
                        new ItemStack(materials.saw, (short) 1))),
                new ItemStack(materials.bellows, (short) 0)));

        registry.registerCraftingRecipe(recipeType, true);
    }

    static void registerFuels(VanillaBasics plugin) {
        VanillaMaterial materials = plugin.getMaterials();
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.grassBundle, (short) 0),
                        1600, 0.06f, 0));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.log, (short) 0), 100,
                        0.2f, 10));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.log, (short) 1), 100,
                        0.2f, 10));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.log, (short) 2), 100,
                        0.2f, 10));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.log, (short) 3), 100,
                        0.2f, 10));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.log, (short) 4), 100,
                        0.2f, 10));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.log, (short) 5), 100,
                        0.2f, 10));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.wood, (short) 0), 60,
                        0.1f, 5));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.wood, (short) 1), 60,
                        0.1f, 5));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.wood, (short) 2), 60,
                        0.1f, 5));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.wood, (short) 3), 60,
                        0.1f, 5));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.wood, (short) 4), 60,
                        0.1f, 5));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.wood, (short) 5), 60,
                        0.1f, 5));
        plugin.addFuel(
                new FurnaceFuel(new ItemStack(materials.oreChunk, (short) 0),
                        200, 0.8f, 60));
    }

    static void registerOres(VanillaBasics plugin,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        VanillaMaterial materials = plugin.getMaterials();
        plugin.addOreType(new OreType(materials.oreCoal, 8, 32.0d, 3, 20,
                Collections.singletonList(
                        stoneRegistry.get(StoneType.DIRT_STONE))));
        plugin.addOreType(new OreType(materials.oreCoal, 5, 24.0d, 3, 20,
                Arrays.asList(stoneRegistry.get(StoneType.CHALK),
                        stoneRegistry.get(StoneType.CHERT),
                        stoneRegistry.get(StoneType.CLAYSTONE),
                        stoneRegistry.get(StoneType.CONGLOMERATE),
                        stoneRegistry.get(StoneType.MARBLE),
                        stoneRegistry.get(StoneType.ANDESITE),
                        stoneRegistry.get(StoneType.BASALT),
                        stoneRegistry.get(StoneType.DACITE),
                        stoneRegistry.get(StoneType.RHYOLITE),
                        stoneRegistry.get(StoneType.DIORITE),
                        stoneRegistry.get(StoneType.GABBRO),
                        stoneRegistry.get(StoneType.GRANITE))));
        plugin.addOreType(new OreType(materials.oreCassiterite, 3, 6.0d, 4, 6,
                Arrays.asList(stoneRegistry.get(StoneType.ANDESITE),
                        stoneRegistry.get(StoneType.BASALT),
                        stoneRegistry.get(StoneType.DACITE),
                        stoneRegistry.get(StoneType.RHYOLITE),
                        stoneRegistry.get(StoneType.GRANITE))));
        plugin.addOreType(new OreType(materials.oreSphalerite, 3, 6.0d, 3, 4,
                Collections
                        .singletonList(stoneRegistry.get(StoneType.MARBLE))));
        plugin.addOreType(new OreType(materials.oreBismuthinite, 2, 3.0d, 8, 9,
                Arrays.asList(stoneRegistry.get(StoneType.CHALK),
                        stoneRegistry.get(StoneType.CHERT),
                        stoneRegistry.get(StoneType.CLAYSTONE),
                        stoneRegistry.get(StoneType.CONGLOMERATE),
                        stoneRegistry.get(StoneType.MARBLE),
                        stoneRegistry.get(StoneType.DIORITE),
                        stoneRegistry.get(StoneType.GABBRO),
                        stoneRegistry.get(StoneType.GRANITE))));
        plugin.addOreType(new OreType(materials.oreChalcocite, 6, 8.0d, 2, 1,
                Arrays.asList(stoneRegistry.get(StoneType.CHALK),
                        stoneRegistry.get(StoneType.CHERT),
                        stoneRegistry.get(StoneType.CLAYSTONE),
                        stoneRegistry.get(StoneType.CONGLOMERATE))));
        plugin.addOreType(new OreType(materials.oreMagnetite, 4, 5.0d, 9, 10,
                Arrays.asList(stoneRegistry.get(StoneType.CHALK),
                        stoneRegistry.get(StoneType.CHERT),
                        stoneRegistry.get(StoneType.CLAYSTONE),
                        stoneRegistry.get(StoneType.CONGLOMERATE))));
        plugin.addOreType(new OreType(materials.orePyrite, 3, 4.0d, 11, 13,
                Arrays.asList(stoneRegistry.get(StoneType.CHALK),
                        stoneRegistry.get(StoneType.CHERT),
                        stoneRegistry.get(StoneType.CLAYSTONE),
                        stoneRegistry.get(StoneType.CONGLOMERATE),
                        stoneRegistry.get(StoneType.MARBLE))));
        plugin.addOreType(new OreType(materials.oreSilver, 2, 3.0d, 4, 8,
                Collections
                        .singletonList(stoneRegistry.get(StoneType.GRANITE))));
        plugin.addOreType(new OreType(materials.oreGold, 1, 2.0d, 4, 32,
                Arrays.asList(stoneRegistry.get(StoneType.ANDESITE),
                        stoneRegistry.get(StoneType.BASALT),
                        stoneRegistry.get(StoneType.DACITE),
                        stoneRegistry.get(StoneType.RHYOLITE),
                        stoneRegistry.get(StoneType.DIORITE),
                        stoneRegistry.get(StoneType.GABBRO),
                        stoneRegistry.get(StoneType.GRANITE))));
    }

    static void registerResearch(VanillaBasics plugin) {
        plugin.addResearchRecipe(
                new ResearchRecipe(new String[]{"vanilla.basics.item.Crop"},
                        "Food",
                        "Using a Quern, you can\ncreate grain out of this."));
        plugin.addResearchRecipe(
                new ResearchRecipe(new String[]{"vanilla.basics.item.Grain"},
                        "Food", "Try making dough out of this?"));
        plugin.addResearchRecipe(new ResearchRecipe(
                new String[]{"vanilla.basics.item.OreChunk.Chalcocite"},
                "Metal",
                "You could try heating this on\na forge and let it melt\ninto a" +
                        " ceramic mold.\nMaybe you can find a way\nto shape it," +
                        " to create\nhandy tools?"));
        plugin.addResearchRecipe(new ResearchRecipe(
                new String[]{"vanilla.basics.item.OreChunk.Magnetite"}, "Iron",
                "Maybe you can figure out\na way to create a strong\nmetal out" +
                        " of this ore..."));
    }

    static void registerMetals(VanillaBasics plugin) {
        Map<String, Double> ingredients = new ConcurrentHashMap<>();
        ingredients.put("Stone", 1.0);
        plugin.addMetalType(
                new MetalType("Stone", "Stone", "Worked Stone", ingredients,
                        0.5f, 0.5f, 0.5f, 1200.0f, 1.0, 4.0, 0.004, 10));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Tin", 1.0);
        plugin.addMetalType(
                new MetalType("Tin", "Tin", ingredients, 1.0f, 1.0f, 1.0f,
                        231.0f, 0.1d, 2.0, 0.01, 0));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Zinc", 1.0);
        plugin.addMetalType(
                new MetalType("Zinc", "Zinc", ingredients, 1.0f, 0.9f, 0.9f,
                        419.0f, 1.0, 4.0, 0.004, 10));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Bismuth", 1.0);
        plugin.addMetalType(
                new MetalType("Bismuth", "Bismuth", ingredients, 0.8f, 0.9f,
                        0.9f, 271.0f, 1.0, 4.0, 0.004, 10));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Copper", 1.0);
        plugin.addMetalType(
                new MetalType("Copper", "Copper", ingredients, 0.8f, 0.2f, 0.0f,
                        1084.0f, 6.0, 8.0, 0.001, 20));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Iron", 1.0);
        plugin.addMetalType(
                new MetalType("Iron", "Iron", ingredients, 0.7f, 0.7f, 0.7f,
                        1538.0f, 30.0, 12.0, 0.0001, 40));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Silver", 1.0);
        plugin.addMetalType(
                new MetalType("Silver", "Silver", ingredients, 0.9f, 0.9f, 1.0f,
                        961.0f, 1.0, 4.0, 0.004, 10));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Gold", 1.0);
        plugin.addMetalType(
                new MetalType("Gold", "Gold", ingredients, 1.0f, 0.3f, 0.0f,
                        1064.0f, 0.1, 2.0, 0.01, 0));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Tin", 0.25);
        ingredients.put("Copper", 0.75);
        plugin.addMetalType(
                new MetalType("Bronze", "Bronze", ingredients, 0.6f, 0.4f, 0.0f,
                        800.0f, 10.0d, 10.0, 0.0005, 30));
        ingredients = new ConcurrentHashMap<>();
        ingredients.put("Bismuth", 0.2);
        ingredients.put("Zinc", 0.2);
        ingredients.put("Copper", 0.6);
        plugin.addMetalType(
                new MetalType("BismuthBronze", "Bismuth Bronze", ingredients,
                        1.0f, 0.8f, 0.8f, 800.0f, 10.0d, 10.0, 0.0005, 30));
    }

    static GameRegistry.Registry<TreeType> registerTreeTypes(
            GameRegistry registry) {
        GameRegistry.Registry<TreeType> treeRegistry =
                registry.add("VanillaBasics", "TreeType", 0, Short.MAX_VALUE);
        treeRegistry.register(TreeType.OAK, "vanilla.basics.tree.Oak");
        treeRegistry.register(TreeType.BIRCH, "vanilla.basics.tree.Birch");
        treeRegistry.register(TreeType.SPRUCE, "vanilla.basics.tree.Spruce");
        treeRegistry.register(TreeType.PALM, "vanilla.basics.tree.Palm");
        treeRegistry.register(TreeType.MAPLE, "vanilla.basics.tree.Maple");
        treeRegistry.register(TreeType.SEQUOIA, "vanilla.basics.tree.Sequoia");
        treeRegistry.register(TreeType.WILLOW, "vanilla.basics.tree.Willow");
        return treeRegistry;
    }

    static GameRegistry.Registry<CropType> registerCropTypes(
            GameRegistry registry) {
        GameRegistry.Registry<CropType> cropRegistry =
                registry.add("VanillaBasics", "CropType", 0, Short.MAX_VALUE);
        cropRegistry.register(CropType.WHEAT, "vanilla.basics.crop.Wheat");
        return cropRegistry;
    }

    static GameRegistry.Registry<StoneType> registerStoneTypes(
            GameRegistry registry) {
        GameRegistry.Registry<StoneType> stoneRegistry =
                registry.add("VanillaBasics", "StoneType", 0, Short.MAX_VALUE);
        stoneRegistry.register(StoneType.DIRT_STONE,
                "vanilla.basics.stone.DirtStone");
        stoneRegistry.register(StoneType.CHALK, "vanilla.basics.stone.Chalk");
        stoneRegistry.register(StoneType.CHERT, "vanilla.basics.stone.Chert");
        stoneRegistry.register(StoneType.CLAYSTONE,
                "vanilla.basics.stone.Claystone");
        stoneRegistry.register(StoneType.CONGLOMERATE,
                "vanilla.basics.stone.Conglomerate");
        stoneRegistry.register(StoneType.MARBLE, "vanilla.basics.stone.Marble");
        stoneRegistry
                .register(StoneType.ANDESITE, "vanilla.basics.stone.Andesite");
        stoneRegistry.register(StoneType.BASALT, "vanilla.basics.stone.Basalt");
        stoneRegistry.register(StoneType.DACITE, "vanilla.basics.stone.Dacite");
        stoneRegistry
                .register(StoneType.RHYOLITE, "vanilla.basics.stone.Rhyolite");
        stoneRegistry
                .register(StoneType.DIORITE, "vanilla.basics.stone.Diorite");
        stoneRegistry.register(StoneType.GABBRO, "vanilla.basics.stone.Gabbro");
        stoneRegistry
                .register(StoneType.GRANITE, "vanilla.basics.stone.Granite");
        return stoneRegistry;
    }

    static void registerVegetation(VanillaBasics plugin) {
        VanillaMaterial materials = plugin.getMaterials();
        // Polar
        plugin.addBiomeDecorator(POLAR, "Waste", 10);

        // Tundra
        plugin.addBiomeDecorator(TUNDRA, "Waste", 10);
        {
            BiomeDecorator d = plugin.addBiomeDecorator(TUNDRA, "Spruce", 1);
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 256));
        }

        // Taiga
        {
            BiomeDecorator d = plugin.addBiomeDecorator(TAIGA, "Spruce", 10);
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 32));
        }
        {
            BiomeDecorator d = plugin.addBiomeDecorator(TAIGA, "Sequoia", 2);
            d.addLayer(new LayerTree(TreeSequoia.INSTANCE, 256));
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 128));
        }

        // Wasteland
        plugin.addBiomeDecorator(WASTELAND, "Waste", 10);

        // Steppe
        plugin.addBiomeDecorator(STEPPE, "Waste", 10);
        {
            BiomeDecorator d = plugin.addBiomeDecorator(STEPPE, "Spruce", 10);
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 512));
        }

        // Forest
        {
            BiomeDecorator d =
                    plugin.addBiomeDecorator(FOREST, "Deciduous", 10);
            d.addLayer(new LayerTree(TreeOak.INSTANCE, 64));
            d.addLayer(new LayerTree(TreeMaple.INSTANCE, 32));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(materials.flower, i, 16, 24, 16384,
                        (terrain, x, y, z) ->
                                terrain.getBlockType(x, y, z - 1) ==
                                        materials.grass));
            }
        }
        {
            BiomeDecorator d = plugin.addBiomeDecorator(FOREST, "Spruce", 10);
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 32));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(materials.flower, i, 16, 24, 16384,
                        (terrain, x, y, z) ->
                                terrain.getBlockType(x, y, z - 1) ==
                                        materials.grass));
            }
        }
        {
            BiomeDecorator d = plugin.addBiomeDecorator(FOREST, "Willow", 10);
            d.addLayer(new LayerTree(TreeWillow.INSTANCE, 64));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(materials.flower, i, 16, 24, 16384,
                        (terrain, x, y, z) ->
                                terrain.getBlockType(x, y, z - 1) ==
                                        materials.grass));
            }
        }
        {
            BiomeDecorator d = plugin.addBiomeDecorator(FOREST, "Sequoia", 2);
            d.addLayer(new LayerTree(TreeSequoia.INSTANCE, 256));
        }

        // Desert
        plugin.addBiomeDecorator(DESERT, "Waste", 10);

        // Waste
        plugin.addBiomeDecorator(SAVANNA, "Waste", 10);

        // Oasis
        {
            BiomeDecorator d = plugin.addBiomeDecorator(OASIS, "Palm", 10);
            d.addLayer(new LayerTree(TreePalm.INSTANCE, 128));
        }

        // Rainforest
        {
            BiomeDecorator d =
                    plugin.addBiomeDecorator(RAINFOREST, "Deciduous", 10);
            d.addLayer(new LayerTree(TreeOak.INSTANCE, 64));
            d.addLayer(new LayerTree(TreeMaple.INSTANCE, 32));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(materials.flower, i, 16, 24, 8192,
                        (terrain, x, y, z) ->
                                terrain.getBlockType(x, y, z - 1) ==
                                        materials.grass));
            }
        }
        {
            BiomeDecorator d =
                    plugin.addBiomeDecorator(RAINFOREST, "Willow", 4);
            d.addLayer(new LayerTree(TreeWillow.INSTANCE, 48));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(materials.flower, i, 16, 24, 8192,
                        (terrain, x, y, z) ->
                                terrain.getBlockType(x, y, z - 1) ==
                                        materials.grass));
            }
        }
        {
            BiomeDecorator d = plugin.addBiomeDecorator(RAINFOREST, "Palm", 3);
            d.addLayer(new LayerTree(TreePalm.INSTANCE, 96));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(materials.flower, i, 16, 24, 8192,
                        (terrain, x, y, z) ->
                                terrain.getBlockType(x, y, z - 1) ==
                                        materials.grass));
            }
        }
    }
}
