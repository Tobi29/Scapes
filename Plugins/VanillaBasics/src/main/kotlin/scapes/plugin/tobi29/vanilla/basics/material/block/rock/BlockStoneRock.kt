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

package scapes.plugin.tobi29.vanilla.basics.material.block.rock

import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.chunk.data.ChunkMesh
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.PointerPane
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.server.MobPlayerServer
import scapes.plugin.tobi29.vanilla.basics.material.StoneType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.block.VanillaBlock
import java.util.*

class BlockStoneRock(materials: VanillaMaterial,
                     private val stoneRegistry: GameRegistry.Registry<StoneType>) : VanillaBlock(
        materials, "vanilla.basics.block.StoneRock") {
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
        return ArrayList()
    }

    override fun isReplaceable(terrain: Terrain,
                               x: Int,
                               y: Int,
                               z: Int): Boolean {
        return true
    }

    override fun place(terrain: TerrainServer.TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        return terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1)
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return 0.0
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Stone.ogg"
    }

    override fun breakSound(item: ItemStack,
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

    override fun isSolid(terrain: Terrain,
                         x: Int,
                         y: Int,
                         z: Int): Boolean {
        return false
    }

    override fun isTransparent(terrain: Terrain,
                               x: Int,
                               y: Int,
                               z: Int): Boolean {
        return true
    }

    override fun lightTrough(terrain: Terrain,
                             x: Int,
                             y: Int,
                             z: Int): Byte {
        return -1
    }

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

    override fun update(terrain: TerrainServer.TerrainMutable,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        if (!terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1)) {
            terrain.world.dropItems(drops(ItemStack(materials.air, 0), data), x,
                    y, z)
            terrain.typeData(x, y, z, terrain.air, 0)
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = stoneRegistry.values().asSequence().map {
            it?.let {
                return@map registry.registerTexture(
                        it.textureRoot() + "/raw/" + it.texture() + ".png")
            }
        }.toArray()
        texturesItem = stoneRegistry.values().asSequence().map {
            it?.let {
                return@map registry.registerTexture(
                        "VanillaBasics:image/terrain/stone/rock/" + it.texture() + ".png")
            }
        }.toArray()
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = it.asSequence().map {
                SELECTION.asSequence().map { i ->
                    val shapes = ArrayList<BlockModelComplex.Shape>()
                    val shape = BlockModelComplex.ShapeBox(it, it, it, it, it,
                            it, i.minX - 0.5, i.minY - 0.5, i.minZ - 0.5,
                            i.maxX - 0.5, i.maxY - 0.5, i.maxZ - 0.5, 1.0, 1.0,
                            1.0, 1.0)
                    shapes.add(shape)
                    BlockModelComplex(registry, shapes, 1.0)
                }.toArray<BlockModel>()
            }.toArray()
        }
        texturesItem?.let {
            modelsItem = it.asSequence().map {
                ItemModelSimple(it, 1.0, 1.0, 1.0, 1.0)
            }.toArray()
        }
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        modelsItem?.get(item.data())?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        modelsItem?.get(item.data())?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return materials.stoneRaw.name(item) + " Rock"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 128
    }

    companion object {
        private val SELECTION = arrayOf(
                AABB(0.375, 0.375, 0.0, 0.625, 0.625, 0.0625),
                AABB(0.375, 0.25, 0.0, 0.625, 0.75, 0.0625),
                AABB(0.4375, 0.375, 0.0, 0.5625, 0.625, 0.0625),
                AABB(0.4375, 0.25, 0.0, 0.5625, 0.75, 0.0625),
                AABB(0.375, 0.375, 0.0, 0.625, 0.625, 0.125),
                AABB(0.375, 0.25, 0.0, 0.625, 0.75, 0.125),
                AABB(0.4375, 0.375, 0.0, 0.5625, 0.625, 0.125),
                AABB(0.4375, 0.25, 0.0, 0.5625, 0.75, 0.125))
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
