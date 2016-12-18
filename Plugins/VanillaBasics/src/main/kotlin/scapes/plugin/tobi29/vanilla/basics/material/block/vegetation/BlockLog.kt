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

package scapes.plugin.tobi29.vanilla.basics.material.block.vegetation

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
import org.tobi29.scapes.chunk.data.ChunkMesh
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.server.MobPlayerServer
import scapes.plugin.tobi29.vanilla.basics.material.TreeType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.block.VanillaBlock
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemFuel

class BlockLog(materials: VanillaMaterial,
               private val treeRegistry: GameRegistry.Registry<TreeType>) : VanillaBlock(
        materials, "vanilla.basics.block.Log"), ItemFuel {
    private var textures: Array<Pair<TerrainTexture, TerrainTexture>?>? = null
    private var models: Array<BlockModel?>? = null

    override fun destroy(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: ItemStack): Boolean {
        if ("Axe" == item.material().toolType(item)) {
            destroy(terrain, Vector3i(x, y, z), data, 512, player, z)
        }
        return true
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return (if ("Axe" == item.material().toolType(item))
            10
        else if ("Saw" == item.material().toolType(item)) 2 else -1).toDouble()
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        if ("Saw" == item.material().toolType(item)) {
            return listOf(ItemStack(materials.wood, data, 2))
        }
        return emptyList()
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Wood.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return if ("Axe" == item.material().toolType(item))
            "VanillaBasics:sound/blocks/Axe.ogg"
        else
            "VanillaBasics:sound/blocks/Saw.ogg"
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        if (face == Face.UP || face == Face.DOWN) {
            return textures?.get(data)?.first
        }
        return textures?.get(data)?.second
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
                1.0, 1.0, 1.0, lod)
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = treeRegistry.values().asSequence().map {
            if (it == null) {
                return@map null
            }
            Pair(registry.registerTexture(it.texture() + "/LogTop.png"),
                    registry.registerTexture(it.texture() + "/LogSide.png"))
        }.toArray()
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = it.asSequence().map {
                if (it == null) {
                    return@map null
                }
                BlockModelSimpleBlock(this, registry, it.first, it.first,
                        it.second, it.second, it.second, it.second,
                        1.0, 1.0, 1.0, 1.0)
            }.toArray()
        }
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        models?.get(item.data())?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        models?.get(item.data())?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return treeRegistry[item.data()].name()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }

    private fun destroy(terrain: TerrainServer.TerrainMutable,
                        pos: Vector3i,
                        data: Int,
                        length: Int,
                        player: MobPlayerServer,
                        minZ: Int) {
        val block = terrain.block(pos.x, pos.y, pos.z)
        val type = terrain.type(block)
        val d = terrain.data(block)
        if (type === materials.leaves && d == data) {
            type.destroy(terrain, pos.x, pos.y, pos.z, d,
                    Face.NONE, player, ItemStack(materials.air, 0))
        }
        if (type !== this || d != data) {
            return
        }
        terrain.world.dropItem(ItemStack(this, data), pos.x, pos.y,
                pos.z)
        terrain.typeData(pos.x, pos.y, pos.z, materials.air,
                0.toShort().toInt())
        if (length > 0) {
            val i = length - 1
            if (pos.z > minZ) {
                destroy(terrain, pos.plus(Vector3i(-1, -1, -1)), data,
                        i, player, minZ)
                destroy(terrain, pos.plus(Vector3i(0, -1, -1)), data,
                        i, player, minZ)
                destroy(terrain, pos.plus(Vector3i(1, -1, -1)), data,
                        i, player, minZ)
                destroy(terrain, pos.plus(Vector3i(-1, 0, -1)), data,
                        i, player, minZ)
                destroy(terrain, pos.plus(Vector3i(0, 0, -1)), data, i,
                        player, minZ)
                destroy(terrain, pos.plus(Vector3i(1, 0, -1)), data, i,
                        player, minZ)
                destroy(terrain, pos.plus(Vector3i(-1, 1, -1)), data,
                        i, player, minZ)
                destroy(terrain, pos.plus(Vector3i(0, 1, -1)), data, i,
                        player, minZ)
                destroy(terrain, pos.plus(Vector3i(1, 1, -1)), data, i,
                        player, minZ)
            }
            destroy(terrain, pos.plus(Vector3i(-1, -1, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(0, -1, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(1, -1, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(-1, 0, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(0, 0, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(1, 0, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(-1, 1, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(0, 1, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(1, 1, 0)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(-1, -1, 1)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(0, -1, 1)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(1, -1, 1)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(-1, 0, 1)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(0, 0, 1)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(1, 0, 1)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(-1, 1, 1)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(0, 1, 1)), data, i,
                    player, minZ)
            destroy(terrain, pos.plus(Vector3i(1, 1, 1)), data, i,
                    player, minZ)
        }
    }

    override fun fuelTemperature(item: ItemStack): Float {
        return 0.2f
    }

    override fun fuelTime(item: ItemStack): Float {
        return 100.0f
    }

    override fun fuelTier(item: ItemStack): Int {
        return 10
    }
}
