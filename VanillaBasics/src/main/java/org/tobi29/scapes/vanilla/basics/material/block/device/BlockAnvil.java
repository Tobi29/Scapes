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

import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelComplex;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobFlyingBlockServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityAnvilServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer;

import java.util.ArrayList;
import java.util.List;

public class BlockAnvil extends VanillaBlockContainer {
    private static final AABB SELECTION = new AABB(0.125, 0, 0, 0.875, 1, 1);
    private TerrainTexture texture;
    private BlockModel model;

    public BlockAnvil(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Anvil");
    }

    @Override
    public void addPointerCollision(int data, Pool<PointerPane> pointerPanes,
            int x, int y, int z) {
        pointerPanes.push().set(SELECTION, Face.UP, x, y, z);
        pointerPanes.push().set(SELECTION, Face.DOWN, x, y, z);
        pointerPanes.push().set(SELECTION, Face.NORTH, x, y, z);
        pointerPanes.push().set(SELECTION, Face.EAST, x, y, z);
        pointerPanes.push().set(SELECTION, Face.SOUTH, x, y, z);
        pointerPanes.push().set(SELECTION, Face.WEST, x, y, z);
    }

    @Override
    public void addCollision(Pool<AABBElement> aabbs, Terrain terrain, int x,
            int y, int z) {
        aabbs.push().set(x + 0.125, y, z, x + 0.875, y + 1, z + 1,
                STANDARD_COLLISION);
    }

    @Override
    public List<AABBElement> getCollision(int data, int x, int y, int z) {
        List<AABBElement> aabbs = new ArrayList<>();
        aabbs.add(new AABBElement(
                new AABB(x + 0.125, y, z, x + 0.875, y + 1, z + 1),
                STANDARD_COLLISION));
        return aabbs;
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return "Pickaxe".equals(item.getMaterial().getToolType(item)) ? 8 : -1;
    }

    @Override
    public String getFootStep(int data) {
        return "VanillaBasics:sound/footsteps/Stone.ogg";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Metal.ogg";
    }

    @Override
    public TerrainTexture getParticleTexture(Face face, TerrainClient terrain,
            int x, int y, int z) {
        return texture;
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
            TerrainClient terrain, int x, int y, int z, float xx, float yy,
            float zz, boolean lod) {
        model.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                1.0f, 1.0f);
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        if (!terrain.getBlockType(x, y, z - 1).isSolid(terrain, x, y, z - 1)) {
            EntityServer entity = new MobFlyingBlockServer(terrain.getWorld(),
                    new Vector3d(x + 0.5, y + 0.5, z + 0.5), Vector3d.ZERO,
                    this, terrain.getBlockData(x, y, z));
            entity.onSpawn();
            terrain.getWorld().addEntity(entity);
            terrain.setBlockTypeAndData(x, y, z, terrain.getWorld().getAir(),
                    (short) 0);
        }
    }

    @Override
    protected EntityContainerServer placeEntity(TerrainServer terrain, int x,
            int y, int z) {
        EntityAnvilServer entity = new EntityAnvilServer(terrain.getWorld(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5));
        entity.onSpawn();
        terrain.getWorld().addEntity(entity);
        return entity;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/device/Anvil.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        List<BlockModelComplex.Shape> shapes = new ArrayList<>();
        shapes.add(new BlockModelComplex.ShapeBox(texture, texture, texture,
                texture, texture, texture, -5.0f, -6.0f, -8.0f, 5.0f, 6.0f,
                -3.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(null, null, texture, texture,
                texture, texture, -4.0f, -4.0f, -3.0f, 4.0f, 4.0f, 4.0f, 1.0f,
                1.0f, 1.0f, 1.0f));
        shapes.add(new BlockModelComplex.ShapeBox(texture, texture, texture,
                texture, texture, texture, -6.0f, -8.0f, 4.0f, 6.0f, 8.0f, 8.0f,
                1.0f, 1.0f, 1.0f, 1.0f));
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
        return "Anvil";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 1;
    }
}
