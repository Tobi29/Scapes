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

import java8.util.concurrent.ConcurrentMaps;
import java8.util.function.Consumer;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.EnvironmentClient;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.utils.Checksum;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.plugins.WorldType;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientVB;
import org.tobi29.scapes.vanilla.basics.entity.model.EntityModelBellowsShared;
import org.tobi29.scapes.vanilla.basics.entity.model.MobLivingModelPigShared;
import org.tobi29.scapes.vanilla.basics.entity.particle.*;
import org.tobi29.scapes.vanilla.basics.entity.server.MobPlayerServerVB;
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator;
import org.tobi29.scapes.vanilla.basics.generator.ClimateInfoLayer;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldClient;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldServer;
import org.tobi29.scapes.vanilla.basics.generator.decorator.BiomeDecorator;
import org.tobi29.scapes.vanilla.basics.material.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class VanillaBasics implements WorldType {
    public final Config c = new Config();
    private final List<CraftingRecipeType> craftingRecipes = new ArrayList<>();
    private final List<ResearchRecipe> researchRecipes = new ArrayList<>();
    private final ConcurrentMap<String, MetalType> metalTypes =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AlloyType> alloyTypes =
            new ConcurrentHashMap<>();
    private final MetalType crapMetal =
            new MetalType("CrapMetal", "Crap Metal", 200.0, 0.17f, 0.12f, 0.1f);
    private final AlloyType crapAlloy =
            new AlloyType("CrapMetal", "Crap Metal", "Crap Metal",
                    Collections.singletonMap(crapMetal, 1.0), 0.17f, 0.12f,
                    0.1f, 0.0, 0.0, 0.0, 0);
    private final List<OreType> oreTypes = new ArrayList<>();
    private final Map<BiomeGenerator.Biome, Map<String, BiomeDecorator>>
            biomeDecorators = new EnumMap<>(BiomeGenerator.Biome.class);
    public VanillaMaterial materials;
    public VanillaParticle particles;
    private MobLivingModelPigShared modelPigShared;
    private EntityModelBellowsShared modelBellowsShared;
    private boolean locked;

    public VanillaBasics() {
        Streams.forEach(BiomeGenerator.Biome.values(),
                biome -> biomeDecorators.put(biome, new ConcurrentHashMap<>()));
    }

    public void addCraftingRecipe(CraftingRecipeType recipe) {
        craftingRecipes.add(recipe);
    }

    private void addResearchRecipe(ResearchRecipe recipe) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        researchRecipes.add(recipe);
    }

    private void addMetalType(MetalType metal) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        metalTypes.put(metal.id(), metal);
    }

    private void addAlloyType(AlloyType alloy) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        alloyTypes.put(alloy.id(), alloy);
    }

    private void addOreType(OreType ore) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        oreTypes.add(ore);
    }

    private BiomeDecorator addBiomeDecorator(BiomeGenerator.Biome biome,
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

    public Stream<CraftingRecipeType> craftingRecipes() {
        return Streams.of(craftingRecipes);
    }

    public Stream<ResearchRecipe> researchRecipes() {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return Streams.of(researchRecipes);
    }

    public MetalType metalType(String id) {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return ConcurrentMaps.getOrDefault(metalTypes, id, crapMetal);
    }

    public AlloyType alloyType(String id) {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return ConcurrentMaps.getOrDefault(alloyTypes, id, crapAlloy);
    }

    public Stream<AlloyType> alloyTypes() {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return Streams.of(alloyTypes.values());
    }

    public Stream<OreType> oreTypes() {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return Streams.of(oreTypes);
    }

    public Stream<BiomeDecorator> biomeDecorators(BiomeGenerator.Biome biome) {
        if (!locked) {
            throw new IllegalStateException("Initializing still running");
        }
        return Streams.of(biomeDecorators.get(biome).values());
    }

    public MobLivingModelPigShared modelPigShared() {
        if (modelPigShared == null) {
            throw new IllegalStateException("Models not available");
        }
        return modelPigShared;
    }

    public EntityModelBellowsShared modelBellowsShared() {
        if (modelBellowsShared == null) {
            throw new IllegalStateException("Models not available");
        }
        return modelBellowsShared;
    }

    @Override
    public void registryType(GameRegistry.RegistryAdder registry) {
        registry.add("VanillaBasics", "TreeType", 0, Short.MAX_VALUE);
        registry.add("VanillaBasics", "CropType", 0, Short.MAX_VALUE);
        registry.add("VanillaBasics", "StoneType", 0, Short.MAX_VALUE);
        registry.add("VanillaBasics", "CraftingRecipe", 0, Integer.MAX_VALUE);
    }

    @Override
    public void register(GameRegistry registry) {
        GameRegistry.AsymSupplierRegistry<WorldServer, EnvironmentServer, WorldClient, EnvironmentClient>
                environmentRegistry =
                registry.getAsymSupplier("Core", "Environment");
        environmentRegistry
                .reg(world -> new EnvironmentOverworldServer(world, this),
                        EnvironmentOverworldClient::new,
                        EnvironmentOverworldServer.class,
                        "vanilla.basics.Overworld");
        GameRegistry.Registry<TreeType> treeRegistry =
                VanillaBasicsTrees.registerTreeTypes(registry);
        GameRegistry.Registry<CropType> cropRegistry =
                VanillaBasicsCrops.registerCropTypes(registry);
        GameRegistry.Registry<StoneType> stoneRegistry =
                VanillaBasicsStones.registerStoneTypes(registry);
        materials =
                new VanillaMaterial(this, registry, treeRegistry, cropRegistry,
                        stoneRegistry);
        VanillaBasicsEntities.registerEntities(registry);
        VanillaBasicsUpdates.registerUpdates(registry);
        VanillaBasicsPackets.registerPackets(registry);
        VanillaBasicsOres.registerOres(this);
        VanillaBasicsResearch.registerResearch(this);
        VanillaBasicsMetals.registerMetals(this);
        VanillaBasicsDecorators.registerDecorators(this);
    }

    @Override
    public void init(GameRegistry registry) {
        Streams.forEach(c.decoratorOverlays.values(), config -> Streams
                .forEach(biomeDecorators.values(),
                        biome -> Streams.forEach(biome.values(), config)));
        locked = true;
        VanillaBasicsCrafting.registerRecipes(this, registry);
    }

    @Override
    public void initServer(ScapesServer server) {
        VanillaBasicsRegisters.registerCommands(server, this);
    }

    @Override
    public void initClient(GameStateGameMP game) {
        particles = new VanillaParticle(game.particleTransparentAtlas());
        modelPigShared = new MobLivingModelPigShared(game.engine());
        modelBellowsShared = new EntityModelBellowsShared(game.engine());
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
        world.scene().particles().register(
                new ParticleEmitterExplosion(world.scene().particles()));
        world.scene().particles().register(
                new ParticleEmitterRain(world.scene().particles(),
                        world.game().engine().graphics().textures().empty()));
        world.scene().particles().register(
                new ParticleEmitterSnow(world.scene().particles(),
                        world.game().engine().graphics().textures().empty()));
        world.scene().particles().register(
                new ParticleEmitterLightning(world.scene().particles()));
        world.scene().particles().register(
                new ParticleEmitterTornado(world.scene().particles()));
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
            Checksum skin, PlayerConnection connection) {
        return new MobPlayerServerVB(world, pos, speed, xRot, zRot, nickname,
                skin, connection);
    }

    @Override
    public EnvironmentServer createEnvironment(WorldServer world) {
        return new EnvironmentOverworldServer(world, this);
    }

    public static class RecipeList {
        private final List<CraftingRecipe.Ingredient> list = new ArrayList<>();

        public void add(CraftingRecipe.Ingredient ingredient) {
            list.add(ingredient);
        }

        public void add(ItemStack item) {
            add(new CraftingRecipe.IngredientList(item));
        }

        public void add(List<ItemStack> item) {
            add(new CraftingRecipe.IngredientList(item));
        }
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

        public void metal(Consumer<MetalTypeCreator> metal) {
            MetalTypeCreator creator = new MetalTypeCreator();
            metal.accept(creator);
            MetalType metalType = new MetalType(creator.id, creator.name,
                    creator.meltingPoint, creator.r, creator.g, creator.b);
            addMetalType(metalType);
            addAlloyType(
                    new AlloyType(creator.id, creator.name, creator.ingotName,
                            Collections.singletonMap(metalType, 1.0), creator.r,
                            creator.g, creator.b, creator.toolEfficiency,
                            creator.toolStrength, creator.toolDamage,
                            creator.toolLevel));
        }

        public void alloy(Consumer<AlloyTypeCreator> alloy) {
            AlloyTypeCreator creator = new AlloyTypeCreator();
            alloy.accept(creator);
            Map<MetalType, Double> ingredients = new ConcurrentHashMap<>();
            Streams.forEach(creator.ingredients.entrySet(), entry -> ingredients
                    .put(metalTypes.get(entry.getKey()), entry.getValue()));
            addAlloyType(
                    new AlloyType(creator.id, creator.name, creator.ingotName,
                            ingredients, creator.r, creator.g, creator.b,
                            creator.toolEfficiency, creator.toolStrength,
                            creator.toolDamage, creator.toolLevel));
        }

        public void ore(Consumer<OreTypeCreator> ore) {
            OreTypeCreator creator = new OreTypeCreator();
            ore.accept(creator);
            List<Integer> stoneTypes = Streams.of(creator.stoneTypes)
                    .map(stoneType -> stoneType.data(materials.registry))
                    .collect(Collectors.toList());
            addOreType(new OreType(creator.type, creator.rarity, creator.size,
                    creator.chance, creator.rockChance, creator.rockDistance,
                    stoneTypes));
        }

        public void craftingRecipe(CraftingRecipeType recipeType,
                Consumer<CraftingRecipeCreator> craftingRecipe, String id) {
            GameRegistry.Registry<CraftingRecipe> craftingRegistry =
                    materials.registry.get("VanillaBasics", "CraftingRecipe");
            CraftingRecipeCreator creator = new CraftingRecipeCreator();
            craftingRecipe.accept(creator);
            CraftingRecipe recipe = new CraftingRecipe(creator.ingredients.list,
                    creator.requirements.list, creator.result);
            craftingRegistry.reg(recipe, id);
            recipeType.add(recipe);
        }

        public class MetalTypeCreator {
            public String id = "", name = "", ingotName = "";
            public double meltingPoint;
            public float r, g, b;
            public double toolEfficiency, toolStrength, toolDamage;
            public int toolLevel;
        }

        public class AlloyTypeCreator {
            public final Map<String, Double> ingredients =
                    new ConcurrentHashMap<>();
            public String id = "", name = "", ingotName = "";
            public float r, g, b;
            public double toolEfficiency, toolStrength, toolDamage;
            public int toolLevel;
        }

        public class OreTypeCreator {
            public final List<StoneType> stoneTypes = new ArrayList<>();
            public BlockType type = materials.stoneRaw;
            public int rarity = 4, chance = 4, rockChance = 8, rockDistance =
                    48;
            public double size = 6.0;
        }

        public class CraftingRecipeCreator {
            public final RecipeList ingredients = new RecipeList(),
                    requirements = new RecipeList();
            public ItemStack result;
        }
    }
}
