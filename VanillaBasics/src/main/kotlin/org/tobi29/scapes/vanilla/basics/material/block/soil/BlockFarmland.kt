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

package org.tobi29.scapes.vanilla.basics.material.block.soil

import org.tobi29.scapes.block.*
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
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.entity.client.EntityFarmlandClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityFarmlandServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockEntity

class BlockFarmland(type: VanillaMaterialType) : VanillaBlockEntity<EntityFarmlandClient, EntityFarmlandServer>(
        type, type.materials.plugin.entityTypes.farmland) {
    private var textureTop: TerrainTexture? = null
    private var textureSide: TerrainTexture? = null
    private var model: BlockModel? = null

    override fun resistance(item: Item?,
                            data: Int): Double {
        return (if ("Shovel" == item.kind<ItemTypeTool>()?.toolType()) 2 else 20).toDouble()
    }

    override fun drops(item: Item?,
                       data: Int): List<Item> {
        return listOf(ItemStackData(materials.dirt, data))
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Dirt.ogg"
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
        return textureSide
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

    override fun causesTileUpdate(): Boolean {
        return true
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Farmland.png")
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Dirt.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        model = BlockModelSimpleBlock(this, registry, textureTop,
                textureSide, textureSide, textureSide, textureSide, textureSide,
                1.0, 1.0, 1.0, 1.0)
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
        return "Dirt"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 16
    }
}
