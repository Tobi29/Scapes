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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.data.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.server.MobPlayerServer
import scapes.plugin.tobi29.vanilla.basics.entity.server.EntityFarmlandServer
import scapes.plugin.tobi29.vanilla.basics.generator.ClimateInfoLayer
import scapes.plugin.tobi29.vanilla.basics.generator.EnvironmentClimate
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial

class BlockDirt(materials: VanillaMaterial) : BlockSoil(materials,
        "vanilla.basics.block.Dirt") {
    private var textureDirt: TerrainTexture? = null
    private var textureSand: TerrainTexture? = null
    private var modelDirt: BlockModel? = null
    private var modelSand: BlockModel? = null

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureDirt = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Dirt.png")
        textureSand = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Sand.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        modelDirt = BlockModelSimpleBlock(this, registry, textureDirt,
                textureDirt, textureDirt, textureDirt, textureDirt, textureDirt,
                1.0, 1.0, 1.0, 1.0)
        modelSand = BlockModelSimpleBlock(this, registry, textureSand,
                textureSand, textureSand, textureSand, textureSand, textureSand,
                1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        modelDirt?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        modelDirt?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return "Dirt"
    }

    override fun destroy(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: ItemStack): Boolean {
        if ("Hoe" == item.material().toolType(item)) {
            terrain.type(x, y, z, materials.farmland)
            terrain.world.addEntity(EntityFarmlandServer(terrain.world,
                    Vector3d(x + 0.5, y + 0.5, z + 0.5), 0.1f, 0.1f, 0.1f))
            return false
        }
        return true
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return (if ("Shovel" == item.material().toolType(
                item)) 2 else 20).toDouble()
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        if (isSand(terrain, x, y, z)) {
            return textureSand
        } else {
            return textureDirt
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
        val climateLayer = info.get<ClimateInfoLayer>("VanillaBasics:Climate")
        val humidity = climateLayer.humidity(x, y)
        if (humidity < 0.3) {
            modelSand?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0,
                    1.0, 1.0, 1.0, lod)
        } else {
            modelDirt?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0,
                    1.0, 1.0, 1.0, lod)
        }
    }

    companion object {

        fun isSand(terrain: TerrainClient,
                   x: Int,
                   y: Int,
                   z: Int): Boolean {
            val environment = terrain.world.environment
            if (environment is EnvironmentClimate) {
                val climateGenerator = environment.climate()
                val humidity = climateGenerator.humidity(x, y, z)
                if (humidity < 0.3) {
                    return true
                }
            }
            return false
        }
    }
}
