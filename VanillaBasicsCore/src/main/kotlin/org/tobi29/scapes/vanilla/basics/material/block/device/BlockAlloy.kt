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

package org.tobi29.scapes.vanilla.basics.material.block.device

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.math.AABB
import org.tobi29.scapes.engine.math.Face
import org.tobi29.scapes.engine.math.PointerPane
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAlloyClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityAlloyServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer

class BlockAlloy(type: VanillaMaterialType) : VanillaBlockContainer<EntityAlloyClient, EntityAlloyServer>(
        type, type.materials.plugin.entityTypes.alloy) {
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
        aabbs.push().set(x + 0.0625, y + 0.0625, z.toDouble(), x + 0.9375,
                y + 0.9375, z + 0.5)
    }

    override fun collision(data: Int,
                           x: Int,
                           y: Int,
                           z: Int): List<AABBElement> {
        val aabbs = ArrayList<AABBElement>()
        aabbs.add(AABBElement(
                AABB(x + 0.0625, y + 0.0625, z.toDouble(), x + 0.9375,
                        y + 0.9375, z + 0.5)))
        return aabbs
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return (if ("Pickaxe" == item.material().toolType(
                item)) 1 else 240).toDouble()
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        return emptyList()
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
        return texture
    }

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
        model?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0, 1.0,
                1.0, 1.0, lod)
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/stone/raw/Granite.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        val shapes = ArrayList<BlockModelComplex.Shape>()
        shapes.add(BlockModelComplex.ShapeBox(texture, texture, texture,
                texture, texture, texture, -7.0, -7.0, -8.0, 7.0, 7.0,
                0.0, 1.0, 1.0, 1.0, 1.0))
        model = BlockModelComplex(registry, shapes, 0.0625)
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        model?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        model?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return "Alloy Mold"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }

    companion object {
        private val SELECTION = AABB(0.0625, 0.0625, 0.0, 0.9375, 0.9375, 0.5)
    }
}
