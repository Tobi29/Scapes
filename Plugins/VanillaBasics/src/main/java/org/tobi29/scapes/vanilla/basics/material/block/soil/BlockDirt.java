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

package org.tobi29.scapes.vanilla.basics.material.block.soil;

import java8.util.Optional;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.EnvironmentClient;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityFarmlandServer;
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator;
import org.tobi29.scapes.vanilla.basics.generator.ClimateInfoLayer;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentClimate;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class BlockDirt extends BlockSoil {
    private TerrainTexture textureDirt, textureSand;
    private BlockModel modelDirt, modelSand;

    public BlockDirt(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Dirt");
    }

    public static boolean isSand(TerrainClient terrain, int x, int y, int z) {
        EnvironmentClient environment = terrain.world().environment();
        if (environment instanceof EnvironmentClimate) {
            EnvironmentClimate environmentClimate =
                    (EnvironmentClimate) environment;
            ClimateGenerator climateGenerator = environmentClimate.climate();
            double humidity = climateGenerator.humidity(x, y, z);
            if (humidity < 0.3) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureDirt = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Dirt.png");
        textureSand = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Sand.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelDirt = new BlockModelSimpleBlock(this, registry, textureDirt,
                textureDirt, textureDirt, textureDirt, textureDirt, textureDirt,
                1.0f, 1.0f, 1.0f, 1.0f);
        modelSand = new BlockModelSimpleBlock(this, registry, textureSand,
                textureSand, textureSand, textureSand, textureSand, textureSand,
                1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader) {
        modelDirt.render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader) {
        modelDirt.renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return "Dirt";
    }

    @Override
    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        if ("Hoe".equals(item.material().toolType(item))) {
            terrain.type(x, y, z, materials.farmland);
            terrain.world().addEntity(new EntityFarmlandServer(terrain.world(),
                    new Vector3d(x + 0.5, y + 0.5, z + 0.5), 0.1f, 0.1f, 0.1f));
            return false;
        }
        return true;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Shovel".equals(item.material().toolType(item)) ? 2 : 20;
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        if (isSand(terrain, x, y, z)) {
            return Optional.of(textureSand);
        } else {
            return Optional.of(textureDirt);
        }
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        ClimateInfoLayer climateLayer = info.get("VanillaBasics:Climate");
        double humidity = climateLayer.humidity(x, y);
        if (humidity < 0.3) {
            modelSand.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f,
                    1.0f, 1.0f, 1.0f, lod);
        } else {
            modelDirt.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f,
                    1.0f, 1.0f, 1.0f, lod);
        }
    }
}
