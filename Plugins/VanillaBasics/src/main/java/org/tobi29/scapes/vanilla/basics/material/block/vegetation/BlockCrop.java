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

import java8.util.Optional;
import org.tobi29.scapes.block.*;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelComplex;
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
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.CropType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BlockCrop extends VanillaBlock {
    private static final AABB SELECTION =
            new AABB(0.15, 0.15, 0, 0.85, 0.85, 0.95);
    private final GameRegistry.Registry<CropType> cropRegistry;
    private TerrainTexture[] textures;
    private BlockModel[] models;

    public BlockCrop(VanillaMaterial materials,
            GameRegistry.Registry<CropType> cropRegistry) {
        super(materials, "vanilla.basics.block.Crop");
        this.cropRegistry = cropRegistry;
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
    public List<AABBElement> collision(int data, int x, int y, int z) {
        return new ArrayList<>();
    }

    @Override
    public boolean isReplaceable(Terrain terrain, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        return false;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return 0;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        short dropData = (short) (data / 8);
        if (data % 8 == 7) {
            Random random = ThreadLocalRandom.current();
            return Arrays.asList(new ItemStack(materials.cropDrop, dropData),
                    new ItemStack(materials.seed, dropData,
                            random.nextInt(2) + 1));
        }
        return Collections.emptyList();
    }

    @Override
    public String footStepSound(int data) {
        return "";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Foliage.ogg";
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textures[terrain.data(x, y, z)]);
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
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        models[data]
                .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                        1.0f, 1.0f, lod);
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        if (terrain.type(x, y, z - 1) != materials.farmland) {
            terrain.typeData(x, y, z, terrain.world().air(), (short) 0);
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<CropType> types = cropRegistry.values();
        textures = new TerrainTexture[(types.size() << 3)];
        int i = 0;
        for (CropType type : types) {
            for (int j = 1; j < 9; j++) {
                textures[i++] = registry.registerTexture(
                        type.texture() + "/Crop" + j + ".png");
            }
        }
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        models = new BlockModel[textures.length];
        for (int i = 0; i < models.length; i++) {
            List<BlockModelComplex.Shape> shapes = new ArrayList<>();
            BlockModelComplex.Shape shape =
                    new BlockModelComplex.ShapeBillboard(textures[i], -8.0f,
                            -8.0f, -8.0f, 8.0f, 8.0f, 8.0f, 1.0f, 1.0f, 1.0f,
                            1.0f);
            shape.rotateZ(45.0f);
            shapes.add(shape);
            models[i] = new BlockModelComplex(registry, shapes, 0.0625f);
        }
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
    }

    @Override
    public String name(ItemStack item) {
        return cropRegistry.get(item.data()).name();
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 64;
    }
}
