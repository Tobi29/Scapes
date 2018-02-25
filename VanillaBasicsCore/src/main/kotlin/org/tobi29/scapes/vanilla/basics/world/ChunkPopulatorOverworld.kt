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

package org.tobi29.scapes.vanilla.basics.world

import org.tobi29.generation.layer.RandomPermutation
import org.tobi29.generation.layer.random
import org.tobi29.generation.layer.randomOffset
import org.tobi29.generation.value.OpenSimplexNoise
import org.tobi29.math.Random
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.generator.ChunkPopulator
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.terrain.TerrainChunk
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.world.decorator.BiomeDecorator
import org.tobi29.scapes.vanilla.basics.world.structure.genOre
import org.tobi29.scapes.vanilla.basics.world.structure.genOreRock
import org.tobi29.scapes.vanilla.basics.world.structure.placeRandomRuin
import org.tobi29.stdex.EnumMap
import org.tobi29.stdex.math.ceilToInt
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.floorToInt
import kotlin.math.abs
import kotlin.math.max

class ChunkPopulatorOverworld(world: WorldServer,
                              private val plugin: VanillaBasics,
                              private val biomeGenerator: BiomeGenerator) : ChunkPopulator {
    private val seedInt: Long
    private val biomes = EnumMap<BiomeGenerator.Biome, BiomeDecoratorChooser>()

    init {
        val random = Random(world.seed)
        seedInt = random.nextInt().toLong() shl 32
        for (biome in BiomeGenerator.Biome.values()) {
            val chooser = BiomeDecoratorChooser(plugin.biomeDecorators(biome),
                    random)
            biomes.put(biome, chooser)
        }
    }

    override fun populate(terrain: TerrainServer,
                          chunk: TerrainChunk) {
        val x = chunk.posBlock.x
        val y = chunk.posBlock.y
        val dx = chunk.size.x
        val dy = chunk.size.y
        var hash = 17
        hash = 31 * hash + x
        hash = 31 * hash + y
        val random = Random(hash + seedInt)
        val gen = terrain.generator as ChunkGeneratorOverworld
        val materials = plugin.materials
        val passes = dx * dy / 64
        for (i in 0 until passes) {
            if (random.nextInt(400) == 0) {
                val xx = random.nextInt(dx) + x
                val yy = random.nextInt(dy) + y
                val zz = terrain.highestTerrainBlockZAt(xx, yy)
                terrain.placeRandomRuin(xx, yy, zz, materials,
                        gen.it(xx + random.nextInt(200) - 100,
                                yy + random.nextInt(200) - 100,
                                zz - random.nextInt(100)).id, random)
            }
            val xx = x + (dx shr 1)
            val yy = y + (dy shr 1)
            val zz = random.nextInt(
                    max(terrain.highestTerrainBlockZAt(xx, yy) - 8, 1))
            val block = terrain.block(xx, yy, zz)
            val type = terrain.type(block)
            val data = terrain.data(block)
            if (type == materials.stoneRaw &&
                    gen.it(xx + random.nextInt(21) - 10,
                            yy + random.nextInt(21) - 10,
                            zz + random.nextInt(9) - 4).id != data) {
                val ore = gen.randomOreType(plugin, data, random)
                if (ore != null) {
                    val ores = terrain.genOre(xx, yy, zz, materials.stoneRaw,
                            ore.type,
                            (random.nextDouble() * ore.size).ceilToInt(),
                            (random.nextDouble() * ore.size).ceilToInt(),
                            (random.nextDouble() * ore.size).ceilToInt(),
                            ore.chance,
                            random)
                    if (ores > 0 && random.nextInt(ore.rockChance) == 0) {
                        val xxx = xx + random.nextInt(21) - 10
                        val yyy = yy + random.nextInt(21) - 10
                        val zzz = terrain.highestTerrainBlockZAt(xxx,
                                yyy) - random.nextInt(4)
                        if (abs(zzz - zz) < ore.rockDistance) {
                            val blockType = terrain.type(xxx, yyy, zzz)
                            if (blockType == materials.grass ||
                                    blockType == materials.dirt ||
                                    blockType == materials.sand ||
                                    blockType == materials.stoneRaw) {
                                val size: Double
                                if (random.nextInt(30) == 0) {
                                    size = random.nextDouble() * 4.0 + 3.0
                                } else {
                                    size = random.nextDouble() * 2.0 + 2.0
                                }
                                terrain.genOreRock(xxx, yyy, zzz,
                                        materials.stoneRaw, ore.type,
                                        gen.it(xxx, yyy, zzz).id,
                                        clamp(64 - ores shr 2, 2, 10), size,
                                        random)
                            }
                        }
                    }
                }
            }
        }
        val biome = biomes[biomeGenerator.get((x + (dx shr 1)).toDouble(),
                (y + (dy shr 1)).toDouble())]
        biome?.biomeDecorator(x + dx / 2, y + dy / 2)?.let { decorator ->
            for (yyy in 0 until dy) {
                val yyyy = yyy + y
                for (xxx in 0 until dx) {
                    val xxxx = xxx + x
                    decorator.decorate(terrain, xxxx, yyyy, materials, random)
                }
            }
        }
    }

    override fun load(terrain: TerrainServer,
                      chunk: TerrainChunk) {
        (terrain.world.environment as EnvironmentOverworldServer).simulateSeason(
                terrain, chunk)
    }

    private class BiomeDecoratorChooser(collection: Sequence<BiomeDecorator>,
                                        random: Random) {
        private val base = RandomPermutation(random)
        private val swirl = OpenSimplexNoise(random.nextLong())
        private val noise = RandomPermutation(random)
        private val decorators: Array<BiomeDecorator>

        init {
            val decorators = ArrayList<BiomeDecorator>()
            collection.forEach { decorator ->
                for (i in 0 until decorator.weight()) {
                    decorators.add(decorator)
                }
            }
            this.decorators = decorators.toTypedArray()
        }

        fun biomeDecorator(x: Int,
                           y: Int) = run {
            noise.randomOffset(3, x, y) { x, y ->
                swirl.randomOffset(512.0, x.toDouble(),
                        y.toDouble()) { x, y ->
                    if (decorators.isEmpty()) {
                        return@run null
                    }
                    decorators[base.random(decorators.size,
                            x.floorToInt() shr 10,
                            y.floorToInt() shr 10)]
                }
            }
        }
    }
}
