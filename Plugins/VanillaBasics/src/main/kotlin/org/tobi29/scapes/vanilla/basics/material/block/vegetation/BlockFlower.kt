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

package org.tobi29.scapes.vanilla.basics.material.block.vegetation

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.chunk.data.ChunkMesh
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.PointerPane
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock
import java.util.*

class BlockFlower(materials: VanillaMaterial) : VanillaBlock(materials,
        "vanilla.basics.block.Flower") {
    private var textures: Array<TerrainTexture>? = null
    private var models: Array<BlockModel>? = null
    private var modelsItem: Array<ItemModel>? = null

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
    }

    override fun collision(data: Int,
                           x: Int,
                           y: Int,
                           z: Int): List<AABBElement> {
        return ArrayList()
    }

    override fun isReplaceable(terrain: Terrain,
                               x: Int,
                               y: Int,
                               z: Int): Boolean {
        return true
    }

    override fun place(terrain: TerrainServer.TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        return terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1)
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return 0.0
    }

    override fun footStepSound(data: Int) = null

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Foliage.ogg"
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        return textures?.get(data)
    }

    override fun isSolid(terrain: Terrain,
                         x: Int,
                         y: Int,
                         z: Int): Boolean {
        return false
    }

    override fun isTransparent(terrain: Terrain,
                               x: Int,
                               y: Int,
                               z: Int): Boolean {
        return true
    }

    override fun lightTrough(terrain: Terrain,
                             x: Int,
                             y: Int,
                             z: Int): Byte {
        return -1
    }

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
                1.0, 1.0, 1.0, 1.0, lod)
    }

    override fun update(terrain: TerrainServer.TerrainMutable,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        if (!terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1)) {
            terrain.world.dropItems(drops(ItemStack(materials.air, 0), data), x,
                    y, z)
            terrain.typeData(x, y, z, terrain.air, 0)
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = arrayOf(registry.registerTexture(
                "VanillaBasics:image/terrain/RoseRed.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RoseOrange.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RoseYellow.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RoseGreen.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RoseBlue.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RosePurple.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RoseBlack.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RoseDarkGray.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RoseLightGray.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/RoseWhite.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerRed.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerOrange.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerYellow.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerGreen.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerBlue.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerPurple.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerBlack.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerDarkGray.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerLightGray.png"),
                registry.registerTexture(
                        "VanillaBasics:image/terrain/FlowerWhite.png"))
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = it.asSequence().map {
                val shapes = ArrayList<BlockModelComplex.Shape>()
                val shape = BlockModelComplex.ShapeBillboard(it, -8.0, -8.0,
                        -8.0, 8.0, 8.0, 8.0, 1.0, 1.0, 1.0, 1.0)
                shape.rotateZ(45.0)
                shapes.add(shape)
                BlockModelComplex(registry, shapes, 0.0625)
            }.toArray()
            modelsItem = it.asSequence().map {
                ItemModelSimple(it, 1.0, 1.0, 1.0, 1.0)
            }.toArray()
        }
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        modelsItem?.get(item.data())?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        modelsItem?.get(item.data())?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return color(
                item.data() % 10) + if (item.data() / 10 == 0) " Rose" else " Flower"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 64
    }

    companion object {
        private val SELECTION = AABB(0.15, 0.15, 0.0, 0.85, 0.85, 0.95)

        private fun color(color: Int): String {
            when (color) {
                0 -> return "Red"
                1 -> return "Orange"
                2 -> return "Yellows"
                3 -> return "Green"
                4 -> return "Blue"
                5 -> return "Purple"
                6 -> return "Black"
                7 -> return "Dark Gray"
                8 -> return "Light Gray"
                9 -> return "White"
                else -> return "Unknown"
            }
        }
    }
}
