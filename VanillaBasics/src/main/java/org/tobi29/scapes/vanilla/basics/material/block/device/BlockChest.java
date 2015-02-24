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

package org.tobi29.scapes.vanilla.basics.material.block.device;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityChestServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer;

import java.util.Optional;

public class BlockChest extends VanillaBlockContainer {
    private TerrainTexture textureTop, textureFront, textureSide;
    private BlockModel[] models;

    public BlockChest(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Chest");
    }

    @Override
    protected EntityContainerServer placeEntity(TerrainServer terrain, int x,
            int y, int z) {
        EntityChestServer entity = new EntityChestServer(terrain.getWorld(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5));
        entity.onSpawn();
        terrain.getWorld().addEntity(entity);
        return entity;
    }

    @Override
    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        if (face == Face.UP || face == Face.DOWN) {
            return false;
        }
        terrain.setBlockData(x, y, z, face.getData());
        return true;
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return "Axe".equals(item.getMaterial().getToolType(item)) ? 4 : -1;
    }

    @Override
    public String getFootStep(int data) {
        return "VanillaBasics:sound/footsteps/Wood.ogg";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Axe.ogg";
    }

    @Override
    public Optional<TerrainTexture> getParticleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textureSide);
    }

    @Override
    public boolean isTransparent(Terrain terrain, int x, int y, int z) {
        return true;
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, int x, int y, int z, float xx, float yy,
            float zz, boolean lod) {
        models[data]
                .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                        1.0f, 1.0f);
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ChestTop.png");
        textureFront = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ChestFront.png");
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ChestSide.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        models = new BlockModel[6];
        models[0] = new BlockModelSimpleBlock(this, registry, textureFront,
                textureTop, textureSide, textureSide, textureSide, textureSide,
                1.0f, 1.0f, 1.0f, 1.0f);
        models[1] = new BlockModelSimpleBlock(this, registry, textureTop,
                textureFront, textureSide, textureSide, textureSide,
                textureSide, 1.0f, 1.0f, 1.0f, 1.0f);
        models[2] = new BlockModelSimpleBlock(this, registry, textureTop,
                textureTop, textureFront, textureSide, textureSide, textureSide,
                1.0f, 1.0f, 1.0f, 1.0f);
        models[3] = new BlockModelSimpleBlock(this, registry, textureTop,
                textureTop, textureSide, textureFront, textureSide, textureSide,
                1.0f, 1.0f, 1.0f, 1.0f);
        models[4] = new BlockModelSimpleBlock(this, registry, textureTop,
                textureTop, textureSide, textureSide, textureFront, textureSide,
                1.0f, 1.0f, 1.0f, 1.0f);
        models[5] = new BlockModelSimpleBlock(this, registry, textureTop,
                textureTop, textureSide, textureSide, textureSide, textureFront,
                1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GraphicsSystem graphics, Shader shader,
            float r, float g, float b, float a) {
        models[4].render(graphics, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GraphicsSystem graphics,
            Shader shader, float r, float g, float b, float a) {
        models[4].renderInventory(graphics, shader);
    }

    @Override
    public String getName(ItemStack item) {
        return "Chest";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 1;
    }
}
