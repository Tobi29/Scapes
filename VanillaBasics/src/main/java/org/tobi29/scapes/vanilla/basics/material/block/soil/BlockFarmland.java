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

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BlockFarmland extends VanillaBlock {
    private TerrainTexture textureTop, textureSide;
    private BlockModel model;

    public BlockFarmland(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Farmland");
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return "Shovel".equals(item.getMaterial().getToolType(item)) ? 2 : 20;
    }

    @Override
    public List<ItemStack> getDrops(ItemStack item, int data) {
        return Collections.singletonList(new ItemStack(materials.dirt, data));
    }

    @Override
    public String getFootStep(int data) {
        return "VanillaBasics:sound/footsteps/Dirt.ogg";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Stone.ogg";
    }

    @Override
    public Optional<TerrainTexture> getParticleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textureSide);
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, int x, int y, int z, float xx, float yy,
            float zz, boolean lod) {
        model.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                1.0f, 1.0f);
    }

    @Override
    public boolean causesTileUpdate() {
        return true;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Farmland.png");
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/soil/Dirt.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        model = new BlockModelSimpleBlock(this, registry, textureTop,
                textureSide, textureSide, textureSide, textureSide, textureSide,
                1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GraphicsSystem graphics, Shader shader,
            float r, float g, float b, float a) {
        model.render(graphics, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GraphicsSystem graphics,
            Shader shader, float r, float g, float b, float a) {
        model.renderInventory(graphics, shader);
    }

    @Override
    public String getName(ItemStack item) {
        return "Dirt";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 16;
    }
}
