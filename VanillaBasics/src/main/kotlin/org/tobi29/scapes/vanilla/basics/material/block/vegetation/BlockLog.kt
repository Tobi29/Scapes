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

package org.tobi29.scapes.vanilla.basics.material.block.vegetation

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.launch
import org.tobi29.math.Face
import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.material.ItemTypeFuelI
import org.tobi29.scapes.vanilla.basics.material.TreeType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock
import org.tobi29.scapes.vanilla.basics.util.dropItem
import org.tobi29.utils.toArray

class BlockLog(
        type: VanillaMaterialType
) : VanillaBlock(type),
        ItemTypeFuelI<BlockType> {
    private val treeRegistry = plugins.registry.get<TreeType>("VanillaBasics",
            "TreeType")
    private var textures: Array<Pair<TerrainTexture, TerrainTexture>?>? = null
    private var models: Array<BlockModel?>? = null

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
        if ("Axe" == item.kind<ItemTypeTool>()?.toolType()) {
            launch(player.world.taskExecutor + CoroutineName("Destroy-Logs")) {
                val imTerrain = player.world.terrain
                destroy(imTerrain, x - 1, y - 1, z + 0, data, 512, player, z)
                destroy(imTerrain, x + 0, y - 1, z + 0, data, 512, player, z)
                destroy(imTerrain, x + 1, y - 1, z + 0, data, 512, player, z)
                destroy(imTerrain, x - 1, y + 0, z + 0, data, 512, player, z)
                destroy(imTerrain, x + 0, y + 0, z + 0, data, 512, player, z)
                destroy(imTerrain, x + 1, y + 0, z + 0, data, 512, player, z)
                destroy(imTerrain, x - 1, y + 1, z + 0, data, 512, player, z)
                destroy(imTerrain, x + 0, y + 1, z + 0, data, 512, player, z)
                destroy(imTerrain, x + 1, y + 1, z + 0, data, 512, player, z)
                destroy(imTerrain, x - 1, y - 1, z + 1, data, 512, player, z)
                destroy(imTerrain, x + 0, y - 1, z + 1, data, 512, player, z)
                destroy(imTerrain, x + 1, y - 1, z + 1, data, 512, player, z)
                destroy(imTerrain, x - 1, y + 0, z + 1, data, 512, player, z)
                destroy(imTerrain, x + 0, y + 0, z + 1, data, 512, player, z)
                destroy(imTerrain, x + 1, y + 0, z + 1, data, 512, player, z)
                destroy(imTerrain, x - 1, y + 1, z + 1, data, 512, player, z)
                destroy(imTerrain, x + 0, y + 1, z + 1, data, 512, player, z)
                destroy(imTerrain, x + 1, y + 1, z + 1, data, 512, player, z)
            }
        }
        return true
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        return (if ("Axe" == item.kind<ItemTypeTool>()?.toolType())
            10
        else if ("Saw" == item.kind<ItemTypeTool>()?.toolType()) 2 else -1).toDouble()
    }

    override fun drops(item: Item?,
                       data: Int): List<Item> {
        if ("Saw" == item.kind<ItemTypeTool>()?.toolType()) {
            return listOf(ItemStackData(materials.wood, data, 2))
        }
        return emptyList()
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Wood.ogg"
    }

    override fun breakSound(item: Item?,
                            data: Int): String {
        return if ("Axe" == item.kind<ItemTypeTool>()?.toolType())
            "VanillaBasics:sound/blocks/Axe.ogg"
        else
            "VanillaBasics:sound/blocks/Saw.ogg"
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        if (face == Face.UP || face == Face.DOWN) {
            return textures?.get(data)?.first
        }
        return textures?.get(data)?.second
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
                1.0, 1.0, 1.0, lod)
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = treeRegistry.values().asSequence().map {
            if (it == null) {
                return@map null
            }
            Pair(registry.registerTexture("${it.texture}/LogTop.png"),
                    registry.registerTexture("${it.texture}/LogSide.png"))
        }.toArray()
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = it.asSequence().map {
                if (it == null) {
                    return@map null
                }
                BlockModelSimpleBlock(this, registry, it.first, it.first,
                        it.second, it.second, it.second, it.second,
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
        return treeRegistry[item.data].name
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 16
    }

    private fun destroy(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int,
                        length: Int,
                        player: MobPlayerServer,
                        minZ: Int) {
        if (!terrain.modify(x, y, z) { terrain ->
            val block = terrain.block(x, y, z)
            val type = terrain.type(block)
            val d = terrain.data(block)
            if (type != this || d != data) {
                return@modify false
            }
            terrain.typeData(x, y, z, materials.air, 0)
            true
        }) {
            return
        }
        terrain.world.dropItem(ItemStackData(this, data), x, y, z)
        if (length > 0) {
            val i = length - 1
            if (z > minZ) {
                destroy(terrain, x - 1, y - 1, z - 1, data, i, player, minZ)
                destroy(terrain, x + 0, y - 1, z - 1, data, i, player, minZ)
                destroy(terrain, x + 1, y - 1, z - 1, data, i, player, minZ)
                destroy(terrain, x - 1, y + 0, z - 1, data, i, player, minZ)
                destroy(terrain, x + 0, y + 0, z - 1, data, i, player, minZ)
                destroy(terrain, x + 1, y + 0, z - 1, data, i, player, minZ)
                destroy(terrain, x - 1, y + 1, z - 1, data, i, player, minZ)
                destroy(terrain, x + 0, y + 1, z - 1, data, i, player, minZ)
                destroy(terrain, x + 1, y + 1, z - 1, data, i, player, minZ)
            }
            destroy(terrain, x - 1, y - 1, z + 0, data, i, player, minZ)
            destroy(terrain, x + 0, y - 1, z + 0, data, i, player, minZ)
            destroy(terrain, x + 1, y - 1, z + 0, data, i, player, minZ)
            destroy(terrain, x - 1, y + 0, z + 0, data, i, player, minZ)
            destroy(terrain, x + 0, y + 0, z + 0, data, i, player, minZ)
            destroy(terrain, x + 1, y + 0, z + 0, data, i, player, minZ)
            destroy(terrain, x - 1, y + 1, z + 0, data, i, player, minZ)
            destroy(terrain, x + 0, y + 1, z + 0, data, i, player, minZ)
            destroy(terrain, x + 1, y + 1, z + 0, data, i, player, minZ)
            destroy(terrain, x - 1, y - 1, z + 1, data, i, player, minZ)
            destroy(terrain, x + 0, y - 1, z + 1, data, i, player, minZ)
            destroy(terrain, x + 1, y - 1, z + 1, data, i, player, minZ)
            destroy(terrain, x - 1, y + 0, z + 1, data, i, player, minZ)
            destroy(terrain, x + 0, y + 0, z + 1, data, i, player, minZ)
            destroy(terrain, x + 1, y + 0, z + 1, data, i, player, minZ)
            destroy(terrain, x - 1, y + 1, z + 1, data, i, player, minZ)
            destroy(terrain, x + 0, y + 1, z + 1, data, i, player, minZ)
            destroy(terrain, x + 1, y + 1, z + 1, data, i, player, minZ)
        }
    }

    override fun fuelTemperature(item: TypedItem<BlockType>) = 0.2

    override fun fuelTime(item: TypedItem<BlockType>) = 80.0

    override fun fuelTier(item: TypedItem<BlockType>) = 10
}
