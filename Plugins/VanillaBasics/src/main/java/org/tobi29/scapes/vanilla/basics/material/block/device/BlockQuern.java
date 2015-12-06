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

import java8.util.Optional;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityQuernServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.Collections;
import java.util.List;

public class BlockQuern extends VanillaBlock {
    private TerrainTexture textureTop, textureSide, textureBottom;
    private BlockModel model;

    public BlockQuern(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Quern");
    }

    @Override
    public boolean click(TerrainServer terrain, int x, int y, int z, Face face,
            MobPlayerServer player) {
        Streams.of(terrain.world().entities(x, y, z))
                .filter(entity -> entity instanceof EntityQuernServer).forEach(
                entity -> player.openGui((EntityContainerServer) entity));
        return true;
    }

    @Override
    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        EntityServer entity = new EntityQuernServer(terrain.world(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5));
        entity.onSpawn();
        terrain.world().addEntity(entity);
        return true;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Pickaxe".equals(item.material().toolType(item)) ? 12 : -1;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        if ("Pickaxe".equals(item.material().toolType(item))) {
            return Collections.singletonList(new ItemStack(this, (short) 0));
        }
        return Collections.emptyList();
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Stone.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Stone.ogg";
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textureSide);
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        model.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                1.0f, 1.0f, lod);
    }

    @Override
    public boolean causesTileUpdate() {
        return true;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/stone/raw/Granite.png");
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/device/Quern.png");
        textureBottom = registry.registerTexture(
                "VanillaBasics:image/terrain/stone/raw/Basalt.png",
                "VanillaBasics:image/terrain/stone/overlay/Cobble.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        model = new BlockModelSimpleBlock(this, registry, textureTop,
                textureBottom, textureSide, textureSide, textureSide,
                textureSide, 1.0f, 1.0f, 1.0f, 1.0f);
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
        return "Quern";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 1;
    }
}
