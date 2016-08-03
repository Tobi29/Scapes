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
import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
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
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.ArrayList;
import java.util.List;

public class BlockFlower extends VanillaBlock {
    private static final AABB SELECTION =
            new AABB(0.15, 0.15, 0, 0.85, 0.85, 0.95);
    private TerrainTexture[] textures;
    private BlockModel[] models;
    private ItemModel[] modelsItem;

    public BlockFlower(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Flower");
    }

    private static String color(int color) {
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
        if (!terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1)) {
            terrain.world().dropItems(
                    drops(new ItemStack(materials.air, (short) 0),
                            terrain.data(x, y, z)), x, y, z);
            terrain.typeData(x, y, z, terrain.world().air(), (short) 0);
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textures = new TerrainTexture[20];
        textures[0] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseRed.png");
        textures[1] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseOrange.png");
        textures[2] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseYellow.png");
        textures[3] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseGreen.png");
        textures[4] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseBlue.png");
        textures[5] = registry.registerTexture(
                "VanillaBasics:image/terrain/RosePurple.png");
        textures[6] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseBlack.png");
        textures[7] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseDarkGray.png");
        textures[8] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseLightGray.png");
        textures[9] = registry.registerTexture(
                "VanillaBasics:image/terrain/RoseWhite.png");
        textures[10] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerRed.png");
        textures[11] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerOrange.png");
        textures[12] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerYellow.png");
        textures[13] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerGreen.png");
        textures[14] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerBlue.png");
        textures[15] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerPurple.png");
        textures[16] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerBlack.png");
        textures[17] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerDarkGray.png");
        textures[18] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerLightGray.png");
        textures[19] = registry.registerTexture(
                "VanillaBasics:image/terrain/FlowerWhite.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        models = new BlockModel[textures.length];
        modelsItem = new ItemModel[textures.length];
        for (int i = 0; i < textures.length; i++) {
            List<BlockModelComplex.Shape> shapes = new ArrayList<>();
            BlockModelComplex.Shape shape =
                    new BlockModelComplex.ShapeBillboard(textures[i], -8.0f,
                            -8.0f, -8.0f, 8.0f, 8.0f, 8.0f, 1.0f, 1.0f, 1.0f,
                            1.0f);
            shape.rotateZ(45.0f);
            shapes.add(shape);
            models[i] = new BlockModelComplex(registry, shapes, 0.0625f);
            modelsItem[i] =
                    new ItemModelSimple(textures[i], 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader) {
        modelsItem[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader) {
        modelsItem[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return color(item.data() % 10) +
                (item.data() / 10 == 0 ? " Rose" : " Flower");
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 64;
    }
}
