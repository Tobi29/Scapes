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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.data.ChunkData;
import org.tobi29.scapes.chunk.generator.ChunkGeneratorInfinite;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkServer;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.noise.layer.*;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.OreType;
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.update.UpdateLavaFlow;

import java.util.Iterator;
import java.util.Random;

public class ChunkGeneratorOverworld implements ChunkGeneratorInfinite {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ChunkGeneratorOverworld.class);
    private final VanillaMaterial materials;
    private final RandomNoiseLayer sandLayer;
    private final RandomNoiseLayer sandstoneLayer;
    private final RandomNoiseLayer[] stoneLayers;
    private final TerrainGenerator terrainGenerator;
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

    @Override
    public void makeLand(int x, int y, TerrainInfiniteChunkServer chunk,
            ChunkData bId, ChunkData bData) {
        long time = System.currentTimeMillis();
        int hash = 17;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        Random random = new Random(hash + seedInt);
        TerrainGenerator.TerrainGeneratorOutput output =
                new TerrainGenerator.TerrainGeneratorOutput();
        for (int xx = 0; xx < 16; xx++) {
            int xxx = (x << 4) + xx;
            for (int yy = 0; yy < 16; yy++) {
                int yyy = (y << 4) + yy;
                terrainGenerator.generate(xxx, yyy, output);
                short stoneType;
                short sandType =
                        output.beach ? (short) sandLayer.getInt(xxx, yyy) : 4;
                boolean sandstone = sandstoneLayer.getInt(xxx, yyy) == 0;
                int stoneTypeZShift =
                        random.nextInt(6) - (int) (output.mountainFactor * 300);
                int lastFree = FastMath.clamp((int) output.height,
                        (int) output.waterHeight, chunk.getZSize() - 1);
                int zzzShifted = lastFree + stoneTypeZShift;
                if (zzzShifted <= 96) {
                    stoneType = (short) stoneLayers[0].getInt(xxx, yyy);
                } else if (zzzShifted <= 240) {
                    stoneType = (short) stoneLayers[1].getInt(xxx, yyy);
                } else if (output.volcanoFactor > 0.4f) {
                    stoneType = (short) stoneLayers[2].getInt(xxx, yyy);
                } else {
                    stoneType = (short) stoneLayers[3].getInt(xxx, yyy);
                }
                for (int zzz = lastFree; zzz >= 0; zzz--) {
                    zzzShifted = zzz + stoneTypeZShift;
                    if (zzzShifted == 240) {
                        stoneType = (short) stoneLayers[1].getInt(xxx, yyy);
                    } else if (zzzShifted == 96) {
                        stoneType = (short) stoneLayers[0].getInt(xxx, yyy);
                    }
                    short blockId;
                    short blockData = 0;
                    if (zzz < output.height) {
                        if (zzz == 0) {
                            blockId = materials.bedrock.getID();
                        } else if (zzz < output.magmaHeight) {
                            blockId = materials.lava.getID();
                        } else {
                            if (sandType < 3) {
                                if (lastFree - zzz < 9) {
                                    if (lastFree - zzz <
                                            5 + random.nextInt(3)) {
                                        blockId = materials.sand.getID();
                                        blockData = sandType;
                                        if (blockData == 1) {
                                            if (random.nextInt(4) == 0 && zzz <
                                                    chunk.getZSize() - 1) {
                                                if (bId.getData(xx, yy, zzz + 1,
                                                        0) == 0) {
                                                    bId.setData(xx, yy, zzz + 1,
                                                            0,
                                                            materials.stoneRock
                                                                    .getID());
                                                    if (random.nextInt(8) ==
                                                            0) {
                                                        bData.setData(xx, yy,
                                                                zzz + 1, 0,
                                                                stoneType);
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        blockId = materials.stoneRaw.getID();
                                    }
                                } else {
                                    blockId = materials.stoneRaw.getID();
                                }
                            } else if (lastFree > 254) {
                                if (zzz == lastFree) {
                                    if (output.soiled) {
                                        blockId = materials.grass.getID();
                                        blockData = (short) random.nextInt(9);
                                    } else {
                                        if (output.lavaChance > 0 &&
                                                random.nextInt(
                                                        output.lavaChance) ==
                                                        0) {
                                            blockId = materials.lava.getID();
                                            chunk.addDelayedUpdate(
                                                    new UpdateLavaFlow()
                                                            .set(xxx, yyy, zzz,
                                                                    0.0));
                                        } else {
                                            blockId =
                                                    materials.stoneRaw.getID();
                                        }
                                    }
                                    if (random.nextInt(128) == 0 &&
                                            zzz < chunk.getZSize() - 1) {
                                        bId.setData(xx, yy, zzz + 1, 0,
                                                materials.stoneRock.getID());
                                        bData.setData(xx, yy, zzz + 1, 0,
                                                stoneType);
                                    }
                                } else if (lastFree - zzz < 9) {
                                    if (lastFree - zzz <
                                            5 + random.nextInt(5)) {
                                        if (output.soiled) {
                                            blockId = materials.dirt.getID();
                                        } else {
                                            blockId =
                                                    materials.stoneRaw.getID();
                                        }
                                    } else {
                                        blockId = materials.stoneRaw.getID();
                                    }
                                } else {
                                    blockId = materials.stoneRaw.getID();
                                }
                            } else {
                                blockId = materials.stoneRaw.getID();
                            }
                        }
                        if (blockId == materials.stoneRaw.getID()) {
                            if (output.caveRiver >
                                    FastMath.abs(zzz - output.caveRiverHeight) /
                                            16.0d) {
                                if (zzz < 128) {
                                    blockId = materials.water.getID();
                                } else {
                                    blockId = materials.air.getID();
                                }
                            } else if (output.cave >
                                    FastMath.abs(zzz - output.caveHeight) /
                                            8.0d) {
                                blockId = materials.air.getID();
                            } else {
                                if (sandstone && zzz > 240) {
                                    blockId = materials.sandstone.getID();
                                    blockData = sandstoneLayers[zzz];
                                } else {
                                    blockData = stoneType;
                                    if (zzz == lastFree) {
                                        if (zzz < chunk.getZSize() - 1 &&
                                                lastFree >= 255 &&
                                                random.nextInt(10) == 0) {
                                            bId.setData(xx, yy, zzz + 1, 0,
                                                    materials.stoneRock
                                                            .getID());
                                            bData.setData(xx, yy, zzz + 1, 0,
                                                    blockData);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (zzz < output.waterHeight) {
                            blockId = materials.water.getID();
                        } else {
                            blockId = materials.air.getID();
                        }
                        lastFree = zzz - 1;
                    }
                    if (blockId != materials.air.getID()) {
                        bId.setData(xx, yy, zzz, 0, blockId);
                        if (blockData != 0) {
                            bData.setData(xx, yy, zzz, 0, blockData);
                        }
                    }
                }
            }
        }
        LOGGER.trace("Generated chunk in {} ms.",
                System.currentTimeMillis() - time);
    }

    public boolean isValidSpawn(int x, int y) {
        TerrainGenerator.TerrainGeneratorOutput output =
                new TerrainGenerator.TerrainGeneratorOutput();
        terrainGenerator.generate(x, y, output);
        return output.height > output.waterHeight;
    }

    public OreType getRandomOreType(VanillaBasics plugin, int stoneType,
            Random random) {
        OreType type = null;
        int max = -1;
        Iterator<OreType> iterator = plugin.getOreTypes()
                .filter(oreType -> oreType.getStoneTypes().contains(stoneType))
                .iterator();
        while (iterator.hasNext()) {
            OreType oreType = iterator.next();
            int check = random.nextInt(oreType.getRarity());
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
        return type;
    }

    public short getStoneType(int xxx, int yyy, int zzz) {
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
}
