/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.vanilla.basics.generator;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.World;
import org.tobi29.scapes.chunk.generator.ChunkPopulator;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfinite;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.noise.layer.*;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.generator.decorator.BiomeDecorator;
import org.tobi29.scapes.vanilla.basics.generator.structure.StructureOre;
import org.tobi29.scapes.vanilla.basics.generator.structure.StructureRock;
import org.tobi29.scapes.vanilla.basics.generator.structure.StructureSmallRuin;
import org.tobi29.scapes.vanilla.basics.material.OreType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.*;
import java.util.stream.Stream;

public class ChunkPopulatorOverworld implements ChunkPopulator {
    private final VanillaBasics plugin;
    private final long seedInt;
    private final Map<BiomeGenerator.Biome, BiomeDecoratorChooser> biomes =
            new EnumMap<>(BiomeGenerator.Biome.class);
    private final BiomeGenerator biomeGenerator;

    public ChunkPopulatorOverworld(World world, VanillaBasics plugin,
            BiomeGenerator biomeGenerator) {
        this.plugin = plugin;
        this.biomeGenerator = biomeGenerator;
        Random random = new Random(world.getSeed());
        seedInt = (long) random.nextInt() << 32;
        for (BiomeGenerator.Biome biome : BiomeGenerator.Biome.values()) {
            BiomeDecoratorChooser chooser =
                    new BiomeDecoratorChooser(plugin.getBiomeDecorators(biome),
                            random);
            biomes.put(biome, chooser);
        }
    }

    @Override
    public void populate(TerrainServer.TerrainMutable terrain, int x, int y,
            int dx, int dy) {
        int hash = 17;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        Random random = new Random(hash + seedInt);
        ChunkGeneratorOverworld gen =
                (ChunkGeneratorOverworld) terrain.world().getGenerator();
        VanillaMaterial materials = plugin.getMaterials();
        int passes = dx * dy / 256;
        for (int i = 0; i < passes; i++) {
            if (random.nextInt(400) == 0) {
                int xx = random.nextInt(dx) + x;
                int yy = random.nextInt(dy) + y;
                int zz = terrain.getHighestTerrainBlockZAt(xx, yy);
                StructureSmallRuin
                        .placeRandomRuin(terrain, xx, yy, zz, materials,
                                gen.getStoneType(xx + random.nextInt(200) - 100,
                                        yy + random.nextInt(200) - 100,
                                        zz - random.nextInt(100)), random);
            }
            int xx = x + (dx >> 1);
            int yy = y + (dy >> 1);
            int zz = random.nextInt(terrain.getHighestTerrainBlockZAt(xx, yy));
            int data = terrain.data(xx, yy, zz);
            if (gen.getStoneType(xx + random.nextInt(21) - 10,
                    yy + random.nextInt(21) - 10, zz + random.nextInt(9) - 4) !=
                    data) {
                OreType oreType = gen.getRandomOreType(plugin, data, random);
                if (oreType != null) {
                    StructureOre.genOre(terrain, xx, yy, zz, materials.stoneRaw,
                            oreType.getBlockType(), (int) FastMath
                                    .ceil(random.nextDouble() *
                                            oreType.getSize()), (int) FastMath
                                    .ceil(random.nextDouble() *
                                            oreType.getSize()), (int) FastMath
                                    .ceil(random.nextDouble() *
                                            oreType.getSize()),
                            oreType.getChance(), random);
                    if (random.nextInt(oreType.getRockChance()) == 0) {
                        int xxx = xx + random.nextInt(21) - 10;
                        int yyy = yy + random.nextInt(21) - 10;
                        int zzz = terrain.getHighestTerrainBlockZAt(xxx, yyy) -
                                random.nextInt(4);
                        BlockType blockType = terrain.type(xxx, yyy, zzz);
                        if (blockType == materials.grass ||
                                blockType == materials.dirt ||
                                blockType == materials.sand ||
                                blockType == materials.stoneRaw) {
                            double size;
                            if (random.nextInt(30) == 0) {
                                size = random.nextDouble() * 4.0 + 3.0;
                            } else {
                                size = random.nextDouble() * 2.0 + 2.0;
                            }
                            StructureRock.genOreRock(terrain, xxx, yyy, zzz,
                                    materials.stoneRaw, oreType.getBlockType(),
                                    gen.getStoneType(xxx, yyy, zzz), 10, size,
                                    random);
                        }
                    }
                }
            }
        }
        BiomeDecoratorChooser biome =
                biomes.get(biomeGenerator.get(x + dx / 2, y + dy / 2));
        if (biome.decorators.length > 0) {
            BiomeDecorator decorator = biome.decorators[biome.noise
                    .getInt(x + dx / 2, y + dy / 2)];
            for (int yyy = 0; yyy < dy; yyy++) {
                int yyyy = yyy + y;
                for (int xxx = 0; xxx < dx; xxx++) {
                    int xxxx = xxx + x;
                    decorator.decorate(terrain, xxxx, yyyy, materials, random);
                }
            }
        }
    }

    @Override
    public void load(TerrainServer.TerrainMutable terrain, int x, int y, int dx,
            int dy) {
        if (terrain instanceof TerrainInfinite) {
            ((TerrainInfinite) terrain).getChunk(x >> 4, y >> 4).ifPresent(
                    chunk -> ((WorldEnvironmentOverworld) terrain.world()
                            .getEnvironment())
                            .simulateSeason(terrain, x, y, dx, dy,
                                    chunk.getMetaData("Vanilla")));
        }
    }

    private static class BiomeDecoratorChooser {
        private final RandomNoiseLayer noise;
        private final BiomeDecorator[] decorators;

        public BiomeDecoratorChooser(Stream<BiomeDecorator> collection,
                Random random) {
            List<BiomeDecorator> decorators = new ArrayList<>();
            collection.forEach(decorator -> {
                for (int i = 0; i < decorator.getWeight(); i++) {
                    decorators.add(decorator);
                }
            });
            this.decorators =
                    decorators.toArray(new BiomeDecorator[decorators.size()]);
            RandomNoiseLayer base =
                    new RandomNoiseRandomLayer(random.nextLong(),
                            decorators.size());
            RandomNoiseLayer zoom = new RandomNoiseZoomLayer(base, 2048);
            RandomNoiseLayer noise =
                    new RandomNoiseSimplexNoiseLayer(zoom, random.nextLong(),
                            1024);
            this.noise = new RandomNoiseNoiseLayer(noise, random.nextLong(), 3);
        }
    }
}
