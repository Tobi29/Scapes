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
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.math.Face
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.entity.client.EntityForgeClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityForgeServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer

class BlockForge(type: VanillaMaterialType) : VanillaBlockContainer<EntityForgeClient, EntityForgeServer>(
        type, type.materials.plugin.entityTypes.forge) {
    private var textureOn: TerrainTexture? = null
    private var textureOff: TerrainTexture? = null
    private var modelOn: BlockModel? = null
    private var modelOff: BlockModel? = null

    override fun resistance(item: Item?,
                            data: Int): Double {
        return (if ("Shovel" == item.kind<ItemTypeTool>()?.toolType()) 4 else 12).toDouble()
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
        return if (data > 0) textureOn else textureOff
    }

    override fun lightEmit(data: Int) = if (data > 0) 15.toByte() else 0

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
        if (data > 0) {
            modelOn?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0,
                    1.0, 1.0, 1.0, lod)
        } else {
            modelOff?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0,
                    1.0, 1.0, 1.0, lod)
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureOn = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ForgeOn.png")
        textureOff = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ForgeOff.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        modelOn = BlockModelSimpleBlock(this, registry, textureOn, textureOn,
                textureOn, textureOn, textureOn, textureOn, 1.0, 1.0,
                1.0, 1.0)
        modelOff = BlockModelSimpleBlock(this, registry, textureOff,
                textureOff, textureOff, textureOff, textureOff, textureOff,
                1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: TypedItem<BlockType>,
                        gl: GL,
                        shader: Shader) {
        modelOff?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<BlockType>,
                                 gl: GL,
                                 shader: Shader) {
        modelOff?.renderInventory(gl, shader)
    }

    override fun name(item: TypedItem<BlockType>): String {
        return "Forge"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 1
    }
}
