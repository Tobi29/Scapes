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

package org.tobi29.scapes.vanilla.basics.material.block

import org.tobi29.math.AABB3
import org.tobi29.math.Face
import org.tobi29.math.PointerPane
import org.tobi29.math.threadLocalRandom
import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelLiquid
import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.*
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.update.UpdateWaterFlow
import org.tobi29.utils.Pool

class BlockWater(type: VanillaMaterialType) : VanillaBlock(type) {
    private var textureStill: TerrainTexture? = null
    private var textureFlow: TerrainTexture? = null
    private var model: BlockModel? = null

    override fun addPointerCollision(data: Int,
                                     pointerPanes: Pool<PointerPane>,
                                     x: Int,
                                     y: Int,
                                     z: Int) {
    }

    override fun addCollision(aabbs: Pool<AABBElement>,
                              terrain: Terrain,
                              x: Int,
                              y: Int,
                              z: Int) {
        aabbs.push().set(x.toDouble(), y.toDouble(), z.toDouble(),
                (x + 1).toDouble(), (y + 1).toDouble(), (z + 1).toDouble(),
                CollisionWater.INSTANCE)
    }

    override fun collision(data: Int,
                           x: Int,
                           y: Int,
                           z: Int): List<AABBElement> {
        val aabbs = ArrayList<AABBElement>()
        aabbs.add(AABBElement(AABB3(x.toDouble(), y.toDouble(), z.toDouble(),
                (x + 1).toDouble(), (y + 1).toDouble(), (z + 1).toDouble()),
                CollisionWater.INSTANCE))
        return aabbs
    }

    override fun isReplaceable(terrain: Terrain,
                               x: Int,
                               y: Int,
                               z: Int): Boolean {
        return true
    }

    override fun resistance(item: Item?,
                            data: Int): Double {
        return -1.0
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Water.ogg"
    }

    override fun breakSound(item: Item?,
                            data: Int) = null

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        return null
    }

    override val isLiquid: Boolean
        get() = true

    override fun isSolid(data: Int) = false

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
        model?.addToChunkMesh(meshAlpha, terrain, x, y, z, xx, yy, zz, 1.0,
                1.0, 1.0, 1.0, lod)
    }

    override fun update(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        val world = terrain.world
        terrain.modify(this, x, y, z) { terrain ->
            if (!terrain.hasDelayedUpdate(x, y, z,
                    UpdateWaterFlow::class.java)) {
                val random = threadLocalRandom()
                terrain.addDelayedUpdate(
                        UpdateWaterFlow(world.registry).set(x, y, z,
                                random.nextDouble() * 0.2 + 0.1))
            }
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureStill = registry.registerTexture(
                "VanillaBasics:image/terrain/WaterStill.png", true,
                ShaderAnimation.WATER)
        textureFlow = registry.registerTexture(
                "VanillaBasics:image/terrain/WaterFlow.png", true,
                ShaderAnimation.WATER)
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        model = BlockModelLiquid(this, registry, textureStill, textureStill,
                textureFlow, textureFlow, textureFlow, textureFlow, 1.0, 1.0,
                1.0, 0.7, 0.0, 1.0, 1, 9)
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
        return "Water"
    }

    override fun maxStackSize(item: TypedItem<BlockType>): Int {
        return 1
    }
}
