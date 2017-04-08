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
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.vanilla.basics.entity.client.EntityResearchTableClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityResearchTableServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer

class BlockResearchTable(type: VanillaMaterialType) : VanillaBlockContainer<EntityResearchTableClient, EntityResearchTableServer>(
        type, type.materials.plugin.entityTypes.researchTable) {
    private var textureTop: TerrainTexture? = null
    private var textureSide1: TerrainTexture? = null
    private var textureSide2: TerrainTexture? = null
    private var textureBottom: TerrainTexture? = null
    private var model: BlockModel? = null

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return (if ("Axe" == item.material().toolType(
                item)) 4 else -1).toDouble()
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Wood.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Axe.ogg"
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        return textureSide1
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
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ResearchTableTop.png")
        textureSide1 = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ResearchTableSide1.png")
        textureSide2 = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ResearchTableSide2.png")
        textureBottom = registry.registerTexture(
                "VanillaBasics:image/terrain/tree/spruce/Planks.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        model = BlockModelSimpleBlock(this, registry, textureTop,
                textureBottom, textureSide1, textureSide1, textureSide2,
                textureSide2, 1.0, 1.0, 1.0, 1.0)
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
        return "Research Table"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }
}
