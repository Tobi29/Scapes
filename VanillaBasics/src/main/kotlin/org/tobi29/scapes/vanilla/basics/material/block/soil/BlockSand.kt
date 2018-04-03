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
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.math.Face
import org.tobi29.stdex.math.floorToInt
import org.tobi29.utils.toArray
import org.tobi29.scapes.inventory.*
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatableI
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType

class BlockSand(type: VanillaMaterialType) : BlockSoil(
        type),
        ItemDefaultHeatableI<BlockType> {
    private var textures: Array<TerrainTexture?>? = null
    private var models: Array<BlockModel?>? = null

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
        return textures?.get(data)
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
        models?.get(data)?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                1.0,
                1.0,
                1.0, 1.0, lod)
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = arrayOf(registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Sand.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/soil/Gravel.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/soil/Clay.png"))
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = it.asSequence().map {
                BlockModelSimpleBlock(this, registry, it, it, it, it, it, it,
                        1.0, 1.0, 1.0, 1.0)
            }.toArray()
        }
    }

    override fun render(item: TypedItem<BlockType>,
                        gl: GL,
                        shader: Shader) {
        models?.get(item.data)?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<BlockType>,
                                 gl: GL,
                                 shader: Shader) {
        models?.get(item.data)?.renderInventory(gl, shader)
    }

    override fun name(item: TypedItem<BlockType>): String {
        if (item.data == 1) return "Gravel"
        val name = StringBuilder(40)
        name.append(when (item.data) {
            2 -> "Clay"
            else -> "Sand"
        })
        val temperature = temperature(item)
        name.append("\nTemp.:").append(temperature.floorToInt()).append("°C")
        return name.toString()
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 16
    }

    override fun heatTransferFactor(item: TypedItem<BlockType>) = 0.001

    override fun temperatureUpdated(item: TypedItem<BlockType>): Item? {
        if (temperature(item) >= meltingPoint(item)) {
            if (item.data == 0) {
                return ItemStack(type = materials.glass, amount = item.amount)
            } else if (item.data == 2) {
                return ItemStack(type = materials.brick, amount = item.amount)
            }
        }
        return item
    }

    fun meltingPoint(item: TypedItem<BlockType>): Float {
        when (item.data) {
            1 -> return 0.0f
            2 -> return 600.0f
            else -> return 1000.0f
        }
    }
}
