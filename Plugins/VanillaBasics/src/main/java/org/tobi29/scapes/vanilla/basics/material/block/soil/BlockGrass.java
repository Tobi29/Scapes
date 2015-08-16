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

package org.tobi29.scapes.vanilla.basics.material.block.soil;

import org.tobi29.scapes.block.*;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelComplex;
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityFarmlandServer;
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator;
import org.tobi29.scapes.vanilla.basics.generator.ClimateInfoLayer;
import org.tobi29.scapes.vanilla.basics.generator.WorldEnvironmentOverworld;
import org.tobi29.scapes.vanilla.basics.material.CropType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;
import org.tobi29.scapes.vanilla.basics.material.update.UpdateGrassGrowth;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BlockGrass extends VanillaBlock {
    private final GameRegistry.Registry<CropType> cropRegistry;
    private TerrainTexture textureTop;
    private TerrainTexture textureSide1Dirt;
    private TerrainTexture textureSide1Sand;
    private TerrainTexture textureSide2;
    private TerrainTexture textureBottomDirt;
    private TerrainTexture textureBottomSand;
    private TerrainTexture[] texturesGrass;
    private BlockModel modelBlockGrass;
    private BlockModel modelBlockDirt;
    private BlockModel modelBlockSand;
    private BlockModel modelBlockFastGrass;
    private BlockModel modelBlockFastDirt;
    private BlockModel modelBlockFastSand;
    private BlockModel[] modelsTallGrass;

    public BlockGrass(VanillaMaterial materials,
            GameRegistry.Registry<CropType> cropRegistry) {
        super(materials, "vanilla.basics.block.Grass");
        this.cropRegistry = cropRegistry;
    }

    @Override
    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        if ("Hoe".equals(item.material().toolType(item))) {
            if (terrain.data(x, y, z) > 0) {
                if (item.material().toolLevel(item) >= 10) {
                    terrain.world().dropItem(
                            new ItemStack(materials.grassBundle, (short) 0,
                                    terrain.data(x, y, z)), x, y, z + 1);
                    terrain.data(x, y, z, (short) 0);
                } else {
                    terrain.world().dropItem(
                            new ItemStack(materials.grassBundle, (short) 0), x,
                            y, z + 1);
                    terrain.data(x, y, z, (short) (terrain.data(x, y, z) - 1));
                }
                Random random = ThreadLocalRandom.current();
                if (random.nextInt(40) == 0) {
                    terrain.world().dropItem(new ItemStack(materials.seed,
                                    (short) random.nextInt(
                                            cropRegistry.values().size())), x,
                            y, z + 1);
                }
            } else {
                terrain.type(x, y, z, materials.farmland);
                terrain.world().addEntity(
                        new EntityFarmlandServer(terrain.world(),
                                new Vector3d(x + 0.5, y + 0.5, z + 0.5), 0.5f,
                                0.5f, 0.5f));
            }
            return false;
        }
        return true;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Shovel".equals(item.material().toolType(item)) ? 3 :
                "Hoe".equals(item.material().toolType(item)) ? 0.2 : 30;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        return Collections
                .singletonList(new ItemStack(materials.dirt, (short) 0));
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Grass.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Stone.ogg";
    }

    @Override
    public float particleColorR(Face face, TerrainClient terrain, int x, int y,
            int z) {
        if (face == Face.DOWN) {
            return 1.0f;
        } else {
            WorldEnvironmentOverworld environment =
                    (WorldEnvironmentOverworld) terrain.world().environment();
            ClimateGenerator climateGenerator = environment.climate();
            return (float) climateGenerator
                    .grassColorR(climateGenerator.temperature(x, y, z),
                            climateGenerator.humidity(x, y, z));
        }
    }

    @Override
    public float particleColorG(Face face, TerrainClient terrain, int x, int y,
            int z) {
        if (face == Face.DOWN) {
            return 1.0f;
        } else {
            WorldEnvironmentOverworld environment =
                    (WorldEnvironmentOverworld) terrain.world().environment();
            ClimateGenerator climateGenerator = environment.climate();
            return (float) climateGenerator
                    .grassColorG(climateGenerator.temperature(x, y, z),
                            climateGenerator.humidity(x, y, z));
        }
    }

    @Override
    public float particleColorB(Face face, TerrainClient terrain, int x, int y,
            int z) {
        if (face == Face.DOWN) {
            return 1.0f;
        } else {
            WorldEnvironmentOverworld environment =
                    (WorldEnvironmentOverworld) terrain.world().environment();
            ClimateGenerator climateGenerator = environment.climate();
            return (float) climateGenerator
                    .grassColorB(climateGenerator.temperature(x, y, z),
                            climateGenerator.humidity(x, y, z));
        }
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        if (face == Face.DOWN) {
            return Optional.of(textureBottomDirt);
        }
        return Optional.of(textureTop);
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        WorldEnvironmentOverworld environment =
                (WorldEnvironmentOverworld) terrain.world().environment();
        ClimateGenerator climateGenerator = environment.climate();
        ClimateInfoLayer climateLayer = info.get("VanillaBasics:Climate");
        double temperature = climateLayer.temperature(x, y, z);
        double humidity = climateLayer.humidity(x, y);
        double grassR = climateGenerator.grassColorR(temperature, humidity);
        double grassG = climateGenerator.grassColorG(temperature, humidity);
        double grassB = climateGenerator.grassColorB(temperature, humidity);
        if (lod) {
            modelBlockGrass.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                    (float) grassR, (float) grassG, (float) grassB, 1.0f, lod);
            if (humidity < 0.3) {
                modelBlockSand
                        .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                                1.0f, 1.0f, 1.0f, 1.0f, lod);
            } else {
                modelBlockDirt
                        .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                                1.0f, 1.0f, 1.0f, 1.0f, lod);
            }
            if (data > 0) {
                if (terrain.type(x, y, z + 1) == materials.air) {
                    modelsTallGrass[data]
                            .addToChunkMesh(mesh, terrain, x, y, z + 1, xx, yy,
                                    zz + 1, (float) grassR, (float) grassG,
                                    (float) grassB, 1.0f, lod);
                }
            }
        } else {
            modelBlockFastGrass
                    .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                            (float) grassR, (float) grassG, (float) grassB,
                            1.0f, lod);
            if (humidity < 0.3) {
                modelBlockFastSand
                        .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                                1.0f, 1.0f, 1.0f, 1.0f, lod);
            } else {
                modelBlockFastDirt
                        .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                                1.0f, 1.0f, 1.0f, 1.0f, lod);
            }
        }
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        if (terrain.blockLight(x, y, z + 1) <= 0 &&
                terrain.sunLight(x, y, z + 1) <= 0 || !terrain.type(x, y, z + 1)
                .isTransparent(terrain, x, y, z + 1)) {
            terrain.typeData(x, y, z, materials.dirt, (short) 0);
        }
        if (terrain.highestTerrainBlockZAt(x, y) > z + 1 &&
                !terrain.hasDelayedUpdate(x, y, z)) {
            Random random = ThreadLocalRandom.current();
            terrain.addDelayedUpdate(new UpdateGrassGrowth()
                    .set(x, y, z, random.nextDouble() * 400.0 + 1600.0));
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/GrassTop.png");
        textureSide1Dirt = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/GrassSide.png");
        textureSide1Sand = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/GrassSideSand.png");
        textureSide2 = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/GrassSideFoliage.png");
        textureBottomDirt = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Dirt.png");
        textureBottomSand = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Sand.png");
        texturesGrass = new TerrainTexture[9];
        for (int i = 1; i < texturesGrass.length; i++) {
            texturesGrass[i] = registry.registerTexture(
                    "VanillaBasics:image/terrain/TallGrass" +
                            i + ".png", ShaderAnimation.TALL_GRASS);
        }
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelBlockGrass =
                new BlockModelSimpleBlock(this, registry, textureTop, null,
                        textureSide2, textureSide2, textureSide2, textureSide2,
                        1.0f, 1.0f, 1.0f, 1.0f);
        modelBlockDirt = new BlockModelSimpleBlock(this, registry, null,
                textureBottomDirt, textureSide1Dirt, textureSide1Dirt,
                textureSide1Dirt, textureSide1Dirt, 1.0f, 1.0f, 1.0f, 1.0f);
        modelBlockSand = new BlockModelSimpleBlock(this, registry, null,
                textureBottomSand, textureSide1Sand, textureSide1Sand,
                textureSide1Sand, textureSide1Sand, 1.0f, 1.0f, 1.0f, 1.0f);
        modelBlockFastGrass =
                new BlockModelSimpleBlock(this, registry, textureTop, null,
                        textureTop, textureTop, textureTop, textureTop, 1.0f,
                        1.0f, 1.0f, 1.0f);
        modelBlockFastDirt = new BlockModelSimpleBlock(this, registry, null,
                textureBottomDirt, null, null, null, null, 1.0f, 1.0f, 1.0f,
                1.0f);
        modelBlockFastSand = new BlockModelSimpleBlock(this, registry, null,
                textureBottomSand, null, null, null, null, 1.0f, 1.0f, 1.0f,
                1.0f);
        modelsTallGrass = new BlockModel[texturesGrass.length];
        for (int i = 1; i < texturesGrass.length; i++) {
            List<BlockModelComplex.Shape> shapes = new ArrayList<>();
            BlockModelComplex.Shape shape =
                    new BlockModelComplex.ShapeBillboard(texturesGrass[i],
                            -8.0f, -8.0f, -8.0f, 8.0f, 8.0f, 8.0f, 0.0f, 0.0f,
                            1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            shape.rotateZ(45.0f);
            shapes.add(shape);
            modelsTallGrass[i] =
                    new BlockModelComplex(registry, shapes, 0.0625f);
        }
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        modelBlockGrass.render(gl, shader);
        modelBlockDirt.render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        modelBlockGrass.renderInventory(gl, shader);
        modelBlockDirt.renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return "Grass";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }
}
