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

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.EnvironmentClient;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.plugins.WorldType;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientVB;
import org.tobi29.scapes.vanilla.basics.entity.server.MobPlayerServerVB;
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator;
import org.tobi29.scapes.vanilla.basics.generator.ClimateInfoLayer;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldClient;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldServer;
import org.tobi29.scapes.vanilla.basics.generator.decorator.BiomeDecorator;
import org.tobi29.scapes.vanilla.basics.material.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VanillaBasics implements WorldType {
    public final Config c = new Config();
    private final List<ResearchRecipe> researchRecipes = new ArrayList<>();
    private final Map<String, MetalType> metalTypes = new ConcurrentHashMap<>();
    private final MetalType crapMetal =
            new MetalType("Crap Metal", "Crap Metal", new ConcurrentHashMap<>(),
                    0.17f, 0.12f, 0.1f, 200.0f, 0.0, 0.0, 0.0, 0);
    private final List<OreType> oreTypes = new ArrayList<>();
    private final Map<BiomeGenerator.Biome, Map<String, BiomeDecorator>>
            biomeDecorators = new EnumMap<>(BiomeGenerator.Biome.class);
    public VanillaMaterial materials;
    private boolean locked;

    public VanillaBasics() {
        Arrays.stream(BiomeGenerator.Biome.values()).forEach(
                biome -> biomeDecorators.put(biome, new ConcurrentHashMap<>()));
    }

    public void addResearchRecipe(ResearchRecipe recipe) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        researchRecipes.add(recipe);
    }

    public void addMetalType(MetalType recipe) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        metalTypes.put(recipe.id(), recipe);
    }

    public void addOreType(OreType oreType) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        oreTypes.add(oreType);
    }

    public BiomeDecorator addBiomeDecorator(BiomeGenerator.Biome biome,
            String name, int weight) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        Map<String, BiomeDecorator> biomeMap = biomeDecorators.get(biome);
        BiomeDecorator biomeDecorator = biomeMap.get(name);
        if (biomeDecorator == null) {
            biomeDecorator = new BiomeDecorator();
            biomeMap.put(name, biomeDecorator);
        }
        biomeDecorator.addWeight(weight);
        return biomeDecorator;
    }

    public Stream<ResearchRecipe> getResearchRecipes() {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return researchRecipes.stream();
    }

    public MetalType getMetalType(String id) {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return metalTypes.getOrDefault(id, crapMetal);
    }

    public Stream<MetalType> getMetalTypes() {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return metalTypes.values().stream();
    }

    public Stream<OreType> getOreTypes() {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return oreTypes.stream();
    }

    public Stream<BiomeDecorator> getBiomeDecorators(
            BiomeGenerator.Biome biome) {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return biomeDecorators.get(biome).values().stream();
    }

    @Override
    public void init(GameRegistry registry) {
        GameRegistry.AsymSupplierRegistry<WorldServer, EnvironmentServer, WorldClient, EnvironmentClient>
                environmentRegistry =
                registry.getAsymSupplier("Core", "Environment");
        environmentRegistry
                .reg(world -> new EnvironmentOverworldServer(world, this),
                        EnvironmentOverworldClient::new,
                        EnvironmentOverworldServer.class,
                        "vanilla.basics.Overworld");
        GameRegistry.Registry<TreeType> treeRegistry =
                VanillaBasicsRegisters.registerTreeTypes(registry);
        GameRegistry.Registry<CropType> cropRegistry =
                VanillaBasicsRegisters.registerCropTypes(registry);
        GameRegistry.Registry<StoneType> stoneRegistry =
                VanillaBasicsRegisters.registerStoneTypes(registry);
        materials =
                new VanillaMaterial(this, registry, treeRegistry, cropRegistry,
                        stoneRegistry);
        VanillaBasicsRegisters.registerEntities(registry);
        VanillaBasicsRegisters.registerUpdates(registry);
        VanillaBasicsRegisters.registerPackets(registry);
        VanillaBasicsRegisters.registerOres(this);
        VanillaBasicsRegisters.registerResearch(this);
        VanillaBasicsRegisters.registerMetals(this);
        VanillaBasicsRegisters.registerVegetation(this);
    }

    @Override
    public void initEnd(GameRegistry registry) {
        c.decoratorOverlays.values().forEach(config -> biomeDecorators.values()
                .forEach(biome -> biome.values().forEach(config::accept)));
        locked = true;
        VanillaBasicsRegisters.registerRecipes(registry, materials);
    }

    @Override
    public void initServer(ScapesServer server) {
        VanillaBasicsRegisters.registerCommands(server, this);
    }

    @Override
    public void initServerEnd(ScapesServer server) {
    }

    @Override
    public void initClient(GameStateGameMP game) {
    }

    @Override
    public void initClientEnd(GameStateGameMP game) {
    }

    @Override
    public void worldInit(WorldServer world) {
    }

    @Override
    public void worldInit(WorldClient world) {
        EnvironmentOverworldClient environment =
                (EnvironmentOverworldClient) world.environment();
        world.infoLayer("VanillaBasics:Climate",
                () -> new ClimateInfoLayer(environment.climate()));
    }

    @Override
    public String id() {
        return "VanillaBasics";
    }

    @Override
    public String assetRoot() {
        return "assets/scapes/tobi29/vanilla/basics/";
    }

    public VanillaMaterial getMaterials() {
        return materials;
    }

    @Override
    public EntityClient.Supplier playerSupplier() {
        return MobPlayerClientVB::new;
    }

    @Override
    public Class<? extends MobPlayerServer> playerClass() {
        return MobPlayerServerVB.class;
    }

    @Override
    public MobPlayerClientMain newPlayer(WorldClient world, Vector3 pos,
            Vector3 speed, double xRot, double zRot, String nickname) {
        return new MobPlayerClientMainVB(world, pos, speed, xRot, zRot,
                nickname);
    }

    @Override
    public MobPlayerServer newPlayer(WorldServer world, Vector3 pos,
            Vector3 speed, double xRot, double zRot, String nickname,
            byte[] skin, PlayerConnection connection) {
        return new MobPlayerServerVB(world, pos, speed, xRot, zRot, nickname,
                skin, connection);
    }

    @Override
    public EnvironmentServer createEnvironment(WorldServer world) {
        return new EnvironmentOverworldServer(world, this);
    }

    public class Config {
        private final Map<String, Consumer<BiomeDecorator>> decoratorOverlays =
                new ConcurrentHashMap<>();

        public void research(String name, String text, String... items) {
            addResearchRecipe(new ResearchRecipe(name, text, items));
        }

        public void decorator(String name, Consumer<BiomeDecorator> config) {
            decoratorOverlays.put(name, config);
        }

        public void decorator(BiomeGenerator.Biome biome, String name,
                int weight, Consumer<BiomeDecorator> config) {
            config.accept(addBiomeDecorator(biome, name, weight));
        }

        public void metal(String id, String name, float r, float g, float b,
                float meltingPoint, double toolEfficiency, double toolStrength,
                double toolDamage, int toolLevel,
                Consumer<Map<String, Double>> config) {
            metal(id, name, name, r, g, b, meltingPoint, toolEfficiency,
                    toolStrength, toolDamage, toolLevel, config);
        }

        public void metal(String id, String name, String ingotName, float r,
                float g, float b, float meltingPoint, double toolEfficiency,
                double toolStrength, double toolDamage, int toolLevel,
                Consumer<Map<String, Double>> config) {
            Map<String, Double> ingredients = new ConcurrentHashMap<>();
            config.accept(ingredients);
            addMetalType(
                    new MetalType(id, name, ingotName, ingredients, r, g, b,
                            meltingPoint, toolEfficiency, toolStrength,
                            toolDamage, toolLevel));
        }

        public void ore(BlockType type, int rarity, double size, int chance,
                int rockChance, StoneType... stoneTypes) {
            GameRegistry.Registry<StoneType> stoneRegistry = materials.registry
                    .<StoneType>get("VanillaBasics", "StoneType");
            List<Integer> stoneTypeList =
                    Arrays.stream(stoneTypes).map(stoneRegistry::get)
                            .collect(Collectors.toList());
            addOreType(new OreType(type, rarity, size, chance, rockChance,
                    stoneTypeList));
        }
    }
}
