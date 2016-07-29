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

package org.tobi29.scapes.vanilla.basics.material.block.rock;

import java8.util.Optional;
import org.tobi29.scapes.block.*;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelComplex;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
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
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockStoneRock extends VanillaBlock {
    private static final AABB[] SELECTION =
            {new AABB(0.375, 0.375, 0, 0.625, 0.625, 0.0625),
                    new AABB(0.375, 0.25, 0, 0.625, 0.75, 0.0625),
                    new AABB(0.4375, 0.375, 0, 0.5625, 0.625, 0.0625),
                    new AABB(0.4375, 0.25, 0, 0.5625, 0.75, 0.0625),
                    new AABB(0.375, 0.375, 0, 0.625, 0.625, 0.125),
                    new AABB(0.375, 0.25, 0, 0.625, 0.75, 0.125),
                    new AABB(0.4375, 0.375, 0, 0.5625, 0.625, 0.125),
                    new AABB(0.4375, 0.25, 0, 0.5625, 0.75, 0.125)};
    private static final int[] PERM = new int[512];

    static {
        Random random = new Random(0);
        int v;
        for (int i = 0; i < 256; i++) {
            v = random.nextInt(256);
            PERM[i] = v;
            PERM[i + 256] = v;
        }
    }

    private final GameRegistry.Registry<StoneType> stoneRegistry;
    private TerrainTexture[] textures;
    private TerrainTexture[] texturesItem;
    private BlockModel[][] models;
    private ItemModel[] modelsItem;

    public BlockStoneRock(VanillaMaterial materials,
            GameRegistry.Registry<StoneType> stoneRegistry) {
        super(materials, "vanilla.basics.block.StoneRock");
        this.stoneRegistry = stoneRegistry;
    }

    @Override
    public void addPointerCollision(int data, Pool<PointerPane> pointerPanes,
            int x, int y, int z) {
        int i = PERM[x & 255 + PERM[y & 255 + PERM[z & 255]]] % 8;
        pointerPanes.push().set(SELECTION[i], Face.UP, x, y, z);
        pointerPanes.push().set(SELECTION[i], Face.DOWN, x, y, z);
        pointerPanes.push().set(SELECTION[i], Face.NORTH, x, y, z);
        pointerPanes.push().set(SELECTION[i], Face.EAST, x, y, z);
        pointerPanes.push().set(SELECTION[i], Face.SOUTH, x, y, z);
        pointerPanes.push().set(SELECTION[i], Face.WEST, x, y, z);
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
        return terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1);
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return 0;
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
        int i = PERM[x & 255 + PERM[y & 255 + PERM[z & 255]]] % 8;
        models[data][i]
                .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f, 1.0f,
                        1.0f, 1.0f, lod);
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        if (!terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1)) {
            terrain.world().dropItems(
                    drops(new ItemStack(materials.air, (short) 0),
                            terrain.data(x, y, z)), x, y, z);
            terrain.typeData(x, y, z, terrain.world().air(), (short) 0);
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<StoneType> types = stoneRegistry.values();
        textures = new TerrainTexture[types.size()];
        texturesItem = new TerrainTexture[types.size()];
        int i = 0;
        for (StoneType type : types) {
            textures[i] =
                    registry.registerTexture(type.textureRoot() + "/raw/" +
                            type.texture() + ".png");
            texturesItem[i++] = registry.registerTexture(
                    "VanillaBasics:image/terrain/stone/rock/" +
                            type.texture() + ".png");
        }
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        models = new BlockModel[textures.length][SELECTION.length];
        modelsItem = new ItemModel[textures.length];
        for (int i = 0; i < textures.length; i++) {
            for (int j = 0; j < SELECTION.length; j++) {
                List<BlockModelComplex.Shape> shapes = new ArrayList<>();
                shapes.add(
                        new BlockModelComplex.ShapeBox(textures[i], textures[i],
                                textures[i], textures[i], textures[i],
                                textures[i], (float) SELECTION[j].minX - 0.5f,
                                (float) SELECTION[j].minY - 0.5f,
                                (float) SELECTION[j].minZ - 0.5f,
                                (float) SELECTION[j].maxX - 0.5f,
                                (float) SELECTION[j].maxY - 0.5f,
                                (float) SELECTION[j].maxZ - 0.5f, 1.0f, 1.0f,
                                1.0f, 1.0f));
                models[i][j] = new BlockModelComplex(registry, shapes, 1.0f);
            }
            modelsItem[i] =
                    new ItemModelSimple(texturesItem[i], 1.0f, 1.0f, 1.0f,
                            1.0f);
        }
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        modelsItem[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        modelsItem[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return materials.stoneRaw.name(item) + " Rock";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 128;
    }
}
