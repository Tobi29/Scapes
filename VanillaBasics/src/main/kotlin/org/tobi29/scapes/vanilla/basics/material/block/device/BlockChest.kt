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
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
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
import org.tobi29.scapes.vanilla.basics.entity.client.EntityChestClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityChestServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer

class BlockChest(type: VanillaMaterialType) : VanillaBlockContainer<EntityChestClient, EntityChestServer>(
        type, type.materials.plugin.entityTypes.chest) {
    private var textureTop: TerrainTexture? = null
    private var textureFront: TerrainTexture? = null
    private var textureSide: TerrainTexture? = null
    private var models: Array<BlockModel>? = null

    override fun place(terrain: TerrainMutableServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        if (face == Face.UP || face == Face.DOWN) {
            return false
        }
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
        return "VanillaBasics:sound/blocks/Axe.ogg"
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
        models?.get(data)?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                1.0, 1.0, 1.0, 1.0, lod)
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ChestTop.png")
        textureFront = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ChestFront.png")
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ChestSide.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        models = arrayOf(
                BlockModelSimpleBlock(this, registry, textureFront,
                        textureTop, textureSide, textureSide, textureSide,
                        textureSide,
                        1.0, 1.0, 1.0, 1.0),
                BlockModelSimpleBlock(this, registry, textureTop,
                        textureFront, textureSide, textureSide, textureSide,
                        textureSide, 1.0, 1.0, 1.0, 1.0),
                BlockModelSimpleBlock(this, registry, textureTop,
                        textureTop, textureFront, textureSide, textureSide,
                        textureSide,
                        1.0, 1.0, 1.0, 1.0),
                BlockModelSimpleBlock(this, registry, textureTop,
                        textureTop, textureSide, textureFront, textureSide,
                        textureSide,
                        1.0, 1.0, 1.0, 1.0),
                BlockModelSimpleBlock(this, registry, textureTop,
                        textureTop, textureSide, textureSide, textureFront,
                        textureSide,
                        1.0, 1.0, 1.0, 1.0),
                BlockModelSimpleBlock(this, registry, textureTop,
                        textureTop, textureSide, textureSide, textureSide,
                        textureFront,
                        1.0, 1.0, 1.0, 1.0))
    }

    override fun render(item: TypedItem<BlockType>,
                        gl: GL,
                        shader: Shader) {
        models?.get(4)?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<BlockType>,
                                 gl: GL,
                                 shader: Shader) {
        models?.get(4)?.renderInventory(gl, shader)
    }

    override fun name(item: TypedItem<BlockType>): String {
        return "Chest"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 1
    }
}
