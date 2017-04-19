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
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.entity.client.EntityFurnaceClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityFurnaceServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer

class BlockFurnace(type: VanillaMaterialType) : VanillaBlockContainer<EntityFurnaceClient, EntityFurnaceServer>(
        type, type.materials.plugin.entityTypes.furnace) {
    private var textureTop: TerrainTexture? = null
    private var textureFront1: TerrainTexture? = null
    private var textureFront2: TerrainTexture? = null
    private var textureSide: TerrainTexture? = null
    private var models: Array<BlockModel>? = null

    override fun place(terrain: TerrainServer.TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        terrain.data(x, y, z, face.data.toInt())
        return super.place(terrain, x, y, z, face, player)
    }

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
        models?.get(data)?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                1.0, 1.0, 1.0, 1.0, lod)
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/device/FurnaceTop.png")
        textureFront1 = registry.registerTexture(
                "VanillaBasics:image/terrain/device/FurnaceFront1.png")
        textureFront2 = registry.registerTexture(
                "VanillaBasics:image/terrain/device/FurnaceFront2.png")
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/device/FurnaceSide.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        val modelList = ArrayList<BlockModel>(6)
        modelList += BlockModelSimpleBlock(this, registry, textureFront2,
                textureTop, textureSide, textureSide, textureSide, textureSide,
                1.0, 1.0, 1.0, 1.0)
        modelList += BlockModelSimpleBlock(this, registry, textureTop,
                textureFront2, textureSide, textureSide, textureSide,
                textureSide, 1.0, 1.0, 1.0, 1.0)
        modelList += BlockModelSimpleBlock(this, registry, textureTop,
                textureTop, textureFront1, textureSide, textureSide,
                textureSide, 1.0, 1.0, 1.0, 1.0)
        modelList += BlockModelSimpleBlock(this, registry, textureTop,
                textureTop, textureSide, textureFront1, textureSide,
                textureSide, 1.0, 1.0, 1.0, 1.0)
        modelList += BlockModelSimpleBlock(this, registry, textureTop,
                textureTop, textureSide, textureSide, textureFront1,
                textureSide, 1.0, 1.0, 1.0, 1.0)
        modelList += BlockModelSimpleBlock(this, registry, textureTop,
                textureTop, textureSide, textureSide, textureSide,
                textureFront1, 1.0, 1.0, 1.0, 1.0)
        models = modelList.toArray(arrayOfNulls<BlockModel>(modelList.size))
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        models?.get(4)?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        models?.get(4)?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return "Furnace"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }
}
