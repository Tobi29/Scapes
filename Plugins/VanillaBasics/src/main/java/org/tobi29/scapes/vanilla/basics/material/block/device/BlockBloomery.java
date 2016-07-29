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
import org.tobi29.scapes.block.models.BlockModelComplex;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityBloomeryServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer;

import java.util.ArrayList;
import java.util.List;

public class BlockBloomery extends VanillaBlockContainer {
    private TerrainTexture textureSide, textureInside;
    private BlockModel model;

    public BlockBloomery(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Bloomery");
    }

    @Override
    protected EntityContainerServer placeEntity(TerrainServer terrain, int x,
            int y, int z) {
        EntityBloomeryServer entity = new EntityBloomeryServer(terrain.world(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5));
        entity.onSpawn();
        terrain.world().addEntity(entity);
        entity.updateBellows(terrain);
        return entity;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Pickaxe".equals(item.material().toolType(item)) ? 12 : -1;
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
    public boolean isTransparent(Terrain terrain, int x, int y, int z) {
        return true;
    }

    @Override
    public short connectStage(TerrainClient terrain, int x, int y, int z) {
        return -1;
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        model.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                1.0f, 1.0f, lod);
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        terrain.world().entities(x, y, z)
                .filter(entity -> entity instanceof EntityBloomeryServer)
                .forEach(entity -> ((EntityBloomeryServer) entity)
                        .updateBellows(terrain));
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/device/BloomerySide.png");
        textureInside = registry.registerTexture(
                "VanillaBasics:image/terrain/device/BloomeryInside.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        List<BlockModelComplex.Shape> shapes = new ArrayList<>();
        shapes.add(new BlockModelComplex.ShapeBox(textureInside, textureInside,
                null, null, null, null, -6.0f, -6.0f, -8.0f, 6.0f, 6.0f, -7.0f,
                1.0f, 1.0f, 1.0f, 1.0f));

        shapes.add(new BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -8.0f,
                -8.0f, -8.0f, 8.0f, -6.0f, -3.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(
                new BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, -8.0f, -6.0f, -8.0f,
                        -6.0f, 6.0f, -3.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -8.0f, 6.0f,
                -8.0f, 8.0f, 8.0f, -3.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(
                new BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, 6.0f, -6.0f, -8.0f,
                        8.0f, 6.0f, -3.0f, 1.0f, 1.0f, 1.0f, 1.0f));

        shapes.add(new BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -6.0f,
                -6.0f, -3.0f, 6.0f, -4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(
                new BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, -6.0f, -4.0f, -3.0f,
                        -4.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -6.0f, 4.0f,
                -3.0f, 6.0f, 6.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(
                new BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, 4.0f, -4.0f, -3.0f,
                        6.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f));

        shapes.add(new BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -4.0f,
                -4.0f, 1.0f, 4.0f, -2.0f, 8.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(
                new BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, -4.0f, -2.0f, 1.0f,
                        -2.0f, 2.0f, 8.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(textureSide, textureSide,
                textureSide, textureSide, textureSide, textureSide, -4.0f, 2.0f,
                1.0f, 4.0f, 4.0f, 8.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(
                new BlockModelComplex.ShapeBox(textureSide, textureSide, null,
                        textureSide, null, textureSide, 2.0f, -2.0f, 1.0f, 4.0f,
                        2.0f, 8.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        model = new BlockModelComplex(registry, shapes, 0.0625f);
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
        return "Bloomery";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 1;
    }
}
