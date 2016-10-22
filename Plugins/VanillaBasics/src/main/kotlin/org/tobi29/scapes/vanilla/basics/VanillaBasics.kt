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

package org.tobi29.scapes.vanilla.basics

import java8.util.concurrent.ConcurrentMaps
import java8.util.stream.Stream
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.chunk.EnvironmentClient
import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.engine.utils.Checksum
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.entity.client.MobPlayerClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.plugins.WorldType
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientVB
import org.tobi29.scapes.vanilla.basics.entity.model.EntityModelBellowsShared
import org.tobi29.scapes.vanilla.basics.entity.model.MobLivingModelPigShared
import org.tobi29.scapes.vanilla.basics.entity.particle.*
import org.tobi29.scapes.vanilla.basics.entity.server.MobPlayerServerVB
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.generator.ClimateInfoLayer
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldClient
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentOverworldServer
import org.tobi29.scapes.vanilla.basics.generator.decorator.BiomeDecorator
import org.tobi29.scapes.vanilla.basics.material.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class VanillaBasics : WorldType {
    private val craftingRecipes = ArrayList<CraftingRecipeType>()
    private val researchRecipes = ArrayList<ResearchRecipe>()
    private val metalTypes = ConcurrentHashMap<String, MetalType>()
    private val alloyTypes = ConcurrentHashMap<String, AlloyType>()
    private val crapMetal = MetalType("CrapMetal", "Crap Metal", 200.0, 0.17f,
            0.12f, 0.1f)
    private val crapAlloy = AlloyType("CrapMetal", "Crap Metal", "Crap Metal",
            Collections.singletonMap(crapMetal, 1.0), 0.17f, 0.12f,
            0.1f, 0.0, 0.0, 0.0, 0)
    private val oreTypes = ArrayList<OreType>()
    private val biomeDecorators = EnumMap<BiomeGenerator.Biome, MutableMap<String, BiomeDecorator>>(
            BiomeGenerator.Biome::class.java)
    private val biomeDecoratorOverlays = ConcurrentHashMap<String, BiomeDecorator.() -> Unit>()
    lateinit var materials: VanillaMaterial
    lateinit var particles: VanillaParticle
    private var modelPigShared: MobLivingModelPigShared? = null
    private var modelBellowsShared: EntityModelBellowsShared? = null
    private var locked = false

    init {
        BiomeGenerator.Biome.values().forEach {
            biomeDecorators.put(it, ConcurrentHashMap())
        }
    }

    fun addCraftingRecipe(recipe: CraftingRecipeType) {
        craftingRecipes.add(recipe)
    }

    fun craftingRecipes(): Stream<CraftingRecipeType> {
        return craftingRecipes.stream()
    }

    fun addResearchRecipe(researchRecipe: ResearchRecipe) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        researchRecipes.add(researchRecipe)
    }

    fun researchRecipes(): Stream<ResearchRecipe> {
        return researchRecipes.stream()
    }

    fun addMetalType(metalType: MetalType) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        metalTypes.put(metalType.id(), metalType)
    }

    fun metalType(id: String): MetalType {
        return ConcurrentMaps.getOrDefault(metalTypes, id, crapMetal)
    }

    fun addAlloyType(alloyType: AlloyType) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        alloyTypes.put(alloyType.id(), alloyType)
    }

    fun alloyType(id: String): AlloyType {
        return ConcurrentMaps.getOrDefault(alloyTypes, id, crapAlloy)
    }

    fun alloyTypes(): Stream<AlloyType> {
        return alloyTypes.values.stream()
    }

    fun addOreType(oreType: OreType) {
        if (locked) {
            throw IllegalStateException("Initializing already ended")
        }
        oreTypes.add(oreType)
    }

    fun oreTypes(): Stream<OreType> {
        return oreTypes.stream()
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

    fun biomeDecorators(biome: BiomeGenerator.Biome): Stream<BiomeDecorator> {
        val biomeDecorator = biomeDecorators[biome] ?: return stream()
        return biomeDecorator.values.stream()
    }

    fun modelPigShared(): MobLivingModelPigShared {
        return modelPigShared ?: throw IllegalStateException(
                "Models not available")
    }

    fun modelBellowsShared(): EntityModelBellowsShared {
        return modelBellowsShared ?: throw IllegalStateException(
                "Models not available")
    }

    override fun registryType(registry: GameRegistry.RegistryAdder) {
        registry.add("VanillaBasics", "TreeType", 0, Short.MAX_VALUE.toInt())
        registry.add("VanillaBasics", "CropType", 0, Short.MAX_VALUE.toInt())
        registry.add("VanillaBasics", "StoneType", 0, Short.MAX_VALUE.toInt())
        registry.add("VanillaBasics", "CraftingRecipe", 0, Int.MAX_VALUE)
    }

    override fun register(registry: GameRegistry) {
        val environmentRegistry = registry.getAsymSupplier<WorldServer, EnvironmentServer, WorldClient, EnvironmentClient>(
                "Core", "Environment")
        environmentRegistry.reg({ EnvironmentOverworldServer(it, this) },
                ::EnvironmentOverworldClient,
                EnvironmentOverworldServer::class.java,
                "vanilla.basics.Overworld")
        registerTreeTypes(registry)
        registerCropTypes(registry)
        registerStoneTypes(registry)
        materials = VanillaMaterial(this, registry)
        registerEntities(registry)
        registerUpdates(registry)
        registerPackets(registry)
        registerOres()
        registerResearch()
        registerMetals()
        registerDecorators()
    }

    override fun init(registry: GameRegistry) {
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
        particles = VanillaParticle(game.particleTransparentAtlas())
        modelPigShared = MobLivingModelPigShared(game.engine)
        modelBellowsShared = EntityModelBellowsShared(game.engine)
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
                        world.game.engine.graphics.textures().empty()))
        world.scene.particles().register(
                ParticleEmitterSnow(world.scene.particles(),
                        world.game.engine.graphics.textures().empty()))
        world.scene.particles().register(
                ParticleEmitterLightning(world.scene.particles()))
        world.scene.particles().register(
                ParticleEmitterTornado(world.scene.particles()))
    }

    override fun id(): String {
        return "VanillaBasics"
    }

    override fun assetRoot(): String {
        return "assets/scapes/tobi29/vanilla/basics/"
    }

    override fun playerSupplier(): (WorldClient) -> MobPlayerClient {
        return { MobPlayerClientVB(it) }
    }

    override fun playerClass(): Class<out MobPlayerServer> {
        return MobPlayerServerVB::class.java
    }

    override fun newPlayer(world: WorldClient,
                           pos: Vector3d,
                           speed: Vector3d,
                           xRot: Double,
                           zRot: Double,
                           nickname: String): MobPlayerClientMain {
        return MobPlayerClientMainVB(world, pos, speed, xRot, zRot,
                nickname)
    }

    override fun newPlayer(world: WorldServer,
                           pos: Vector3d,
                           speed: Vector3d,
                           xRot: Double,
                           zRot: Double,
                           nickname: String,
                           skin: Checksum,
                           connection: PlayerConnection): MobPlayerServer {
        return MobPlayerServerVB(world, pos, speed, xRot, zRot, nickname,
                skin, connection)
    }

    override fun createEnvironment(world: WorldServer): EnvironmentServer {
        return EnvironmentOverworldServer(world, this)
    }
}
