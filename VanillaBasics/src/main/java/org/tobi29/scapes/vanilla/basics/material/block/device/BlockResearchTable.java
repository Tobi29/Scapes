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
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityResearchTableServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.Optional;

public class BlockResearchTable extends VanillaBlock {
    private TerrainTexture textureTop, textureSide1, textureSide2,
            textureBottom;
    private BlockModel model;

    public BlockResearchTable(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.ResearchTable");
    }

    @Override
    public boolean click(TerrainServer terrain, int x, int y, int z, Face face,
            MobPlayerServer player) {
        terrain.getWorld().getEntities(x, y, z).stream()
                .filter(entity -> entity instanceof EntityResearchTableServer)
                .forEach(entity -> player
                        .openGui((EntityContainerServer) entity));
        return true;
    }

    @Override
    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        EntityServer entity = new EntityResearchTableServer(terrain.getWorld(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5));
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
        return "VanillaBasics:sound/blocks/Axe.ogg";
    }

    @Override
    public Optional<TerrainTexture> getParticleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textureSide1);
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
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
                "VanillaBasics:image/terrain/device/ResearchTableTop.png");
        textureSide1 = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ResearchTableSide1.png");
        textureSide2 = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ResearchTableSide2.png");
        textureBottom = registry.registerTexture(
                "VanillaBasics:image/terrain/tree/spruce/Planks.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        model = new BlockModelSimpleBlock(this, registry, textureTop,
                textureBottom, textureSide1, textureSide1, textureSide2,
                textureSide2, 1.0f, 1.0f, 1.0f, 1.0f);
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
        return "Research Table";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 1;
    }
}
