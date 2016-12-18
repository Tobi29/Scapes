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

package scapes.plugin.tobi29.vanilla.basics.material.block

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelLiquid
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
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.update.UpdateLavaFlow
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class BlockLava(materials: VanillaMaterial) : VanillaBlock(materials,
        "vanilla.basics.block.Lava") {
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
                CollisionLava.INSTANCE)
    }

    override fun collision(data: Int,
                           x: Int,
                           y: Int,
                           z: Int): List<AABBElement> {
        val aabbs = ArrayList<AABBElement>()
        aabbs.add(AABBElement(AABB(x.toDouble(), y.toDouble(), z.toDouble(),
                (x + 1).toDouble(), (y + 1).toDouble(), (z + 1).toDouble()),
                CollisionLava.INSTANCE))
        return aabbs
    }

    override fun isReplaceable(terrain: Terrain,
                               x: Int,
                               y: Int,
                               z: Int): Boolean {
        return true
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return -1.0
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Water.ogg"
    }

    override fun breakSound(item: ItemStack,
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

    override fun isSolid(terrain: Terrain,
                         x: Int,
                         y: Int,
                         z: Int): Boolean {
        return false
    }

    override fun lightEmit(terrain: Terrain,
                           x: Int,
                           y: Int,
                           z: Int,
                           data: Int): Byte {
        return 15
    }

    override fun lightTrough(terrain: Terrain,
                             x: Int,
                             y: Int,
                             z: Int): Byte {
        return -4
    }

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
        model?.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0, 1.0, 1.0,
                1.0, lod)
    }

    override fun update(terrain: TerrainServer.TerrainMutable,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
        if (!terrain.hasDelayedUpdate(x, y, z, UpdateLavaFlow::class.java)) {
            val random = ThreadLocalRandom.current()
            terrain.addDelayedUpdate(UpdateLavaFlow().set(x, y, z,
                    random.nextDouble() * 0.3 + 0.2))
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureStill = registry.registerTexture(
                "VanillaBasics:image/terrain/LavaStill.png", true)
        textureFlow = registry.registerTexture(
                "VanillaBasics:image/terrain/LavaFlow.png", true)
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        model = BlockModelLiquid(this, registry, textureStill, textureStill,
                textureFlow, textureFlow, textureFlow, textureFlow, 1.0, 1.0,
                1.0, 1.0, 0.0, 1.0)
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        model?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        model?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return "Lava"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }
}
