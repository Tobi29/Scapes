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
package org.tobi29.scapes.vanilla.basics.generator;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.generator.ChunkPopulator2D;
import org.tobi29.scapes.chunk.terrain.TerrainChunk2D;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
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

public class ChunkPopulatorOverworld implements ChunkPopulator2D {
    private final VanillaBasics plugin;
    private final long seedInt;
    private final Map<BiomeGenerator.Biome, BiomeDecoratorChooser> biomes =
            new EnumMap<>(BiomeGenerator.Biome.class);
    private final BiomeGenerator biomeGenerator;

    public ChunkPopulatorOverworld(WorldServer world, VanillaBasics plugin,
            BiomeGenerator biomeGenerator) {
        this.plugin = plugin;
        this.biomeGenerator = biomeGenerator;
        Random random = new Random(world.seed());
        seedInt = (long) random.nextInt() << 32;
        for (BiomeGenerator.Biome biome : BiomeGenerator.Biome.values()) {
            BiomeDecoratorChooser chooser =
                    new BiomeDecoratorChooser(plugin.biomeDecorators(biome),
                            random);
            biomes.put(biome, chooser);
        }
    }

    @Override
    public void populate(TerrainServer.TerrainMutable terrain,
            TerrainChunk2D chunk) {
        int x = chunk.blockX();
        int y = chunk.blockY();
        int dx = chunk.blockDX();
        int dy = chunk.blockDY();
        int hash = 17;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        Random random = new Random(hash + seedInt);
        ChunkGeneratorOverworld gen =
                (ChunkGeneratorOverworld) terrain.world().generator();
        VanillaMaterial materials = plugin.getMaterials();
        int passes = dx * dy / 64;
        for (int i = 0; i < passes; i++) {
            if (random.nextInt(400) == 0) {
                int xx = random.nextInt(dx) + x;
                int yy = random.nextInt(dy) + y;
                int zz = terrain.highestTerrainBlockZAt(xx, yy);
                StructureSmallRuin
                        .placeRandomRuin(terrain, xx, yy, zz, materials,
                                gen.stoneType(xx + random.nextInt(200) - 100,
                                        yy + random.nextInt(200) - 100,
                                        zz - random.nextInt(100)), random);
            }
            int xx = x + (dx >> 1);
            int yy = y + (dy >> 1);
            int zz = random.nextInt(terrain.highestTerrainBlockZAt(xx, yy) - 8);
            int data = terrain.data(xx, yy, zz);
            if (gen.stoneType(xx + random.nextInt(21) - 10,
                    yy + random.nextInt(21) - 10, zz + random.nextInt(9) - 4) !=
                    data) {
                Optional<OreType> ore = gen.randomOreType(plugin, data, random);
                if (ore.isPresent()) {
                    OreType oreType = ore.get();
                    int ores = StructureOre
                            .genOre(terrain, xx, yy, zz, materials.stoneRaw,
                                    oreType.type(), (int) FastMath
                                            .ceil(random.nextDouble() *
                                                    oreType.size()),
                                    (int) FastMath.ceil(random.nextDouble() *
                                            oreType.size()), (int) FastMath
                                            .ceil(random.nextDouble() *
                                                    oreType.size()),
                                    oreType.chance(), random);
                    if (ores > 0 && random.nextInt(oreType.rockChance()) == 0) {
                        int xxx = xx + random.nextInt(21) - 10;
                        int yyy = yy + random.nextInt(21) - 10;
                        int zzz = terrain.highestTerrainBlockZAt(xxx, yyy) -
                                random.nextInt(4);
                        if (FastMath.abs(zzz - zz) < oreType.rockDistance()) {
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
                                        materials.stoneRaw, oreType.type(),
                                        gen.stoneType(xxx, yyy, zzz),
                                        FastMath.clamp(64 - ores >> 2, 2, 10),
                                        size, random);
                            }
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
    public void load(TerrainServer.TerrainMutable terrain,
            TerrainChunk2D chunk) {
        ((EnvironmentOverworldServer) terrain.world().environment())
                .simulateSeason(terrain, chunk);
    }

    private static class BiomeDecoratorChooser {
        private final RandomNoiseLayer noise;
        private final BiomeDecorator[] decorators;

        public BiomeDecoratorChooser(Stream<BiomeDecorator> collection,
                Random random) {
            List<BiomeDecorator> decorators = new ArrayList<>();
            collection.forEach(decorator -> {
                for (int i = 0; i < decorator.weight(); i++) {
                    decorators.add(decorator);
                }
            });
            this.decorators =
                    decorators.toArray(new BiomeDecorator[decorators.size()]);
            RandomNoiseLayer base =
                    new RandomNoiseRandomLayer(random.nextLong(),
                            decorators.size());
            RandomNoiseLayer zoom = new RandomNoiseZoomLayer(base, 1024);
            RandomNoiseLayer noise =
                    new RandomNoiseSimplexNoiseLayer(zoom, random.nextLong(),
                            512);
            this.noise = new RandomNoiseNoiseLayer(noise, random.nextLong(), 3);
        }
    }
}
