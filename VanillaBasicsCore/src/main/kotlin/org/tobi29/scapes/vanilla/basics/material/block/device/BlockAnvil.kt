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
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.math.AABB
import org.tobi29.math.Face
import org.tobi29.math.PointerPane
import org.tobi29.math.vector.Vector3d
import org.tobi29.utils.Pool
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAnvilClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityAnvilServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer

class BlockAnvil(type: VanillaMaterialType) : VanillaBlockContainer<EntityAnvilClient, EntityAnvilServer>(
        type, type.materials.plugin.entityTypes.anvil) {
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
        aabbs.push().set(x + 0.125, y.toDouble(), z.toDouble(), x + 0.875,
                y + 1.0, z + 1.0)
    }

    override fun collision(data: Int,
                           x: Int,
                           y: Int,
                           z: Int): List<AABBElement> {
        val aabbs = ArrayList<AABBElement>()
        aabbs.add(AABBElement(
                AABB(x + 0.125, y.toDouble(), z.toDouble(), x + 0.875,
                        y + 1.0, z + 1.0)))
        return aabbs
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        val tool = item.kind<ItemTypeTool>()
        return (if ("Pickaxe" == tool?.toolType()) 8 else -1).toDouble()
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Stone.ogg"
    }

    override fun breakSound(item: Item?,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Metal.ogg"
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
        val world = terrain.world
        terrain.modify(x, y, z - 1, 1, 1, 2) { terrain ->
            val block = terrain.block(x, y, z - 1)
            if (!terrain.type(block).isSolid(data)) {
                val entity = materials.plugin.entityTypes.flyingBlock.createServer(
                        world).apply {
                    setPos(Vector3d(x + 0.5, y + 0.5, z + 0.5))
                    setType(ItemStackData(this@BlockAnvil, data))
                }
                world.addEntityNew(entity)
                terrain.typeData(x, y, z, terrain.air, 0)
            }
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/device/Anvil.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        val shapes = ArrayList<BlockModelComplex.Shape>()
        shapes.add(BlockModelComplex.ShapeBox(texture, texture, texture,
                texture, texture, texture, -5.0, -6.0, -8.0, 5.0, 6.0,
                -3.0, 1.0, 1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(null, null, texture, texture,
                texture, texture, -4.0, -4.0, -3.0, 4.0, 4.0, 4.0, 1.0,
                1.0, 1.0, 1.0))
        shapes.add(BlockModelComplex.ShapeBox(texture, texture, texture,
                texture, texture, texture, -6.0, -8.0, 4.0, 6.0, 8.0, 8.0,
                1.0, 1.0, 1.0, 1.0))
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
        return "Anvil"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 1
    }

    companion object {
        private val SELECTION = AABB(0.125, 0.0, 0.0, 0.875, 1.0, 1.0)
    }
}
