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
import org.tobi29.scapes.vanilla.basics.material.CropType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class BlockCrop(materials: VanillaMaterial,
                private val cropRegistry: GameRegistry.Registry<CropType>) : VanillaBlock(
        materials, "vanilla.basics.block.Crop") {
    private var textures: Array<Array<TerrainTexture>?>? = null
    private var models: Array<Array<BlockModel>?>? = null

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
        return false
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return 0.0
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        val dropData = (data / 8).toShort()
        if (data % 8 == 7) {
            val random = ThreadLocalRandom.current()
            return listOf(
                    ItemStack(materials.cropDrop, dropData.toInt()),
                    ItemStack(materials.seed, dropData.toInt(),
                            random.nextInt(2) + 1))
        }
        return emptyList()
    }

    override fun footStepSound(data: Int) = null

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Foliage.ogg"
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        return textures?.get(data shr 3)?.get(data % 8)
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
        models?.get(data shr 3)?.get(data % 8)?.addToChunkMesh(mesh, terrain, x,
                y, z, xx, yy, zz, 1.0, 1.0, 1.0, 1.0, lod)
    }

    override fun update(terrain: TerrainServer.TerrainMutable,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        if (terrain.type(x, y, z - 1) !== materials.farmland) {
            terrain.typeData(x, y, z, terrain.air,
                    0.toShort().toInt())
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = cropRegistry.values().asSequence().map {
            if (it == null) {
                return@map null
            }
            return@map (1..8).asSequence().map { i ->
                registry.registerTexture(
                        it.texture() + "/Crop" + i + ".png")
            }.toArray()
        }.toArray()
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = it.asSequence().map {
                if (it == null) {
                    return@map null
                }
                it.asSequence().map {
                    val shapes = ArrayList<BlockModelComplex.Shape>()
                    val shape = BlockModelComplex.ShapeBillboard(it, -8.0, -8.0,
                            -8.0, 8.0, 8.0, 8.0, 1.0, 1.0, 1.0, 1.0)
                    shape.rotateZ(45.0)
                    shapes.add(shape)
                    BlockModelComplex(registry, shapes, 0.0625)
                }.toArray<BlockModel>()
            }.toArray()
        }
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
    }

    override fun name(item: ItemStack): String {
        return cropRegistry[item.data()].name()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 64
    }

    companion object {
        private val SELECTION = AABB(0.15, 0.15, 0.0, 0.85, 0.85, 0.95)
    }
}
