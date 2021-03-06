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

package org.tobi29.scapes.vanilla.basics

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.tobi29.checksums.Checksum
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.EnvironmentType
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.plugins.WorldType
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import org.tobi29.scapes.vanilla.basics.entity.model.EntityModelBellowsShared
import org.tobi29.scapes.vanilla.basics.entity.model.MobLivingModelPigShared
import org.tobi29.scapes.vanilla.basics.entity.particle.*
import org.tobi29.scapes.vanilla.basics.entity.server.MobPlayerServerVB
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.generator.StoneType
import org.tobi29.scapes.vanilla.basics.material.*
import org.tobi29.scapes.vanilla.basics.world.ClimateInfoLayer
import org.tobi29.scapes.vanilla.basics.world.EnvironmentOverworldClient
import org.tobi29.scapes.vanilla.basics.world.EnvironmentOverworldServer
import org.tobi29.scapes.vanilla.basics.world.decorator.BiomeDecorator
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.EnumMap
import org.tobi29.stdex.readOnly

class VanillaBasics : WorldType {
    private val craftingRecipesMut = ArrayList<CraftingRecipeType>()
    val craftingRecipes = craftingRecipesMut.readOnly()
    private val researchRecipesMut = ArrayList<ResearchRecipe>()
    val researchRecipes = researchRecipesMut.readOnly()
    private val metalTypesMut = ConcurrentHashMap<String, MetalType>()
    private val alloyTypesMut = ConcurrentHashMap<String, AlloyType>()
    val metalTypes = metalTypesMut.readOnly()
    val alloyTypes = alloyTypesMut.readOnly()
    val crapMetal = MetalType("CrapMetal", "Crap Metal", 200.0, 0.17, 0.12, 0.1)
    val crapAlloy = AlloyType("CrapMetal", "Crap Metal", "Crap Metal",
            mapOf(Pair(crapMetal, 1.0)), 0.17, 0.12, 0.1, 0.0, 0.0, 0.0, 0)
    private val oreTypesMut = ArrayList<OreType>()
    val oreTypes = oreTypesMut.readOnly()
    private val biomeDecorators = EnumMap<BiomeGenerator.Biome, MutableMap<String, BiomeDecorator>>()
    private val biomeDecoratorOverlays = ConcurrentHashMap<String, BiomeDecorator.() -> Unit>()
    lateinit var materials: VanillaMaterial
    lateinit var particles: VanillaParticle
    lateinit var cropTypes: VanillaBasicsCrops
    lateinit var treeTypes: VanillaBasicsTrees
    lateinit var stoneTypes: VanillaBasicsStones
    lateinit var entityTypes: VanillaBasicsEntities
    private lateinit var environmentOverworld: EnvironmentType
    private var modelPigShared: Deferred<MobLivingModelPigShared>? = null
    private var modelBellowsShared: Deferred<EntityModelBellowsShared>? = null
    private var locked = false
    override val id = "VanillaBasics"

    init {
        BiomeGenerator.Biome.values().forEach {
            biomeDecorators.put(it, ConcurrentHashMap())
        }
    }

    fun addCraftingRecipe(recipe: CraftingRecipeType) {
        craftingRecipesMut.add(recipe)
    }

    fun addResearchRecipe(researchRecipe: ResearchRecipe) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        researchRecipesMut.add(researchRecipe)
    }

    fun addMetalType(metalType: MetalType) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        metalTypesMut.put(metalType.id, metalType)
    }

    fun metalType(id: String): MetalType? {
        return metalTypes[id]
    }

    fun addAlloyType(alloyType: AlloyType) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        alloyTypesMut.put(alloyType.id, alloyType)
    }

    fun alloyType(id: String): AlloyType? {
        return alloyTypes[id]
    }

    fun addOreType(oreType: OreType) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        oreTypesMut.add(oreType)
    }

    fun addBiomeDecoratorOverlay(name: String,
                                 overlay: BiomeDecorator.() -> Unit) {
        biomeDecoratorOverlays.put(name, overlay)
    }

    fun addBiomeDecorator(biome: BiomeGenerator.Biome,
                          name: String,
                          weight: Int,
                          decorator: BiomeDecorator.() -> Unit) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        biomeDecorators[biome]?.let {
            val biomeDecorator: BiomeDecorator
            val existingBiomeDecorator = it[name]
            if (existingBiomeDecorator == null) {
                biomeDecorator = BiomeDecorator()
                it[name] = biomeDecorator
            } else {
                biomeDecorator = existingBiomeDecorator
            }
            biomeDecorator.addWeight(weight)
            decorator(biomeDecorator)
        }
    }

    fun biomeDecorators(biome: BiomeGenerator.Biome): Sequence<BiomeDecorator> {
        val biomeDecorator = biomeDecorators[biome] ?: return emptySequence()
        return biomeDecorator.values.asSequence()
    }

    fun modelPigShared(): Deferred<MobLivingModelPigShared> {
        return modelPigShared ?: throw IllegalStateException(
                "Models not available")
    }

    fun modelBellowsShared(): Deferred<EntityModelBellowsShared> {
        return modelBellowsShared ?: throw IllegalStateException(
                "Models not available")
    }

    override fun registryType(registry: Registries.RegistryAdder) {
        registry.add("VanillaBasics", "TreeType", 0, Short.MAX_VALUE.toInt())
        registry.add("VanillaBasics", "CropType", 0, Short.MAX_VALUE.toInt())
        registry.add("VanillaBasics", "StoneType", 0, Short.MAX_VALUE.toInt())
        registry.add("VanillaBasics", "CraftingRecipe", 0, Int.MAX_VALUE)
    }

    override fun register(plugins: Plugins) {
        val registry = plugins.registry
        registry.get<EnvironmentType>("Core", "Environment").run {
            environmentOverworld = reg("vanilla.basics.Overworld") {
                EnvironmentType(it, ::EnvironmentOverworldClient, { t, w ->
                    EnvironmentOverworldServer(t, w, this@VanillaBasics)
                })
            }
        }
        val cropRegistry = registry.get<CropType>("VanillaBasics", "CropType")
        cropTypes = VanillaBasicsCrops(cropRegistry::reg)
        val treeRegistry = registry.get<TreeType>("VanillaBasics", "TreeType")
        treeTypes = VanillaBasicsTrees(treeRegistry::reg)
        val stoneRegistry = registry.get<StoneType>("VanillaBasics",
                "StoneType")
        stoneTypes = VanillaBasicsStones(stoneRegistry::reg)
        entityTypes = VanillaBasicsEntities(
                registry.get<EntityType<*, *>>("Core", "Entity"))
        materials = VanillaMaterial(this, plugins)
        registerUpdates(registry)
        registerPackets(registry)
        registerOres()
        registerResearch()
        registerMetals()
        registerDecorators()
    }

    override fun init(registry: Registries) {
        biomeDecoratorOverlays.values.forEach { it ->
            biomeDecorators.values.forEach { i ->
                if (i.isEmpty()) {
                    val emptyDecorator = BiomeDecorator()
                    i.put("", emptyDecorator)
                    emptyDecorator.addWeight(1)
                    it(emptyDecorator)
                } else {
                    i.values.forEach { j -> it(j) }
                }
            }
        }
        locked = true
        registerRecipes(registry)
    }

    override fun initServer(server: ScapesServer) {
        registerCommands(server, this)
    }

    override fun initClient(game: GameStateGameMP) {
        particles = VanillaParticle(game.particleTransparentAtlasBuilder())
        modelPigShared = async(game.engine.taskExecutor) {
            MobLivingModelPigShared(game.engine)
        }
        modelBellowsShared = async(game.engine.taskExecutor) {
            EntityModelBellowsShared.load(game.engine)
        }
    }

    override fun worldInit(world: WorldServer) {
    }

    override fun worldInit(world: WorldClient) {
        val environment = world.environment as EnvironmentOverworldClient
        world.infoLayer("VanillaBasics:Climate") {
            ClimateInfoLayer(environment.climate())
        }
        world.scene.particles().register(
                ParticleEmitterExplosion(world.scene.particles()))
        world.scene.particles().register(
                ParticleEmitterRain(world.scene.particles(),
                        world.game.engine.graphics.textureEmpty()))
        world.scene.particles().register(
                ParticleEmitterSnow(world.scene.particles(),
                        world.game.engine.graphics.textureEmpty()))
        world.scene.particles().register(
                ParticleEmitterLightning(world.scene.particles()))
        world.scene.particles().register(
                ParticleEmitterTornado(world.scene.particles()))
    }

    override fun newPlayer(world: WorldClient): MobPlayerClientMain {
        return MobPlayerClientMainVB(entityTypes.player, world)
    }

    override fun newPlayer(world: WorldServer,
                           nickname: String,
                           skin: Checksum,
                           connection: PlayerConnection): MobPlayerServer {
        return MobPlayerServerVB(entityTypes.player, world, nickname, skin,
                connection)
    }

    override fun createEnvironment(world: WorldServer): EnvironmentServer {
        return environmentOverworld.createServer(world)
    }
}
