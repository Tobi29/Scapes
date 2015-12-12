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

import java8.util.Optional;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.generator.ChunkGenerator;
import org.tobi29.scapes.chunk.generator.GeneratorOutput;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.noise.layer.*;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.OreType;
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.update.UpdateLavaFlow;

import java.util.Iterator;
import java.util.Random;

public class ChunkGeneratorOverworld implements ChunkGenerator {
    private final Random random = new Random();
    private final VanillaMaterial materials;
    private final RandomNoiseLayer sandLayer;
    private final RandomNoiseLayer sandstoneLayer;
    private final RandomNoiseLayer[] stoneLayers;
    private final TerrainGenerator terrainGenerator;
    private final TerrainGenerator.TerrainGeneratorLayer layer =
            new TerrainGenerator.TerrainGeneratorLayer();
    private final TerrainGenerator.TerrainGeneratorOutput generator =
            new TerrainGenerator.TerrainGeneratorOutput();
    private final short[] sandstoneLayers;
    private final long seedInt;

    public ChunkGeneratorOverworld(Random random,
            TerrainGenerator terrainGenerator, VanillaMaterial materials) {
        this.materials = materials;
        GameRegistry.Registry<StoneType> stoneRegistry =
                materials.registry.get("VanillaBasics", "StoneType");
        int[][] stoneTypes = {new int[]{stoneRegistry.get(StoneType.GRANITE),
                stoneRegistry.get(StoneType.GABBRO),
                stoneRegistry.get(StoneType.DIORITE)},
                new int[]{stoneRegistry.get(StoneType.ANDESITE),
                        stoneRegistry.get(StoneType.BASALT),
                        stoneRegistry.get(StoneType.DACITE),
                        stoneRegistry.get(StoneType.RHYOLITE),
                        stoneRegistry.get(StoneType.MARBLE),
                        stoneRegistry.get(StoneType.GRANITE),
                        stoneRegistry.get(StoneType.GABBRO),
                        stoneRegistry.get(StoneType.DIORITE)},
                new int[]{stoneRegistry.get(StoneType.ANDESITE),
                        stoneRegistry.get(StoneType.BASALT),
                        stoneRegistry.get(StoneType.DACITE),
                        stoneRegistry.get(StoneType.RHYOLITE),
                        stoneRegistry.get(StoneType.DIRT_STONE)},
                new int[]{stoneRegistry.get(StoneType.DIRT_STONE),
                        stoneRegistry.get(StoneType.CHALK),
                        stoneRegistry.get(StoneType.CLAYSTONE),
                        stoneRegistry.get(StoneType.CHERT),
                        stoneRegistry.get(StoneType.CONGLOMERATE)}};
        seedInt = (long) random.nextInt() << 32;
        this.terrainGenerator = terrainGenerator;
        sandstoneLayers = new short[512];
        for (int i = 0; i < sandstoneLayers.length; i++) {
            sandstoneLayers[i] = (short) random.nextInt(6);
        }
        RandomNoiseLayer sandBase =
                new RandomNoiseRandomLayer(random.nextLong(), 6);
        RandomNoiseLayer sandZoom = new RandomNoiseZoomLayer(sandBase, 1024);
        RandomNoiseLayer sandNoise =
                new RandomNoiseSimplexNoiseLayer(sandZoom, random.nextLong(),
                        128);
        sandLayer = new RandomNoiseNoiseLayer(sandNoise, random.nextLong(), 2);
        stoneLayers = new RandomNoiseLayer[stoneTypes.length];
        for (int i = 0; i < stoneTypes.length; i++) {
            RandomNoiseLayer stoneBase =
                    new RandomNoiseRandomLayer(random.nextLong(),
                            stoneTypes[i].length);
            RandomNoiseLayer stoneFilter =
                    new RandomNoiseFilterLayer(stoneBase, stoneTypes[i]);
            RandomNoiseLayer stoneZoom =
                    new RandomNoiseZoomLayer(stoneFilter, 2048);
            RandomNoiseLayer stoneNoise =
                    new RandomNoiseSimplexNoiseLayer(stoneZoom,
                            random.nextLong(), 1024);
            stoneLayers[i] =
                    new RandomNoiseNoiseLayer(stoneNoise, random.nextLong(), 3);
        }
        RandomNoiseLayer sandstoneBase =
                new RandomNoiseRandomLayer(random.nextLong(), 4);
        RandomNoiseLayer sandstoneZoom =
                new RandomNoiseZoomLayer(sandstoneBase, 2048);
        RandomNoiseLayer sandstoneNoise =
                new RandomNoiseSimplexNoiseLayer(sandstoneZoom,
                        random.nextLong(), 1024);
        sandstoneLayer =
                new RandomNoiseNoiseLayer(sandstoneNoise, random.nextLong(), 3);
    }

    public Optional<OreType> randomOreType(VanillaBasics plugin, int stoneType,
            Random random) {
        OreType type = null;
        int max = -1;
        Iterator<OreType> iterator = plugin.getOreTypes()
                .filter(ore -> ore.stoneTypes().contains(stoneType)).iterator();
        while (iterator.hasNext()) {
            OreType oreType = iterator.next();
            int check = random.nextInt(oreType.rarity());
            if (random.nextBoolean()) {
                if (check > max) {
                    type = oreType;
                    max = check;
                }
            } else {
                if (check >= max) {
                    type = oreType;
                    max = check;
                }
            }
        }
        return Optional.ofNullable(type);
    }

    public short stoneType(int xxx, int yyy, int zzz) {
        zzz += (int) (terrainGenerator.generateMountainFactorLayer(xxx, yyy) *
                300);
        if (zzz <= 96) {
            return (short) stoneLayers[0].getInt(xxx, yyy);
        } else if (zzz <= 240) {
            return (short) stoneLayers[1].getInt(xxx, yyy);
        } else if (terrainGenerator.generateVolcanoFactorLayer(xxx, yyy) >
                0.4f) {
            return (short) stoneLayers[2].getInt(xxx, yyy);
        } else {
            return (short) stoneLayers[3].getInt(xxx, yyy);
        }
    }

    @Override
    public void seed(int x, int y) {
        int hash = 17;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        random.setSeed(hash + seedInt);
    }

    @Override
    public void makeLand(int x, int y, int z, int dz, GeneratorOutput output) {
        terrainGenerator.generate(x, y, layer);
        terrainGenerator.generate(x, y, layer, generator);
        int stoneType;
        int sandType = generator.beach ? sandLayer.getInt(x, y) : 4;
        boolean sandstone = sandstoneLayer.getInt(x, y) == 0;
        int stoneTypeZShift =
                random.nextInt(6) - (int) (generator.mountainFactor * 300);
        int lastFree = FastMath.clamp((int) generator.height,
                (int) generator.waterHeight, dz - 1);
        int waterLevel = (int) generator.waterHeight - 1;
        int riverBedLevel = waterLevel - 14;
        int zzzShifted = lastFree + stoneTypeZShift;
        if (zzzShifted <= 96) {
            stoneType = stoneLayers[0].getInt(x, y);
        } else if (zzzShifted <= 240) {
            stoneType = stoneLayers[1].getInt(x, y);
        } else if (generator.volcanoFactor > 0.4f) {
            stoneType = stoneLayers[2].getInt(x, y);
        } else {
            stoneType = stoneLayers[3].getInt(x, y);
        }
        for (int zz = dz - 1; zz > lastFree; zz--) {
            output.type(zz, materials.air);
            output.data(zz, 0);
        }
        for (int zz = lastFree; zz >= z; zz--) {
            zzzShifted = zz + stoneTypeZShift;
            if (zzzShifted == 240) {
                stoneType = (short) stoneLayers[1].getInt(x, y);
            } else if (zzzShifted == 96) {
                stoneType = (short) stoneLayers[0].getInt(x, y);
            }
            BlockType type;
            int data = 0;
            if (zz < generator.height) {
                if (zz == 0) {
                    type = materials.bedrock;
                } else if (zz < generator.magmaHeight) {
                    type = materials.lava;
                } else if (generator.caveRiver >
                        FastMath.abs(zz - generator.caveRiverHeight) / 16.0) {
                    if (zz < generator.caveRiverHeight) {
                        type = materials.water;
                    } else {
                        type = materials.air;
                    }
                } else if (generator.cave >
                        FastMath.abs(zz - generator.caveHeight) / 8.0) {
                    type = materials.air;
                } else {
                    if (sandType < 3) {
                        if (lastFree - zz < 9 &&
                                lastFree - zz < 5 + random.nextInt(3)) {
                            type = materials.sand;
                            data = sandType;
                        } else {
                            type = materials.stoneRaw;
                        }
                    } else if (lastFree >= waterLevel) {
                        if (zz == lastFree) {
                            if (generator.soiled) {
                                type = materials.grass;
                                data = random.nextInt(9);
                            } else {
                                if (generator.lavaChance > 0 &&
                                        random.nextInt(generator.lavaChance) ==
                                                0) {
                                    type = materials.lava;
                                    output.updates.add(new UpdateLavaFlow()
                                            .set(x, y, zz, 0.0));
                                } else {
                                    type = materials.stoneRaw;
                                }
                            }
                        } else if (lastFree - zz < 9) {
                            if (lastFree - zz < 5 + random.nextInt(5)) {
                                if (generator.soiled) {
                                    type = materials.dirt;
                                } else {
                                    type = materials.stoneRaw;
                                }
                            } else {
                                type = materials.stoneRaw;
                            }
                        } else {
                            type = materials.stoneRaw;
                        }
                    } else if (lastFree > riverBedLevel && lastFree - zz < 9 &&
                            generator.river < 0.9 &&
                            lastFree - zz < 5 + random.nextInt(3)) {
                        type = materials.sand;
                        data = 2;
                    } else {
                        type = materials.stoneRaw;
                    }
                }
                if (type == materials.stoneRaw) {
                    if (sandstone && zz > 240) {
                        type = materials.sandstone;
                        data = sandstoneLayers[zz];
                    } else {
                        data = stoneType;
                    }
                }
            } else {
                if (zz < generator.waterHeight) {
                    type = materials.water;
                } else {
                    type = materials.air;
                }
                lastFree = zz - 1;
            }
            output.type(zz, type);
            output.data(zz, data);
        }
    }
}
