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

package org.tobi29.scapes.vanilla.basics.material.block.structural

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.math.Face
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock

class BlockGlass(type: VanillaMaterialType) : VanillaBlock(type) {
    private var textureFrame: TerrainTexture? = null
    private var textureTransparent: TerrainTexture? = null
    private var modelFrame: BlockModel? = null
    private var modelTransparent: BlockModel? = null

    override fun resistance(item: Item?,
                            data: Int): Double {
        return 1.0
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
        return textureFrame
    }

    override fun isTransparent(data: Int) = true

    override fun lightTrough(data: Int) = (-1).toByte()

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
        if (lod) {
            modelTransparent?.addToChunkMesh(meshAlpha, terrain, x, y, z, xx,
                    yy, zz,
                    1.0, 1.0, 1.0, 1.0, lod)
        }
        modelFrame?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0,
                1.0,
                1.0, 1.0, lod)
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureFrame = registry.registerTexture(
                "VanillaBasics:image/terrain/Glass.png")
        textureTransparent = registry.registerTexture(
                "VanillaBasics:image/terrain/GlassTransparent.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        modelFrame = BlockModelSimpleBlock(this, registry, textureFrame,
                textureFrame, textureFrame, textureFrame, textureFrame,
                textureFrame, 1.0, 1.0, 1.0, 1.0)
        modelTransparent = BlockModelSimpleBlock(this, registry,
                textureTransparent,
                textureTransparent, textureTransparent,
                textureTransparent, textureTransparent,
                textureTransparent, 1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: TypedItem<BlockType>,
                        gl: GL,
                        shader: Shader) {
        modelFrame?.render(gl, shader)
        modelTransparent?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<BlockType>,
                                 gl: GL,
                                 shader: Shader) {
        modelFrame?.renderInventory(gl, shader)
        modelTransparent?.renderInventory(gl, shader)
    }

    override fun name(item: TypedItem<BlockType>): String {
        return "Glass"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 16
    }
}
