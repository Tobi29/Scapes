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

import org.tobi29.scapes.block.AABBElement;
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
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BlockTorch extends VanillaBlock {
    private static final AABB[] SELECTION =
            {new AABB(0.4375, 0.4375, 0, 0.5625, 0.5625, 0.625), null,
                    new AABB(0.4375, 0.4375, 0, 0.5625, 1.0, 0.625),
                    new AABB(0.0, 0.4375, 0, 0.5625, 0.5625, 0.625),
                    new AABB(0.4375, 0.0, 0, 0.5625, 0.5625, 0.625),
                    new AABB(0.4375, 0.4375, 0, 1.0, 0.5625, 0.625)};
    private TerrainTexture textureTop, textureSide;
    private BlockModel[] models;

    public BlockTorch(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Torch");
    }

    @Override
    public void addPointerCollision(int data, Pool<PointerPane> pointerPanes,
            int x, int y, int z) {
        pointerPanes.push().set(SELECTION[data], Face.UP, x, y, z);
        pointerPanes.push().set(SELECTION[data], Face.DOWN, x, y, z);
        pointerPanes.push().set(SELECTION[data], Face.NORTH, x, y, z);
        pointerPanes.push().set(SELECTION[data], Face.EAST, x, y, z);
        pointerPanes.push().set(SELECTION[data], Face.SOUTH, x, y, z);
        pointerPanes.push().set(SELECTION[data], Face.WEST, x, y, z);
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
    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        Vector3 ground =
                face.getOpposite().getDelta().plus(new Vector3i(x, y, z));
        boolean flag = terrain.getBlockType(ground.intX(), ground.intY(),
                ground.intZ()).isSolid(terrain, ground.intX(), ground.intY(),
                ground.intZ()) &&
                !terrain.getBlockType(ground.intX(), ground.intY(),
                        ground.intZ())
                        .isTransparent(terrain, ground.intX(), ground.intY(),
                                ground.intZ());
        if (flag) {
            terrain.setBlockData(x, y, z, face.getData());
        }
        return flag;
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return 0;
    }

    @Override
    public List<ItemStack> getDrops(ItemStack item, int data) {
        return Collections.singletonList(new ItemStack(this, (short) 0));
    }

    @Override
    public String getFootStep(int data) {
        return "";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "";
    }

    @Override
    public Optional<TerrainTexture> getParticleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(textureSide);
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
    public byte lightEmit(Terrain terrain, int x, int y, int z) {
        return 15;
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
                        1.0f, 1.0f);
    }

    @Override
    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
        Vector3 ground =
                Face.get(terrain.getBlockData(x, y, z)).getOpposite().getDelta()
                        .plus(new Vector3i(x, y, z));
        if (!terrain.getBlockType(ground.intX(), ground.intY(), ground.intZ())
                .isSolid(terrain, ground.intX(), ground.intY(),
                        ground.intZ()) ||
                terrain.getBlockType(ground.intX(), ground.intY(),
                        ground.intZ())
                        .isTransparent(terrain, ground.intX(), ground.intY(),
                                ground.intZ())) {
            terrain.getWorld().dropItems(
                    getDrops(new ItemStack(materials.air, (short) 0),
                            terrain.getBlockData(x, y, z)), x, y, z);
            terrain.setBlockTypeAndData(x, y, z, terrain.getWorld().getAir(),
                    (short) 0);
        }
    }

    @Override
    public boolean isTool(ItemStack item) {
        return true;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureTop = registry.registerTexture(
                "VanillaBasics:image/terrain/TorchTop.png");
        textureSide = registry.registerTexture(
                "VanillaBasics:image/terrain/TorchSide.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        models = new BlockModel[6];
        List<BlockModelComplex.Shape> shapes = new ArrayList<>();
        BlockModelComplex.Shape shape =
                new BlockModelComplex.ShapeBox(textureTop, textureSide,
                        textureSide, textureSide, textureSide, textureSide, -1,
                        -1, -8, 1, 1, 2, 1.0f, 1.0f, 1.0f, 1.0f);
        shapes.add(shape);
        models[0] = new BlockModelComplex(registry, shapes, 0.0625f);
        models[1] = models[0];
        shapes = new ArrayList<>();
        shape = new BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1, -1, -8,
                1, 1, 2, 1.0f, 1.0f, 1.0f, 1.0f);
        shape.translate(0.0f, 6.0f, 0.0f);
        shape.rotateX(30.0f);
        shapes.add(shape);
        models[2] = new BlockModelComplex(registry, shapes, 0.0625f);
        shapes = new ArrayList<>();
        shape = new BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1, -1, -8,
                1, 1, 2, 1.0f, 1.0f, 1.0f, 1.0f);
        shape.translate(-6.0f, 0, 0.0f);
        shape.rotateY(-30.0f);
        shapes.add(shape);
        models[3] = new BlockModelComplex(registry, shapes, 0.0625f);
        shapes = new ArrayList<>();
        shape = new BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1, -1, -8,
                1, 1, 2, 1.0f, 1.0f, 1.0f, 1.0f);
        shape.translate(0.0f, -6.0f, 0.0f);
        shape.rotateX(-30.0f);
        shapes.add(shape);
        models[4] = new BlockModelComplex(registry, shapes, 0.0625f);
        shapes = new ArrayList<>();
        shape = new BlockModelComplex.ShapeBox(textureTop, textureSide,
                textureSide, textureSide, textureSide, textureSide, -1, -1, -8,
                1, 1, 2, 1.0f, 1.0f, 1.0f, 1.0f);
        shape.translate(6.0f, 0.0f, 0.0f);
        shape.rotateY(30.0f);
        shapes.add(shape);
        models[5] = new BlockModelComplex(registry, shapes, 0.0625f);
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
    public float getPlayerLight(ItemStack item) {
        return 0.7f;
    }

    @Override
    public String getName(ItemStack item) {
        return "Torch";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 32;
    }
}
