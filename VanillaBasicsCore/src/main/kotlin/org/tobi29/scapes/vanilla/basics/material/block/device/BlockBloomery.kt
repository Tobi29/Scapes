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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBloomeryClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityBloomeryServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer

class BlockBloomery(type: VanillaMaterialType) : VanillaBlockContainer<EntityBloomeryClient, EntityBloomeryServer>(
        type, type.materials.plugin.entityTypes.bloomery) {
    private var textureSide: TerrainTexture? = null
    private var textureInside: TerrainTexture? = null
    private var model: BlockModel? = null

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return (if ("Pickaxe" == item.material().toolType(
                item)) 12 else -1).toDouble()
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
        return textureSide
    }

    override fun isTransparent(data: Int) = true

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

    override fun update(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        terrain.getEntities(x, y,
                z).filterMap<EntityBloomeryServer>().forEach { entity ->
            entity.updateBellows(terrain)
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/device/BloomerySide.png")
        textureInside = registry.registerTexture(
                "VanillaBasics:image/terrain/device/BloomeryInside.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        val shapes = ArrayList<BlockModelComplex.Shape>()
        shapes.add(BlockModelComplex.ShapeBox(textureInside, textureInside,
                null, null, null, null, -6.0, -6.0, -8.0, 6.0, 6.0, -7.0,
                1.0, 1.0, 1.0, 1.0))

        shapes.add(BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -8.0,
                -8.0, -8.0, 8.0, -6.0, -3.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(
                BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, -8.0, -6.0, -8.0,
                        -6.0, 6.0, -3.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -8.0, 6.0,
                -8.0, 8.0, 8.0, -3.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(
                BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, 6.0, -6.0, -8.0,
                        8.0, 6.0, -3.0, 1.0, 1.0, 1.0, 1.0))

        shapes.add(BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -6.0,
                -6.0, -3.0, 6.0, -4.0, 1.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(
                BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, -6.0, -4.0, -3.0,
                        -4.0, 4.0, 1.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -6.0, 4.0,
                -3.0, 6.0, 6.0, 1.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(
                BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, 4.0, -4.0, -3.0,
                        6.0, 4.0, 1.0, 1.0, 1.0, 1.0, 1.0))

        shapes.add(BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -4.0,
                -4.0, 1.0, 4.0, -2.0, 8.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(
                BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, -4.0, -2.0, 1.0,
                        -2.0, 2.0, 8.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -4.0, 2.0,
                1.0, 4.0, 4.0, 8.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(
                BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, 2.0, -2.0, 1.0, 4.0,
                        2.0, 8.0, 1.0, 1.0, 1.0, 1.0))
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
        return "Bloomery"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }
}
