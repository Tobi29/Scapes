/*
 * Copyright 2012-2016 Tobi29
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

package scapes.plugin.tobi29.vanilla.basics.material.block.device

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.data.ChunkMesh
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.server.EntityContainerServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import scapes.plugin.tobi29.vanilla.basics.entity.server.EntityChestServer
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.block.VanillaBlockContainer

class BlockChest(materials: VanillaMaterial) : VanillaBlockContainer(materials,
        "vanilla.basics.block.Chest") {
    private var textureTop: TerrainTexture? = null
    private var textureFront: TerrainTexture? = null
    private var textureSide: TerrainTexture? = null
    private var models: Array<BlockModel>? = null

    override fun placeEntity(terrain: TerrainServer,
                             x: Int,
                             y: Int,
                             z: Int): EntityContainerServer {
        val entity = EntityChestServer(terrain.world,
                Vector3d(x + 0.5, y + 0.5, z + 0.5))
        terrain.world.addEntityNew(entity)
        return entity
    }

    override fun place(terrain: TerrainServer.TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        if (face == Face.UP || face == Face.DOWN) {
            return false
        }
        terrain.data(x, y, z, face.data.toInt())
        return true
    }

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
        return textureSide
    }

    override fun isTransparent(terrain: Terrain,
                               x: Int,
                               y: Int,
                               z: Int): Boolean {
        return true
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
        return "Chest"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }
}