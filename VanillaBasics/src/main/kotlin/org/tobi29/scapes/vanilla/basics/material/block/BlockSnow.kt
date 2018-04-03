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

package org.tobi29.scapes.vanilla.basics.material.block

import org.tobi29.math.AABB3
import org.tobi29.math.Face
import org.tobi29.math.PointerPane
import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelLiquid
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.utils.Pool

class BlockSnow(type: VanillaMaterialType) : VanillaBlock(type) {
    private var texture: TerrainTexture? = null
    private var model: BlockModel? = null

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

    override fun resistance(item: Item?,
                            data: Int): Double {
        return 1.0
    }

    override fun drops(item: Item?,
                       data: Int): List<Item> {
        return emptyList()
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Snow.ogg"
    }

    override fun breakSound(item: Item?,
                            data: Int) = null

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        return texture
    }

    override fun isSolid(data: Int) = false

    override fun isTransparent(data: Int) = true

    override fun lightTrough(data: Int) = (-2).toByte()

    override fun connectStage(terrain: TerrainClient,
                              x: Int,
                              y: Int,
                              z: Int): Int {
        return 3
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
        model?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0, 1.0,
                1.0, 1.0, lod)
    }

    override fun update(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        terrain.modify(x, y, z - 1, 1, 1, 2) { terrain ->
            val type = terrain.type(x, y, z - 1)
            if (type == materials.air || type == materials.snow ||
                    type.isLiquid) {
                terrain.typeData(x, y, z, terrain.air, 0)
            }
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/Snow.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        model = BlockModelLiquid(this, registry, texture, texture, texture,
                texture, texture, texture, 1.0, 1.0, 1.0, 1.0, 0.0625,
                1.0625, 0, 8)
    }

    override fun render(item: TypedItem<BlockType>,
                        gl: GL,
                        shader: Shader) {
        model?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<BlockType>,
                                 gl: GL,
                                 shader: Shader) {
        model?.renderInventory(gl, shader)
    }

    override fun name(item: TypedItem<BlockType>): String {
        return "Snow"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 16
    }

    companion object {
        private val SELECTION = AABB3(0.0, 0.0, 0.0, 1.0, 1.0, 0.5)
    }
}
