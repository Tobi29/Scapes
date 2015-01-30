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

package org.tobi29.scapes.vanilla.basics.material.block.vegetation;

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
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockBush extends VanillaBlock {
    private static final AABB SELECTION =
            new AABB(0.15, 0.15, 0, 0.85, 0.85, 0.95);
    private TerrainTexture[] texturesInside;
    private TerrainTexture[] texturesOutside;
    private BlockModel[] models;

    public BlockBush(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Bush");
    }

    private static String getColor(int color) {
        switch (color) {
            case 0:
                return "Red";
            case 1:
                return "Orange";
            case 2:
                return "Yellows";
            case 3:
                return "Green";
            case 4:
                return "Blue";
            case 5:
                return "Purple";
            case 6:
                return "Black";
            case 7:
                return "Dark Gray";
            case 8:
                return "Light Gray";
            case 9:
                return "White";
            default:
                return "Unknown";
        }
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
    }

    @Override
    public List<AABBElement> getCollision(int data, int x, int y, int z) {
        return new ArrayList<>();
    }

    @Override
    public boolean isReplaceable(Terrain terrain, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        return terrain.getBlockType(x, y, z - 1).isSolid(terrain, x, y, z - 1);
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return 0;
    }

    @Override
    public List<ItemStack> getDrops(ItemStack item, int data) {
        return Collections.singletonList(new ItemStack(this, data));
    }

    @Override
    public String getFootStep(int data) {
        return "";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Foliage.ogg";
    }

    @Override
    public TerrainTexture getParticleTexture(Face face, TerrainClient terrain,
            int x, int y, int z) {
        return texturesOutside[terrain.getBlockData(x, y, z)];
    }

    @Override
    public boolean isSolid(Terrain terrain, int x, int y, int z) {
        return false;
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
            TerrainClient terrain, int x, int y, int z, float xx, float yy,
            float zz, boolean lod) {
        models[data]
                .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                        1.0f, 1.0f);
    }

    @Override
    public boolean needsLodUpdate(int data, TerrainClient terrain, int x, int y,
            int z) {
        return true;
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        if (!terrain.getBlockType(x, y, z - 1).isSolid(terrain, x, y, z - 1)) {
            terrain.getWorld().dropItems(
                    getDrops(new ItemStack(materials.air, (short) 0),
                            terrain.getBlockData(x, y, z)), x, y, z);
            terrain.setBlockTypeAndData(x, y, z, terrain.getWorld().getAir(),
                    (short) 0);
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        texturesInside = new TerrainTexture[1];
        texturesOutside = new TerrainTexture[texturesInside.length];
        texturesInside[0] = registry.registerTexture(
                "VanillaBasics:image/terrain/vegetation/bush/inside/Base.png");
        texturesOutside[0] = registry.registerTexture(
                "VanillaBasics:image/terrain/vegetation/bush/outside/Base.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        models = new BlockModel[texturesInside.length];
        for (int i = 0; i < models.length; i++) {
            List<BlockModelComplex.Shape> shapes = new ArrayList<>();
            BlockModelComplex.Shape shape =
                    new BlockModelComplex.ShapeBillboard(texturesInside[i],
                            -8.0f, -8.0f, -8.0f, 8.0f, 8.0f, 8.0f, 1.0f, 1.0f,
                            1.0f, 1.0f);
            shape.rotateZ(45.0f);
            shapes.add(shape);
            shape = new BlockModelComplex.ShapeBox(texturesOutside[i],
                    texturesOutside[i], texturesOutside[i], texturesOutside[i],
                    texturesOutside[i], texturesOutside[i], -8.0f, -8.0f, -8.0f,
                    8.0f, 8.0f, 8.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            shapes.add(shape);
            models[i] = new BlockModelComplex(registry, shapes, 0.0625f);
        }
    }

    @Override
    public void render(ItemStack item, GraphicsSystem graphics, Shader shader,
            float r, float g, float b, float a) {
        models[item.getData()].render(graphics, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GraphicsSystem graphics,
            Shader shader, float r, float g, float b, float a) {
        models[item.getData()].renderInventory(graphics, shader);
    }

    @Override
    public String getName(ItemStack item) {
        return getColor(item.getData() % 10) +
                (item.getData() / 10 == 0 ? " Rose" : " Flower");
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 64;
    }
}
