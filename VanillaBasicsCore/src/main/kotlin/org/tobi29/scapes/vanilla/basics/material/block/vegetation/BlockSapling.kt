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

import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.*
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.math.AABB
import org.tobi29.math.Face
import org.tobi29.math.PointerPane
import org.tobi29.math.threadLocalRandom
import org.tobi29.utils.Pool
import org.tobi29.utils.toArray
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.TreeType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock
import org.tobi29.scapes.vanilla.basics.material.update.UpdateSaplingGrowth
import org.tobi29.scapes.vanilla.basics.util.dropItems

class BlockSapling(type: VanillaMaterialType) : VanillaBlock(type) {
    private val treeRegistry = plugins.registry.get<TreeType>("VanillaBasics",
            "TreeType")
    private var textures: Array<TerrainTexture?>? = null
    private var models: Array<BlockModel?>? = null
    private var modelsItem: Array<ItemModel?>? = null

    override fun addPointerCollision(data: Int,
                                     pointerPanes: Pool<PointerPane>,
                                     x: Int,
                                     y: Int,
                                     z: Int) {
        pointerPanes.push().set(SELECTION, Face.UP, x, y, z)
        pointerPanes.push().set(SELECTION, Face.DOWN, x, y, z)
        pointerPanes.push().set(SELECTION, Face.NORTH, x, y, z)
        pointerPanes.push().set(SELECTION, Face.EAST, x, y, z)
        pointerPanes.push().set(SELECTION, Face.SOUTH, x, y, z)
        pointerPanes.push().set(SELECTION, Face.WEST, x, y, z)
    }

    override fun addCollision(aabbs: Pool<AABBElement>,
                              terrain: Terrain,
                              x: Int,
                              y: Int,
                              z: Int) {
    }

    override fun collision(data: Int,
                           x: Int,
                           y: Int,
                           z: Int): List<AABBElement> {
        return ArrayList()
    }

    override fun place(terrain: TerrainMutableServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        if (!super.place(terrain, x, y, z, face, player)) {
            return false
        }
        if (terrain.isSolid(x, y, z - 1)) {
            val random = threadLocalRandom()
            terrain.addDelayedUpdate(
                    UpdateSaplingGrowth(player.world.registry).set(x, y, z,
                            random.nextDouble() * 3600.0 + 3600.0))
            return true
        }
        return false
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        return 0.0
    }

    override fun footStepSound(data: Int) = null

    override fun breakSound(item: Item?,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Foliage.ogg"
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        return textures?.get(data)
    }

    override fun isSolid(data: Int) = false

    override fun isTransparent(data: Int) = true

    override fun lightTrough(data: Int) = (-1).toByte()

    override fun connectStage(terrain: TerrainClient,
                              x: Int,
                              y: Int,
                              z: Int): Int {
        return -1
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

    override fun update(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        val world = terrain.world
        terrain.modify(x, y, z - 1, 1, 1, 2) { terrain ->
            if (!terrain.isSolid(x, y, z - 1)) {
                world.dropItems(drops(null, data), x, y, z)
                terrain.typeData(x, y, z, terrain.air, 0)
            }
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = treeRegistry.values().asSequence().map {
            if (it == null) {
                return@map null
            }
            registry.registerTexture("${it.texture}/Sapling.png")
        }.toArray()
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = it.asSequence().map {
                if (it == null) {
                    return@map null
                }
                val shapes = ArrayList<BlockModelComplex.Shape>()
                val shape = BlockModelComplex.ShapeBillboard(it, -8.0, -8.0,
                        -8.0, 8.0, 8.0, 8.0, 1.0, 1.0, 1.0, 1.0)
                shape.rotateZ(45.0)
                shapes.add(shape)
                BlockModelComplex(registry, shapes, 0.0625)
            }.toArray()
            modelsItem = it.asSequence().map {
                ItemModelSimple(it!!, 1.0, 1.0, 1.0, 1.0)
            }.toArray()
        }
    }

    override fun render(item: TypedItem<BlockType>,
                        gl: GL,
                        shader: Shader) {
        modelsItem?.get(item.data)?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<BlockType>,
                                 gl: GL,
                                 shader: Shader) {
        modelsItem?.get(item.data)?.renderInventory(gl, shader)
    }

    override fun name(item: TypedItem<BlockType>): String {
        return materials.log.name(item) + " Sapling"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 64
    }

    companion object {
        private val SELECTION = AABB(0.15, 0.15, 0.0, 0.85, 0.85, 0.95)
    }
}
