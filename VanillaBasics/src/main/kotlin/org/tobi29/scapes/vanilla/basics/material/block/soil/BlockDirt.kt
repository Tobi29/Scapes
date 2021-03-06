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

package org.tobi29.scapes.vanilla.basics.material.block.soil

import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.math.Face
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.world.ClimateInfoLayer
import org.tobi29.scapes.vanilla.basics.world.EnvironmentClimate

class BlockDirt(type: VanillaMaterialType) : BlockSoil(type) {
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

    override fun render(item: TypedItem<BlockType>,
                        gl: GL,
                        shader: Shader) {
        modelDirt?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<BlockType>,
                                 gl: GL,
                                 shader: Shader) {
        modelDirt?.renderInventory(gl, shader)
    }

    override fun name(item: TypedItem<BlockType>): String {
        return "Dirt"
    }

    override fun destroy(terrain: TerrainMutableServer,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: Item?): Boolean {
        if (!super.destroy(terrain, x, y, z, data, face, player, item)) {
            return false
        }
        if ("Hoe" == item.kind<ItemTypeTool>()?.toolType()) {
            terrain.type(x, y, z, materials.farmland)
            materials.farmland.getEntity(player.world.terrain, x, y, z).nourish(
                    0.1)
            return false
        }
        return true
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        return (if ("Shovel" == item.kind<ItemTypeTool>()?.toolType()) 2 else 20).toDouble()
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
