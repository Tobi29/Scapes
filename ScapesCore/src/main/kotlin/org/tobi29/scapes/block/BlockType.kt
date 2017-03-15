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

package org.tobi29.scapes.block

import org.tobi29.scapes.chunk.ChunkMesh
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.PointerPane
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.terrain.VoxelType
import java.util.*

abstract class BlockType(type: MaterialType) : Material(type), VoxelType {
    fun block(data: Int): Long {
        return (id.toLong() shl 32) or data.toLong()
    }

    open fun addPointerCollision(data: Int,
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

    open fun click(terrain: TerrainServer,
                   x: Int,
                   y: Int,
                   z: Int,
                   face: Face,
                   player: MobPlayerServer): Boolean {
        return false
    }

    override fun click(entity: MobPlayerServer,
                       item: ItemStack,
                       terrain: TerrainServer.TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face): Double {
        val place = face.delta + Vector3i(x, y, z)
        if (terrain.type(place.x, place.y,
                place.z).isReplaceable(terrain, place.x,
                place.y,
                place.z)) {
            val aabbs = collision(item.data(), place.x, place.y,
                    place.z)
            var flag = true
            val coll = entity.getAABB()
            for (element in aabbs) {
                if (coll.overlay(
                        element.aabb) && element.collision.isSolid) {
                    flag = false
                }
            }
            if (flag) {
                entity.inventories().modify("Container") {
                    terrain.data(place.x, place.y, place.z,
                            item.data())
                    if (place(terrain, place.x, place.y,
                            place.z, face, entity)) {
                        terrain.type(place.x, place.y,
                                place.z, this)
                        item.setAmount(item.amount() - 1)
                    }
                }
            }
        }
        return 0.0
    }

    override fun click(entity: MobPlayerServer,
                       item: ItemStack,
                       hit: MobServer): Double {
        return 0.0
    }

    override fun toolType(item: ItemStack): String {
        return "Block"
    }

    open fun addCollision(aabbs: Pool<AABBElement>,
                          terrain: Terrain,
                          x: Int,
                          y: Int,
                          z: Int) {
        aabbs.push().set(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0,
                y + 1.0, z + 1.0)
    }

    open fun collision(data: Int,
                       x: Int,
                       y: Int,
                       z: Int): List<AABBElement> {
        val aabbs = ArrayList<AABBElement>()
        aabbs.add(AABBElement(
                AABB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0,
                        z + 1.0)))
        return aabbs
    }

    open fun isReplaceable(terrain: Terrain,
                           x: Int,
                           y: Int,
                           z: Int): Boolean {
        return false
    }

    open fun place(terrain: TerrainServer.TerrainMutable,
                   x: Int,
                   y: Int,
                   z: Int,
                   face: Face,
                   player: MobPlayerServer): Boolean {
        return true
    }

    open fun punch(terrain: TerrainServer.TerrainMutable,
                   x: Int,
                   y: Int,
                   z: Int,
                   data: Int,
                   face: Face,
                   player: MobPlayerServer,
                   item: ItemStack,
                   br: Double,
                   strength: Double) {
    }

    open fun destroy(terrain: TerrainServer.TerrainMutable,
                     x: Int,
                     y: Int,
                     z: Int,
                     data: Int,
                     face: Face,
                     player: MobPlayerServer,
                     item: ItemStack): Boolean {
        return true
    }

    abstract fun resistance(item: ItemStack,
                            data: Int): Double

    open fun drops(item: ItemStack,
                   data: Int): List<ItemStack> {
        return listOf(ItemStack(this, data))
    }

    abstract fun footStepSound(data: Int): String?

    abstract fun breakSound(item: ItemStack,
                            data: Int): String?

    open fun particleFriction(face: Face,
                              terrain: TerrainClient,
                              x: Int,
                              y: Int,
                              z: Int,
                              data: Int): Float {
        return 0.2f
    }

    open fun particleColorR(face: Face,
                            terrain: TerrainClient,
                            x: Int,
                            y: Int,
                            z: Int,
                            data: Int): Float {
        return 1.0f
    }

    open fun particleColorG(face: Face,
                            terrain: TerrainClient,
                            x: Int,
                            y: Int,
                            z: Int,
                            data: Int): Float {
        return 1.0f
    }

    open fun particleColorB(face: Face,
                            terrain: TerrainClient,
                            x: Int,
                            y: Int,
                            z: Int,
                            data: Int): Float {
        return 1.0f
    }

    abstract fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture?

    open val isLiquid: Boolean
        get() = false

    open fun connectStage(terrain: TerrainClient,
                          x: Int,
                          y: Int,
                          z: Int): Int {
        return 4
    }

    abstract fun addToChunkMesh(mesh: ChunkMesh,
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
                                lod: Boolean)

    open fun update(terrain: TerrainServer.TerrainMutable,
                    x: Int,
                    y: Int,
                    z: Int,
                    data: Int) {
    }

    companion object {
        val STANDARD_COLLISION = Collision()
        private val SELECTION = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
    }
}
