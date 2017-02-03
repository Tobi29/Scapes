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

package scapes.plugin.tobi29.vanilla.basics.material.block.soil

import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.data.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.server.MobPlayerServer
import scapes.plugin.tobi29.vanilla.basics.entity.server.EntityFarmlandServer
import scapes.plugin.tobi29.vanilla.basics.generator.ClimateInfoLayer
import scapes.plugin.tobi29.vanilla.basics.generator.EnvironmentClimate
import scapes.plugin.tobi29.vanilla.basics.material.CropType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.block.VanillaBlock
import scapes.plugin.tobi29.vanilla.basics.material.update.UpdateGrassGrowth
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class BlockGrass(materials: VanillaMaterial,
                 private val cropRegistry: GameRegistry.Registry<CropType>) : VanillaBlock(
        materials, "vanilla.basics.block.Grass") {
    private var textureTop: TerrainTexture? = null
    private var textureSide1Dirt: TerrainTexture? = null
    private var textureSide1Sand: TerrainTexture? = null
    private var textureSide2: TerrainTexture? = null
    private var textureBottomDirt: TerrainTexture? = null
    private var textureBottomSand: TerrainTexture? = null
    private var texturesGrass: Array<TerrainTexture?>? = null
    private var modelBlockGrass: BlockModel? = null
    private var modelBlockDirt: BlockModel? = null
    private var modelBlockSand: BlockModel? = null
    private var modelBlockFastGrass: BlockModel? = null
    private var modelBlockFastDirt: BlockModel? = null
    private var modelBlockFastSand: BlockModel? = null
    private var modelsTallGrass: Array<BlockModel?>? = null

    override fun destroy(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: ItemStack): Boolean {
        if ("Hoe" == item.material().toolType(item)) {
            if (data > 0) {
                if (item.material().toolLevel(item) >= 10) {
                    terrain.world.dropItem(
                            ItemStack(materials.grassBundle, 0, data), x, y,
                            z + 1)
                    terrain.data(x, y, z, 0)
                } else {
                    terrain.world.dropItem(ItemStack(materials.grassBundle, 0),
                            x, y, z + 1)
                    terrain.data(x, y, z, data - 1)
                }
                val random = ThreadLocalRandom.current()
                if (random.nextInt(20) == 0) {
                    terrain.world.dropItem(ItemStack(materials.seed,
                            random.nextInt(cropRegistry.values().size)), x, y,
                            z + 1)
                }
            } else {
                terrain.type(x, y, z, materials.farmland)
                terrain.world.addEntity(EntityFarmlandServer(terrain.world,
                        Vector3d(x + 0.5, y + 0.5, z + 0.5), 0.5f, 0.5f, 0.5f))
            }
            return false
        }
        return true
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return if ("Shovel" == item.material().toolType(item))
            3.0
        else if ("Hoe" == item.material().toolType(item)) 0.2 else 30.0
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        return listOf(ItemStack(materials.dirt, 0))
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Grass.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Stone.ogg"
    }

    override fun particleColorR(face: Face,
                                terrain: TerrainClient,
                                x: Int,
                                y: Int,
                                z: Int,
                                data: Int): Float {
        if (face != Face.DOWN) {
            val environment = terrain.world.environment
            if (environment is EnvironmentClimate) {
                val climateGenerator = environment.climate()
                return climateGenerator.grassColorR(
                        climateGenerator.temperature(x, y, z),
                        climateGenerator.humidity(x, y, z)).toFloat()
            }
        }
        return 1.0f
    }

    override fun particleColorG(face: Face,
                                terrain: TerrainClient,
                                x: Int,
                                y: Int,
                                z: Int,
                                data: Int): Float {
        if (face != Face.DOWN) {
            val environment = terrain.world.environment
            if (environment is EnvironmentClimate) {
                val climateGenerator = environment.climate()
                return climateGenerator.grassColorG(
                        climateGenerator.temperature(x, y, z),
                        climateGenerator.humidity(x, y, z)).toFloat()
            }
        }
        return 1.0f
    }

    override fun particleColorB(face: Face,
                                terrain: TerrainClient,
                                x: Int,
                                y: Int,
                                z: Int,
                                data: Int): Float {
        if (face != Face.DOWN) {
            val environment = terrain.world.environment
            if (environment is EnvironmentClimate) {
                val climateGenerator = environment.climate()
                return climateGenerator.grassColorB(
                        climateGenerator.temperature(x, y, z),
                        climateGenerator.humidity(x, y, z)).toFloat()
            }
        }
        return 1.0f
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        if (face != Face.DOWN) {
            return textureTop
        }
        if (BlockDirt.isSand(terrain, x, y, z)) {
            return textureBottomSand
        } else {
            return textureBottomDirt
        }
    }

    override fun addToChunkMesh(mesh: ChunkMesh,
                                meshAlpha: ChunkMesh,
                                data: Int,
                                terrain: TerrainClient,
                                info: TerrainRenderInfo,
                                x: Int,
                                y: Int,
                                z: Int,
                                xx: Double,
                                yy: Double,
                                zz: Double,
                                lod: Boolean) {
        val grassR: Double
        val grassG: Double
        val grassB: Double
        val sand: Boolean
        val environment = terrain.world.environment
        if (environment is EnvironmentClimate) {
            val climateGenerator = environment.climate()
            val climateLayer = info.get<ClimateInfoLayer>(
                    "VanillaBasics:Climate")
            val temperature = climateLayer.temperature(x, y, z)
            val humidity = climateLayer.humidity(x, y)
            grassR = climateGenerator.grassColorR(temperature, humidity)
            grassG = climateGenerator.grassColorG(temperature, humidity)
            grassB = climateGenerator.grassColorB(temperature, humidity)
            sand = humidity < 0.3
        } else {
            grassR = 1.0
            grassG = 1.0
            grassB = 1.0
            sand = false
        }
        if (lod) {
            modelBlockGrass?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                    grassR, grassG, grassB, 1.0, lod)
            if (sand) {
                modelBlockSand?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy,
                        zz, 1.0, 1.0, 1.0, 1.0, lod)
            } else {
                modelBlockDirt?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy,
                        zz, 1.0, 1.0, 1.0, 1.0, lod)
            }
            if (data > 0) {
                if (terrain.type(x, y, z + 1) === materials.air) {
                    modelsTallGrass?.get(data - 1)?.addToChunkMesh(mesh,
                            terrain, x, y, z + 1, xx, yy, zz + 1,
                            grassR, grassG, grassB, 1.0, lod)
                }
            }
        } else {
            modelBlockFastGrass?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy,
                    zz, grassR, grassG, grassB, 1.0, lod)
            if (sand) {
                modelBlockFastSand?.addToChunkMesh(mesh, terrain, x, y, z, xx,
                        yy, zz, 1.0, 1.0, 1.0, 1.0, lod)
            } else {
                modelBlockFastDirt?.addToChunkMesh(mesh, terrain, x, y, z, xx,
                        yy, zz, 1.0, 1.0, 1.0, 1.0, lod)
            }
        }
    }

    override fun update(terrain: TerrainServer.TerrainMutable,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        if (terrain.blockLight(x, y, z + 1) <= 0 && terrain.sunLight(x, y,
                z + 1) <= 0 || !terrain.type(x, y, z + 1).isTransparent(terrain,
                x, y, z + 1)) {
            terrain.typeData(x, y, z, materials.dirt, 0)
        }
        if (terrain.highestTerrainBlockZAt(x,
                y) > z + 1 && !terrain.hasDelayedUpdate(x, y, z,
                UpdateGrassGrowth::class.java)) {
            val random = ThreadLocalRandom.current()
            terrain.addDelayedUpdate(UpdateGrassGrowth().set(x, y, z,
                    random.nextDouble() * 400.0 + 1600.0))
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/GrassTop.png")
        textureSide1Dirt = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/GrassSide.png")
        textureSide1Sand = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/GrassSideSand.png")
        textureSide2 = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/GrassSideFoliage.png")
        textureBottomDirt = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Dirt.png")
        textureBottomSand = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Sand.png")
        texturesGrass = (1..8).asSequence().map {
            registry.registerTexture(
                    "VanillaBasics:image/terrain/TallGrass" +
                            it + ".png", ShaderAnimation.TALL_GRASS)
        }.toArray()
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        modelBlockGrass = BlockModelSimpleBlock(this, registry, textureTop,
                null,
                textureSide2, textureSide2, textureSide2, textureSide2,
                1.0, 1.0, 1.0, 1.0)
        modelBlockDirt = BlockModelSimpleBlock(this, registry, null,
                textureBottomDirt, textureSide1Dirt, textureSide1Dirt,
                textureSide1Dirt, textureSide1Dirt, 1.0, 1.0, 1.0, 1.0)
        modelBlockSand = BlockModelSimpleBlock(this, registry, null,
                textureBottomSand, textureSide1Sand, textureSide1Sand,
                textureSide1Sand, textureSide1Sand, 1.0, 1.0, 1.0, 1.0)
        modelBlockFastGrass = BlockModelSimpleBlock(this, registry, textureTop,
                null,
                textureTop, textureTop, textureTop, textureTop, 1.0,
                1.0, 1.0, 1.0)
        modelBlockFastDirt = BlockModelSimpleBlock(this, registry, null,
                textureBottomDirt, null, null, null, null, 1.0, 1.0, 1.0,
                1.0)
        modelBlockFastSand = BlockModelSimpleBlock(this, registry, null,
                textureBottomSand, null, null, null, null, 1.0, 1.0, 1.0,
                1.0)
        texturesGrass?.let {
            modelsTallGrass = it.asSequence().map {
                val shapes = ArrayList<BlockModelComplex.Shape>()
                val shape = BlockModelComplex.ShapeBillboard(it,
                        -8.0, -8.0, -8.0, 8.0, 8.0, 8.0, 0.0, 0.0,
                        1.0, 1.0, 1.0, 1.0, 1.0)
                shape.rotateZ(45.0)
                shapes.add(shape)
                BlockModelComplex(registry, shapes, 0.0625)
            }.toArray()
        }
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        modelBlockGrass?.render(gl, shader)
        modelBlockDirt?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        modelBlockGrass?.renderInventory(gl, shader)
        modelBlockDirt?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return "Grass"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }
}
