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
import org.tobi29.scapes.block.models.BlockModelComplex;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityBellowsServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlockBellows extends VanillaBlock {
    private TerrainTexture textureFrame, textureInside;
    private BlockModel model;

    public BlockBellows(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Bellows");
    }

    @Override
    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        terrain.setBlockData(x, y, z, face.getData());
        EntityServer entity = new EntityBellowsServer(terrain.getWorld(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5), face);
        entity.onSpawn();
        terrain.getWorld().addEntity(entity);
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
        return "Axe".equals(item.getMaterial().getToolType(item)) ?
                "VanillaBasics:sound/blocks/Axe.ogg" :
                "VanillaBasics:sound/blocks/Saw.ogg";
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
        return -1;
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
    }

    @Override
    public boolean causesTileUpdate() {
        return true;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureFrame = registry.registerTexture(
                "VanillaBasics:image/terrain/tree/oak/Planks.png");
        textureInside = registry.registerTexture(
                "VanillaBasics:image/terrain/tree/birch/Planks.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        List<BlockModelComplex.Shape> shapes = new ArrayList<>();
        shapes.add(new BlockModelComplex.ShapeBox(textureFrame, textureFrame,
                textureFrame, textureFrame, textureFrame, textureFrame, -7.0f,
                -7.0f, -8.0f, 7.0f, 7.0f, -6.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(textureInside, textureInside,
                textureInside, textureInside, textureInside, textureInside,
                -6.0f, -6.0f, -6.0f, 6.0f, 6.0f, -1.0f, 1.0f, 1.0f, 1.0f,
                1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(textureFrame, textureFrame,
                textureFrame, textureFrame, textureFrame, textureFrame, -7.0f,
                -7.0f, -1.0f, 7.0f, 7.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(textureInside, textureInside,
                textureInside, textureInside, textureInside, textureInside,
                -6.0f, -6.0f, 1.0f, 6.0f, 6.0f, 6.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(textureFrame, textureFrame,
                textureFrame, textureFrame, textureFrame, textureFrame, -7.0f,
                -7.0f, 6.0f, 7.0f, 7.0f, 8.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        model = new BlockModelComplex(registry, shapes, 0.0625f);
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
        return "Bellows";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 1;
    }
}
