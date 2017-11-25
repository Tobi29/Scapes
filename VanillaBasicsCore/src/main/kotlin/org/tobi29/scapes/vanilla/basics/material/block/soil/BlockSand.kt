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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.math.Face
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.vanilla.basics.material.ItemDefaultHeatable
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType

class BlockSand(type: VanillaMaterialType) : BlockSoil(
        type), ItemDefaultHeatable {
    private var textures: Array<TerrainTexture?>? = null
    private var models: Array<BlockModel?>? = null

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

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        models?.get(item.data())?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        models?.get(item.data())?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        when (item.data()) {
            1 -> return "Gravel"
            2 -> return "Clay\nTemp.: " + temperature(item) + " C"
            else -> return "Sand\nTemp.: " + temperature(item) + " C"
        }
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }

    override fun heatTransferFactor(item: ItemStack) = 0.001

    override fun temperatureUpdated(item: ItemStack) {
        if (temperature(item) >= meltingPoint(item)) {
            if (item.data() == 0) {
                item.setMaterial(materials.glass)
            } else if (item.data() == 2) {
                item.setMaterial(materials.brick)
            }
        }
    }

    fun meltingPoint(item: ItemStack): Float {
        when (item.data()) {
            1 -> return 0.0f
            2 -> return 600.0f
            else -> return 1000.0f
        }
    }
}
