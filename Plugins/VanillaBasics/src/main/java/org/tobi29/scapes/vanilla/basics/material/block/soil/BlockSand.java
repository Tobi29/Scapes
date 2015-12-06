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

import java8.util.Optional;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemHeatable;

public class BlockSand extends BlockSoil implements ItemHeatable {
    private TerrainTexture[] textures;
    private BlockModel[] models;

    public BlockSand(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Sand");
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Shovel".equals(item.material().toolType(item)) ? 2 : 20;
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textures[terrain.data(x, y, z)]);
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        models[data]
                .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                        1.0f, 1.0f, lod);
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textures = new TerrainTexture[3];
        textures[0] = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Sand.png");
        textures[1] = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Gravel.png");
        textures[2] = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Clay.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        models = new BlockModel[textures.length];
        for (int i = 0; i < models.length; i++) {
            models[i] = new BlockModelSimpleBlock(this, registry, textures[i],
                    textures[i], textures[i], textures[i], textures[i],
                    textures[i], 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        models[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        models[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        switch (item.data()) {
            case 1:
                return "Gravel";
            case 2:
                return "Clay\nTemp.: " + temperature(item) + " C";
            default:
                return "Sand\nTemp.: " + temperature(item) + " C";
        }
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return temperature(item) == 0 ? 16 : 1;
    }

    @Override
    public void heat(ItemStack item, float temperature) {
        if (item.data() == 1) {
            return;
        }
        float currentTemperature = temperature(item);
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            if (temperature >= meltingPoint(item)) {
                if (item.data() == 0) {
                    item.setMaterial(materials.glass);
                } else if (item.data() == 2) {
                    item.setMaterial(materials.brick);
                }
            }
            item.metaData("Vanilla").setFloat("Temperature", FastMath.max(
                    currentTemperature +
                            (temperature - currentTemperature) / 400.0f, 1.1f));
        }
    }

    @Override
    public void cool(ItemStack item) {
        if (item.data() == 1) {
            return;
        }
        float currentTemperature = temperature(item);
        if (currentTemperature < 1) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.metaData("Vanilla")
                    .setFloat("Temperature", currentTemperature / 1.002f);
        }
    }

    @Override
    public void cool(MobItemServer item) {
        if (item.item().data() == 1) {
            return;
        }
        float currentTemperature = temperature(item.item());
        if (currentTemperature < 1) {
            item.item().metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            if (item.isInWater()) {
                item.item().metaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 4.0f);
            } else {
                item.item().metaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 1.002f);
            }
        }
    }

    @Override
    public float meltingPoint(ItemStack item) {
        switch (item.data()) {
            case 1:
                return 0.0f;
            case 2:
                return 600.0f;
            default:
                return 1000.0f;
        }
    }

    @Override
    public float temperature(ItemStack item) {
        if (item.data() == 1) {
            return 0.0f;
        }
        return item.metaData("Vanilla").getFloat("Temperature");
    }
}
