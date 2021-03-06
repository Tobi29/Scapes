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

import org.tobi29.math.AABB3
import org.tobi29.math.Face
import org.tobi29.math.PointerPane
import org.tobi29.math.vector.Vector3i
import org.tobi29.math.vector.plus
import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.*
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock
import org.tobi29.scapes.vanilla.basics.util.dropItems
import org.tobi29.utils.Pool

class BlockTorch(type: VanillaMaterialType) : VanillaBlock(type) {
    private var textureTop: TerrainTexture? = null
    private var textureSide: TerrainTexture? = null
    private var models: Array<BlockModel>? = null

    override fun addPointerCollision(data: Int,
                                     pointerPanes: Pool<PointerPane>,
                                     x: Int,
                                     y: Int,
                                     z: Int) {
        pointerPanes.push().set(SELECTION[data], Face.UP, x, y, z)
        pointerPanes.push().set(SELECTION[data], Face.DOWN, x, y, z)
        pointerPanes.push().set(SELECTION[data], Face.NORTH, x, y, z)
        pointerPanes.push().set(SELECTION[data], Face.EAST, x, y, z)
        pointerPanes.push().set(SELECTION[data], Face.SOUTH, x, y, z)
        pointerPanes.push().set(SELECTION[data], Face.WEST, x, y, z)
    }

    override fun addCollision(aabbs: Pool<AABBElement>,
                              terrain: Terrain,
                              x: Int,
                              y: Int,
                              z: Int) {
    }

    override fun collision(data: Int,
                           x: Int,
                           y: Int,
                           z: Int): List<AABBElement> {
        return ArrayList()
    }

    override fun place(terrain: TerrainMutableServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        if (!super.place(terrain, x, y, z, face, player)) {
            return false
        }
        val ground = face.opposite.delta + Vector3i(x, y, z)
        if (terrain.block(ground.x, ground.y, ground.z) {
            isSolid(it) && !isTransparent(it)
        }) {
            terrain.data(x, y, z, face.data.toInt())
            return true
        }
        return false
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        return 0.0
    }

    override fun drops(item: Item?,
                       data: Int): List<Item> {
        return listOf(ItemStackData(this, 0.toShort().toInt()))
    }

    override fun footStepSound(data: Int) = null

    override fun breakSound(item: Item?,
                            data: Int) = null

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        return textureSide
    }

    override fun isSolid(data: Int) = false

    override fun isTransparent(data: Int) = true

    override fun lightEmit(data: Int) = 15.toByte()

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
        models?.get(data)?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                1.0,
                1.0,
                1.0, 1.0, lod)
    }

    override fun update(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        val world = terrain.world
        terrain.modify(x - 1, y - 1, z - 1, 3, 3, 3) { terrain ->
            val ground = Face[data].opposite.delta + Vector3i(x, y, z)
            if (terrain.block(ground.x, ground.y, ground.z) {
                !isSolid(it) || isTransparent(it)
            }) {
                world.dropItems(drops(null, data), x, y, z)
                terrain.typeData(x, y, z, terrain.air, 0)
            }
        }
    }

    override fun isTool(item: TypedItem<BlockType>): Boolean {
        return true
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/TorchTop.png")
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/TorchSide.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        val modelsList = ArrayList<BlockModel>(6)
        var shapes: MutableList<BlockModelComplex.Shape> = ArrayList()
        var shape: BlockModelComplex.Shape
        shape = BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1.0, -1.0,
                -8.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0)
        shapes.add(shape)
        modelsList += BlockModelComplex(registry, shapes, 0.0625)
        shape = BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1.0, -1.0,
                -8.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0)
        shapes.add(shape)
        modelsList += BlockModelComplex(registry, shapes, 0.0625)
        shapes = ArrayList<BlockModelComplex.Shape>()
        shape = BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1.0, -1.0,
                -8.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0)
        shape.translate(0.0, 6.0, 0.0)
        shape.rotateX(30.0)
        shapes.add(shape)
        modelsList += BlockModelComplex(registry, shapes, 0.0625)
        shapes = ArrayList<BlockModelComplex.Shape>()
        shape = BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1.0, -1.0,
                -8.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0)
        shape.translate(-6.0, 0.0, 0.0)
        shape.rotateY(-30.0)
        shapes.add(shape)
        modelsList += BlockModelComplex(registry, shapes, 0.0625)
        shapes = ArrayList<BlockModelComplex.Shape>()
        shape = BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1.0, -1.0,
                -8.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0)
        shape.translate(0.0, -6.0, 0.0)
        shape.rotateX(-30.0)
        shapes.add(shape)
        modelsList += BlockModelComplex(registry, shapes, 0.0625)
        shapes = ArrayList<BlockModelComplex.Shape>()
        shape = BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1.0, -1.0,
                -8.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0)
        shape.translate(6.0, 0.0, 0.0)
        shape.rotateY(30.0)
        shapes.add(shape)
        modelsList += BlockModelComplex(registry, shapes, 0.0625)
        models = modelsList.toArray(arrayOfNulls<BlockModel>(modelsList.size))
    }

    override fun render(item: TypedItem<BlockType>,
                        gl: GL,
                        shader: Shader) {
        models?.get(item.data)?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<BlockType>,
                                 gl: GL,
                                 shader: Shader) {
        models?.get(item.data)?.renderInventory(gl, shader)
    }

    override fun light(item: TypedItem<BlockType>): Double {
        return 0.7
    }

    override fun name(item: TypedItem<BlockType>): String {
        return "Torch"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 32
    }

    companion object {
        private val SELECTION = arrayOf(
                AABB3(0.4375, 0.4375, 0.0, 0.5625, 0.5625, 0.625),
                AABB3(0.4375, 0.4375, 0.0, 0.5625, 0.5625, 0.625),
                AABB3(0.4375, 0.4375, 0.0, 0.5625, 1.0, 0.625),
                AABB3(0.0, 0.4375, 0.0, 0.5625, 0.5625, 0.625),
                AABB3(0.4375, 0.0, 0.0, 0.5625, 0.5625, 0.625),
                AABB3(0.4375, 0.4375, 0.0, 1.0, 0.5625, 0.625))
    }
}
