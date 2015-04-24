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

package org.tobi29.scapes.vanilla.basics.material.block.structural;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.Optional;

public class BlockGlass extends VanillaBlock {
    private TerrainTexture textureFrame, textureTransparent;
    private BlockModel modelFrame, modelTransparent;

    public BlockGlass(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Glass");
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return 1;
    }

    @Override
    public String getFootStep(int data) {
        return "VanillaBasics:sound/footsteps/Stone.ogg";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Stone.ogg";
    }

    @Override
    public Optional<TerrainTexture> getParticleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textureFrame);
    }

    @Override
    public boolean isTransparent(Terrain terrain, int x, int y, int z) {
        return true;
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
        if (lod) {
            modelTransparent
                    .addToChunkMesh(meshAlpha, terrain, x, y, z, xx, yy, zz,
                            1.0f, 1.0f, 1.0f, 1.0f, lod);
        }
        modelFrame
                .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                        1.0f, 1.0f, lod);
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureFrame = registry.registerTexture(
                "VanillaBasics:image/terrain/Glass.png");
        textureTransparent = registry.registerTexture(
                "VanillaBasics:image/terrain/GlassTransparent.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelFrame = new BlockModelSimpleBlock(this, registry, textureFrame,
                textureFrame, textureFrame, textureFrame, textureFrame,
                textureFrame, 1.0f, 1.0f, 1.0f, 1.0f);
        modelTransparent =
                new BlockModelSimpleBlock(this, registry, textureTransparent,
                        textureTransparent, textureTransparent,
                        textureTransparent, textureTransparent,
                        textureTransparent, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GraphicsSystem graphics, Shader shader,
            float r, float g, float b, float a) {
        modelFrame.render(graphics, shader);
        modelTransparent.render(graphics, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GraphicsSystem graphics,
            Shader shader, float r, float g, float b, float a) {
        modelFrame.renderInventory(graphics, shader);
        modelTransparent.renderInventory(graphics, shader);
    }

    @Override
    public String getName(ItemStack item) {
        return "Glass";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 16;
    }
}
