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

package org.tobi29.scapes.vanilla.basics.material.block.rock

import org.tobi29.math.AABB3
import org.tobi29.math.Face
import org.tobi29.math.PointerPane
import org.tobi29.math.Random
import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.*
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.generator.StoneType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock
import org.tobi29.scapes.vanilla.basics.util.dropItems
import org.tobi29.utils.Pool
import org.tobi29.utils.toArray

class BlockStoneRock(type: VanillaMaterialType) : VanillaBlock(type) {
    private val stoneRegistry = plugins.registry.get<StoneType>("VanillaBasics",
            "StoneType")
    private var textures: Array<TerrainTexture?>? = null
    private var texturesItem: Array<TerrainTexture?>? = null
    private var models: Array<Array<BlockModel>>? = null
    private var modelsItem: Array<ItemModel>? = null

    override fun addPointerCollision(data: Int,
                                     pointerPanes: Pool<PointerPane>,
                                     x: Int,
                                     y: Int,
                                     z: Int) {
        val i = PERM[x and 255 + PERM[y and 255 + PERM[z and 255]]] % 8
        pointerPanes.push().set(SELECTION[i], Face.UP, x, y, z)
        pointerPanes.push().set(SELECTION[i], Face.DOWN, x, y, z)
        pointerPanes.push().set(SELECTION[i], Face.NORTH, x, y, z)
        pointerPanes.push().set(SELECTION[i], Face.EAST, x, y, z)
        pointerPanes.push().set(SELECTION[i], Face.SOUTH, x, y, z)
        pointerPanes.push().set(SELECTION[i], Face.WEST, x, y, z)
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
        return emptyList()
    }

    override fun isReplaceable(terrain: Terrain,
                               x: Int,
                               y: Int,
                               z: Int): Boolean {
        return true
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
        val block = terrain.block(x, y, z - 1)
        return terrain.type(block).isSolid(terrain.data(block))
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        return 0.0
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Stone.ogg"
    }

    override fun breakSound(item: Item?,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Stone.ogg"
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
        val i = PERM[x and 255 + PERM[y and 255 + PERM[z and 255]]] % 8
        models?.get(data)?.get(i)?.addToChunkMesh(mesh, terrain, x, y, z, xx,
                yy, zz,
                1.0, 1.0,
                1.0, 1.0, lod)
    }

    override fun update(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        val world = terrain.world
        terrain.modify(x, y, z - 1, 1, 1, 2) { terrain ->
            val block = terrain.block(x, y, z - 1)
            if (!terrain.type(block).isSolid(terrain.data(block))) {
                world.dropItems(drops(null, data), x, y, z)
                terrain.typeData(x, y, z, terrain.air, 0)
            }
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = stoneRegistry.values().asSequence().map {
            it?.let {
                return@map registry.registerTexture(
                        "${it.textureRoot}/raw/${it.texture}.png")
            }
        }.toArray()
        texturesItem = stoneRegistry.values().asSequence().map {
            it?.let {
                return@map registry.registerTexture(
                        "VanillaBasics:image/terrain/stone/rock/${it.texture}.png")
            }
        }.toArray()
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = it.asSequence().map {
                SELECTION.asSequence().map { i ->
                    val shapes = ArrayList<BlockModelComplex.Shape>()
                    val shape = BlockModelComplex.ShapeBox(it, it, it, it, it,
                            it, i.min.x - 0.5, i.min.y - 0.5, i.min.z - 0.5,
                            i.max.x - 0.5, i.max.y - 0.5, i.max.z - 0.5, 1.0, 1.0,
                            1.0, 1.0)
                    shapes.add(shape)
                    BlockModelComplex(registry, shapes, 1.0)
                }.toArray<BlockModel>()
            }.toArray()
        }
        texturesItem?.let {
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
        return materials.stoneRaw.name(item) + " Rock"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 128
    }

    companion object {
        private val SELECTION = arrayOf(
                AABB3(0.375, 0.375, 0.0, 0.625, 0.625, 0.0625),
                AABB3(0.375, 0.25, 0.0, 0.625, 0.75, 0.0625),
                AABB3(0.4375, 0.375, 0.0, 0.5625, 0.625, 0.0625),
                AABB3(0.4375, 0.25, 0.0, 0.5625, 0.75, 0.0625),
                AABB3(0.375, 0.375, 0.0, 0.625, 0.625, 0.125),
                AABB3(0.375, 0.25, 0.0, 0.625, 0.75, 0.125),
                AABB3(0.4375, 0.375, 0.0, 0.5625, 0.625, 0.125),
                AABB3(0.4375, 0.25, 0.0, 0.5625, 0.75, 0.125))
        private val PERM = IntArray(512)

        init {
            val random = Random(0)
            var v: Int
            for (i in 0..255) {
                v = random.nextInt(256)
                PERM[i] = v
                PERM[i + 256] = v
            }
        }
    }
}
