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

package scapes.plugin.tobi29.vanilla.basics.material.block.soil

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.data.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.io.tag.getFloat
import org.tobi29.scapes.engine.utils.io.tag.setFloat
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.server.MobItemServer
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemHeatable

class BlockSand(materials: VanillaMaterial) : BlockSoil(materials,
        "vanilla.basics.block.Sand"), ItemHeatable {
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
        return if (temperature(item) == 0f) 16 else 1
    }

    override fun heat(item: ItemStack,
                      temperature: Float) {
        if (item.data() == 1) {
            return
        }
        var currentTemperature = temperature(item)
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            currentTemperature += (temperature - currentTemperature) / 400.0f
            item.metaData("Vanilla").setFloat("Temperature", currentTemperature)
            if (currentTemperature >= meltingPoint(item)) {
                if (item.data() == 0) {
                    item.setMaterial(materials.glass)
                } else if (item.data() == 2) {
                    item.setMaterial(materials.brick)
                }
            }
        }
    }

    override fun cool(item: ItemStack) {
        if (item.data() == 1) {
            return
        }
        val currentTemperature = temperature(item)
        if (currentTemperature < 1) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            item.metaData("Vanilla").setFloat("Temperature",
                    currentTemperature / 1.002f)
        }
    }

    override fun cool(item: MobItemServer) {
        if (item.item().data() == 1) {
            return
        }
        val currentTemperature = temperature(item.item())
        if (currentTemperature < 1) {
            item.item().metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            if (item.isInWater) {
                item.item().metaData("Vanilla").setFloat("Temperature",
                        currentTemperature / 4.0f)
            } else {
                item.item().metaData("Vanilla").setFloat("Temperature",
                        currentTemperature / 1.002f)
            }
        }
    }

    override fun meltingPoint(item: ItemStack): Float {
        when (item.data()) {
            1 -> return 0.0f
            2 -> return 600.0f
            else -> return 1000.0f
        }
    }

    override fun temperature(item: ItemStack): Float {
        if (item.data() == 1) {
            return 0.0f
        }
        return item.metaData("Vanilla").getFloat("Temperature") ?: 0.0f
    }
}
