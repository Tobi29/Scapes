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

import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.BlockModel
import org.tobi29.scapes.block.models.BlockModelComplex
import org.tobi29.scapes.block.models.BlockModelSimpleBlock
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
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.mix
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3i
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.world.ClimateInfoLayer
import org.tobi29.scapes.vanilla.basics.world.EnvironmentClimate
import org.tobi29.scapes.vanilla.basics.material.TreeType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.block.CollisionLeaves
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock
import java.util.*

class BlockLeaves(materials: VanillaMaterial,
                  private val treeRegistry: GameRegistry.Registry<TreeType>) : VanillaBlock(
        materials, "vanilla.basics.block.Leaves") {
    private var textures: Array<Triple<TreeType, TerrainTexture, TerrainTexture>?>? = null
    private var modelsFancy: Array<BlockModel?>? = null
    private var modelsFast: Array<BlockModel?>? = null
    private var modelsColored: Array<BlockModel?>? = null

    override fun addCollision(aabbs: Pool<AABBElement>,
                              terrain: Terrain,
                              x: Int,
                              y: Int,
                              z: Int) {
        aabbs.push().set(x.toDouble(), y.toDouble(), z.toDouble(),
                (x + 1).toDouble(), (y + 1).toDouble(), (z + 1).toDouble(),
                CollisionLeaves.INSTANCE)
    }

    override fun collision(data: Int,
                           x: Int,
                           y: Int,
                           z: Int): List<AABBElement> {
        val aabbs = ArrayList<AABBElement>()
        aabbs.add(AABBElement(AABB(x.toDouble(), y.toDouble(), z.toDouble(),
                (x + 1).toDouble(), (y + 1).toDouble(), (z + 1).toDouble()),
                CollisionLeaves.INSTANCE))
        return aabbs
    }

    override fun destroy(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: ItemStack): Boolean {
        destroy(terrain, Vector3i(x, y, z), data, 256)
        return true
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return 0.1
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        val type = treeRegistry[data]
        val random = Random()
        val drops = ArrayList<ItemStack>()
        if (random.nextInt(type.dropChance()) == 0) {
            if (random.nextInt(3) == 0) {
                drops.add(ItemStack(materials.sapling, data))
            } else {
                drops.add(ItemStack(materials.stick, 0.toShort().toInt()))
            }
        }
        return drops
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Grass.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Foliage.ogg"
    }

    override fun particleFriction(face: Face,
                                  terrain: TerrainClient,
                                  x: Int,
                                  y: Int,
                                  z: Int,
                                  data: Int): Float {
        return 4.0f
    }

    override fun particleColorR(face: Face,
                                terrain: TerrainClient,
                                x: Int,
                                y: Int,
                                z: Int,
                                data: Int): Float {
        val environment = terrain.world.environment
        if (environment is EnvironmentClimate) {
            val climateGenerator = environment.climate()
            val type = treeRegistry[data]
            val temperature = climateGenerator.temperature(x, y, z)
            val mix = clamp(temperature / 30.0, 0.0, 1.0)
            val colorCold = type.colorCold()
            val colorWarm = type.colorWarm()
            var r = mix(colorCold.floatX().toDouble(),
                    colorWarm.floatX().toDouble(), mix)
            if (!type.isEvergreen) {
                val autumn = climateGenerator.autumnLeaves(y.toDouble())
                val colorAutumn = type.colorAutumn()
                r = mix(r, colorAutumn.floatX().toDouble(), autumn)
            }
            return r.toFloat()
        }
        return 1.0f
    }

    override fun particleColorG(face: Face,
                                terrain: TerrainClient,
                                x: Int,
                                y: Int,
                                z: Int,
                                data: Int): Float {
        val environment = terrain.world.environment
        if (environment is EnvironmentClimate) {
            val climateGenerator = environment.climate()
            val type = treeRegistry[data]
            val temperature = climateGenerator.temperature(x, y, z)
            val mix = clamp(temperature / 30.0, 0.0, 1.0)
            val colorCold = type.colorCold()
            val colorWarm = type.colorWarm()
            var g = mix(colorCold.floatY().toDouble(),
                    colorWarm.floatY().toDouble(), mix)
            if (!type.isEvergreen) {
                val autumn = climateGenerator.autumnLeaves(y.toDouble())
                val colorAutumn = type.colorAutumn()
                g = mix(g, colorAutumn.floatY().toDouble(), autumn)
            }
            return g.toFloat()
        }
        return 1.0f
    }

    override fun particleColorB(face: Face,
                                terrain: TerrainClient,
                                x: Int,
                                y: Int,
                                z: Int,
                                data: Int): Float {
        val environment = terrain.world.environment
        if (environment is EnvironmentClimate) {
            val climateGenerator = environment.climate()
            val type = treeRegistry[data]
            val temperature = climateGenerator.temperature(x, y, z)
            val mix = clamp(temperature / 30.0, 0.0, 1.0)
            val colorCold = type.colorCold()
            val colorWarm = type.colorWarm()
            var b = mix(colorCold.floatZ().toDouble(),
                    colorWarm.floatZ().toDouble(), mix)
            if (!type.isEvergreen) {
                val autumn = climateGenerator.autumnLeaves(y.toDouble())
                val colorAutumn = type.colorAutumn()
                b = mix(b, colorAutumn.floatZ().toDouble(), autumn)
            }
            return b.toFloat()
        }
        return 1.0f
    }

    override fun particleTexture(face: Face,
                                 terrain: TerrainClient,
                                 x: Int,
                                 y: Int,
                                 z: Int,
                                 data: Int): TerrainTexture? {
        return textures?.get(data)?.second
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
        return -2
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
        if (!isCovered(terrain, x, y, z)) {
            val type = treeRegistry[data]
            val climateLayer = info.get<ClimateInfoLayer>(
                    "VanillaBasics:Climate")
            val temperature = climateLayer.temperature(x, y, z)
            val mix = clamp((temperature + 20.0) / 50.0, 0.0, 1.0)
            val colorCold = type.colorCold()
            val colorWarm = type.colorWarm()
            var r = mix(colorCold.floatX().toDouble(),
                    colorWarm.floatX().toDouble(), mix)
            var g = mix(colorCold.floatY().toDouble(),
                    colorWarm.floatY().toDouble(), mix)
            var b = mix(colorCold.floatZ().toDouble(),
                    colorWarm.floatZ().toDouble(), mix)
            if (!type.isEvergreen) {
                val environment = terrain.world.environment
                if (environment is EnvironmentClimate) {
                    val climateGenerator = environment.climate()
                    val autumn = climateGenerator.autumnLeaves(y.toDouble())
                    val colorAutumn = type.colorAutumn()
                    r = mix(r, colorAutumn.floatX().toDouble(), autumn)
                    g = mix(g, colorAutumn.floatY().toDouble(), autumn)
                    b = mix(b, colorAutumn.floatZ().toDouble(), autumn)
                }
            }
            if (lod) {
                modelsFancy?.get(data)?.addToChunkMesh(mesh, terrain, x, y, z,
                        xx, yy, zz, r, g, b, 1.0, lod)
            } else {
                modelsFast?.get(data)?.addToChunkMesh(mesh, terrain, x, y, z,
                        xx, yy, zz, r, g, b, 1.0, lod)
            }
        }
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = treeRegistry.values().asSequence().map {
            if (it == null) {
                return@map null
            }
            return@map Triple(it,
                    registry.registerTexture(
                            it.texture() + "/LeavesFancy.png",
                            ShaderAnimation.LEAVES),
                    registry.registerTexture(
                            it.texture() + "/LeavesFast.png"))
        }.toArray()
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            modelsFancy = it.asSequence().map {
                if (it == null) {
                    return@map null
                }
                val shapes = ArrayList<BlockModelComplex.Shape>()
                val shape = BlockModelComplex.ShapeBox(it.second, it.second,
                        it.second, it.second, it.second, it.second, -8.0, -8.0,
                        -8.0, 8.0, 8.0, 8.0, 1.0, 1.0, 1.0, 1.0)
                shapes.add(shape)
                BlockModelComplex(registry, shapes, 0.0625)
            }.toArray()
            modelsFast = it.asSequence().map {
                if (it == null) {
                    return@map null
                }
                BlockModelSimpleBlock(this, registry, it.third, it.third,
                        it.third, it.third, it.third, it.third, 1.0, 1.0,
                        1.0, 1.0)
            }.toArray()
            modelsColored = it.asSequence().map {
                if (it == null) {
                    return@map null
                }
                val colorCold = it.first.colorCold()
                val colorWarm = it.first.colorWarm()
                val r = mix(colorCold.x, colorWarm.x, 0.5)
                val g = mix(colorCold.y, colorWarm.y, 0.5)
                val b = mix(colorCold.z, colorWarm.z, 0.5)
                val shapes = ArrayList<BlockModelComplex.Shape>()
                val shape = BlockModelComplex.ShapeBox(it.second, it.second,
                        it.second, it.second, it.second, it.second, -8.0, -8.0,
                        -8.0, 8.0, 8.0, 8.0, r, g, b, 1.0)
                shapes.add(shape)
                BlockModelComplex(registry, shapes, 0.0625)
            }.toArray()
        }
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        modelsColored?.get(item.data())?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        modelsColored?.get(item.data())?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return materials.log.name(item) + " Leaves"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }

    private fun destroy(terrain: TerrainServer.TerrainMutable,
                        pos: Vector3i,
                        data: Int,
                        length: Int) {
        var length = length
        val block = terrain.block(pos.x, pos.y, pos.z)
        val type = terrain.type(block)
        val d = terrain.data(block)
        if (type != this || d != data) {
            return
        }
        var checks = Pool { MutableVector3i() }
        var checks2 = Pool { MutableVector3i() }
        var checksSwap: Pool<MutableVector3i>
        checks.push().set(pos)
        var i = 0
        while (i < 5 && !checks.isEmpty()) {
            for (check in checks) {
                val checkBlock = terrain.block(check.x, check.y,
                        check.z)
                val checkData = terrain.data(checkBlock)
                if (checkData == data) {
                    val checkType = terrain.type(checkBlock)
                    if (checkType == materials.log) {
                        return
                    }
                    if (i < 10 && checkType == this) {
                        checks2.push().set(check.x, check.y,
                                check.z + 1)
                        checks2.push().set(check.x, check.y,
                                check.z - 1)
                        checks2.push().set(check.x, check.y - 1,
                                check.z)
                        checks2.push().set(check.x + 1, check.y,
                                check.z)
                        checks2.push().set(check.x, check.y + 1,
                                check.z)
                        checks2.push().set(check.x - 1, check.y,
                                check.z)
                    }
                }
            }
            checks.reset()
            checksSwap = checks
            checks = checks2
            checks2 = checksSwap
            i++
        }
        terrain.world.dropItems(drops(ItemStack(materials.air, 0), d),
                pos.x, pos.y, pos.z)
        terrain.typeData(pos.x, pos.y, pos.z, materials.air, 0)
        if (length-- > 0) {
            destroy(terrain, pos.plus(Vector3i(-1, -1, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, -1, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, -1, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(-1, 0, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, 0, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, 0, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(-1, 1, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, 1, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, 1, -1)), data, length)
            destroy(terrain, pos.plus(Vector3i(-1, -1, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, -1, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, -1, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(-1, 0, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, 0, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, 0, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(-1, 1, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, 1, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, 1, 0)), data, length)
            destroy(terrain, pos.plus(Vector3i(-1, -1, 1)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, -1, 1)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, -1, 1)), data, length)
            destroy(terrain, pos.plus(Vector3i(-1, 0, 1)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, 0, 1)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, 0, 1)), data, length)
            destroy(terrain, pos.plus(Vector3i(-1, 1, 1)), data, length)
            destroy(terrain, pos.plus(Vector3i(0, 1, 1)), data, length)
            destroy(terrain, pos.plus(Vector3i(1, 1, 1)), data, length)
        }
    }

    private fun isCovered(terrain: Terrain,
                          x: Int,
                          y: Int,
                          z: Int): Boolean {
        for (face in Face.values()) {
            val xx = x + face.x
            val yy = y + face.y
            val zz = z + face.z
            if (terrain.type(xx, yy, zz).isReplaceable(terrain, xx, yy, zz)) {
                return false
            }
        }
        return true
    }
}
