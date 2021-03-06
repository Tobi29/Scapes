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

import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.math.Face
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBellowsClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityBellowsServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockEntity

class BlockBellows(type: VanillaMaterialType) : VanillaBlockEntity<EntityBellowsClient, EntityBellowsServer>(
        type, type.materials.plugin.entityTypes.bellows) {
    private var textureFrame: TerrainTexture? = null
    private var textureInside: TerrainTexture? = null
    private var model: BlockModel? = null

    override fun place(terrain: TerrainMutableServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        terrain.data(x, y, z, face.data.toInt())
        return super.place(terrain, x, y, z, face, player)
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        return (if ("Axe" == item.kind<ItemTypeTool>()?.toolType()) 4 else -1).toDouble()
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
        return textureFrame
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
    }

    override fun causesTileUpdate(): Boolean {
        return true
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureFrame = registry.registerTexture(
                "VanillaBasics:image/terrain/tree/oak/Planks.png")
        textureInside = registry.registerTexture(
                "VanillaBasics:image/terrain/tree/birch/Planks.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        val shapes = ArrayList<BlockModelComplex.Shape>()
        shapes.add(BlockModelComplex.ShapeBox(textureFrame, textureFrame,
                textureFrame, textureFrame, textureFrame, textureFrame,
                -7.0, -7.0, -8.0, 7.0, 7.0, -6.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(textureInside, textureInside,
                textureInside, textureInside, textureInside, textureInside,
                -6.0, -6.0, -6.0, 6.0, 6.0, -1.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(textureFrame, textureFrame,
                textureFrame, textureFrame, textureFrame, textureFrame,
                -7.0, -7.0, -1.0, 7.0, 7.0, 1.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(textureInside, textureInside,
                textureInside, textureInside, textureInside, textureInside,
                -6.0, -6.0, 1.0, 6.0, 6.0, 6.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(textureFrame, textureFrame,
                textureFrame, textureFrame, textureFrame, textureFrame,
                -7.0, -7.0, 6.0, 7.0, 7.0, 8.0, 1.0, 1.0, 1.0, 1.0))
        model = BlockModelComplex(registry, shapes, 0.0625)
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
        return "Bellows"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 1
    }
}
