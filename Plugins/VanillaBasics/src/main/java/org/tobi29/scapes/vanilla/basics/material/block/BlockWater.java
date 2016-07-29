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
package org.tobi29.scapes.vanilla.basics.material.block;

import java8.util.Optional;
import org.tobi29.scapes.block.*;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelLiquid;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.update.UpdateWaterFlow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BlockWater extends VanillaBlock {
    private TerrainTexture textureStill, textureFlow;
    private BlockModel model;

    public BlockWater(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Water");
    }

    @Override
    public void addPointerCollision(int data, Pool<PointerPane> pointerPanes,
            int x, int y, int z) {
    }

    @Override
    public void addCollision(Pool<AABBElement> aabbs, Terrain terrain, int x,
            int y, int z) {
        aabbs.push().set(x, y, z, x + 1, y + 1, z + 1, CollisionWater.INSTANCE);
    }

    @Override
    public List<AABBElement> collision(int data, int x, int y, int z) {
        List<AABBElement> aabbs = new ArrayList<>();
        aabbs.add(new AABBElement(new AABB(x, y, z, x + 1, y + 1, z + 1),
                CollisionWater.INSTANCE));
        return aabbs;
    }

    @Override
    public boolean isReplaceable(Terrain terrain, int x, int y, int z) {
        return true;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return -1;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        return Collections.singletonList(new ItemStack(this, data));
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Water.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "";
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.empty();
    }

    @Override
    public boolean isLiquid() {
        return true;
    }

    @Override
    public boolean isSolid(Terrain terrain, int x, int y, int z) {
        return false;
    }

    @Override
    public byte lightTrough(Terrain terrain, int x, int y, int z) {
        return -1;
    }

    @Override
    public short connectStage(TerrainClient terrain, int x, int y, int z) {
        return 3;
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        model.addToChunkMesh(meshAlpha, terrain, x, y, z, xx, yy, zz, 1.0f,
                1.0f, 1.0f, 1.0f, lod);
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        if (!terrain.hasDelayedUpdate(x, y, z, UpdateWaterFlow.class)) {
            Random random = ThreadLocalRandom.current();
            terrain.addDelayedUpdate(new UpdateWaterFlow()
                    .set(x, y, z, random.nextDouble() * 0.2 + 0.1));
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureStill = registry.registerTexture(
                "VanillaBasics:image/terrain/WaterStill.png", true,
                ShaderAnimation.WATER);
        textureFlow = registry.registerTexture(
                "VanillaBasics:image/terrain/WaterFlow.png", true,
                ShaderAnimation.WATER);
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        model = new BlockModelLiquid(this, registry, textureStill, textureStill,
                textureFlow, textureFlow, textureFlow, textureFlow, 1.0f, 1.0f,
                1.0f, 0.7f, 0.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        model.render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        model.renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return "Water";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 1;
    }
}
