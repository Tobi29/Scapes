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
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.TreeType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;
import org.tobi29.scapes.vanilla.basics.material.item.ItemFuel;

import java.util.Collections;
import java.util.List;

public class BlockLog extends VanillaBlock implements ItemFuel {
    private final GameRegistry.Registry<TreeType> treeRegistry;
    private TerrainTexture[] texturesTop;
    private TerrainTexture[] texturesSide;
    private BlockModel[] models;

    public BlockLog(VanillaMaterial materials,
            GameRegistry.Registry<TreeType> treeRegistry) {
        super(materials, "vanilla.basics.block.Log");
        this.treeRegistry = treeRegistry;
    }

    @Override
    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        if ("Axe".equals(item.material().toolType(item))) {
            destroy(terrain, new Vector3i(x, y, z), terrain.data(x, y, z), 512,
                    player, z);
        }
        return true;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Axe".equals(item.material().toolType(item)) ? 10 :
                "Saw".equals(item.material().toolType(item)) ? 2 : -1;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        if ("Saw".equals(item.material().toolType(item))) {
            return Collections
                    .singletonList(new ItemStack(materials.wood, data, 2));
        }
        return Collections.emptyList();
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Wood.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "Axe".equals(item.material().toolType(item)) ?
                "VanillaBasics:sound/blocks/Axe.ogg" :
                "VanillaBasics:sound/blocks/Saw.ogg";
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        if (face == Face.UP || face == Face.DOWN) {
            return Optional.of(texturesTop[terrain.data(x, y, z)]);
        }
        return Optional.of(texturesSide[terrain.data(x, y, z)]);
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
    public void registerTextures(TerrainTextureRegistry registry) {
        List<TreeType> types = treeRegistry.values();
        texturesTop = new TerrainTexture[types.size()];
        texturesSide = new TerrainTexture[types.size()];
        for (int i = 0; i < types.size(); i++) {
            String texture = types.get(i).texture();
            texturesTop[i] = registry.registerTexture(texture + "/LogTop.png");
            texturesSide[i] =
                    registry.registerTexture(texture + "/LogSide.png");
        }
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        List<TreeType> types = treeRegistry.values();
        models = new BlockModel[types.size()];
        for (int i = 0; i < types.size(); i++) {
            models[i] =
                    new BlockModelSimpleBlock(this, registry, texturesTop[i],
                            texturesTop[i], texturesSide[i], texturesSide[i],
                            texturesSide[i], texturesSide[i], 1.0f, 1.0f, 1.0f,
                            1.0f);
        }
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        models[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        models[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return treeRegistry.get(item.data()).name();
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }

    private void destroy(TerrainServer.TerrainMutable terrain, Vector3 pos,
            int data, int length, MobPlayerServer player, int minZ) {
        BlockType type = terrain.type(pos.intX(), pos.intY(), pos.intZ());
        int d = terrain.data(pos.intX(), pos.intY(), pos.intZ());
        if (type == materials.leaves && d == data) {
            type.destroy(terrain, pos.intX(), pos.intY(), pos.intZ(), Face.NONE,
                    player, new ItemStack(materials.air, (short) 0));
        }
        if (type != this || d != data) {
            return;
        }
        terrain.world()
                .dropItem(new ItemStack(this, data), pos.intX(), pos.intY(),
                        pos.intZ());
        terrain.typeData(pos.intX(), pos.intY(), pos.intZ(), materials.air,
                (short) 0);
        if (length-- > 0) {
            if (pos.intZ() > minZ) {
                destroy(terrain, pos.plus(new Vector3i(-1, -1, -1)), data,
                        length, player, minZ);
                destroy(terrain, pos.plus(new Vector3i(0, -1, -1)), data,
                        length, player, minZ);
                destroy(terrain, pos.plus(new Vector3i(1, -1, -1)), data,
                        length, player, minZ);
                destroy(terrain, pos.plus(new Vector3i(-1, 0, -1)), data,
                        length, player, minZ);
                destroy(terrain, pos.plus(new Vector3i(0, 0, -1)), data, length,
                        player, minZ);
                destroy(terrain, pos.plus(new Vector3i(1, 0, -1)), data, length,
                        player, minZ);
                destroy(terrain, pos.plus(new Vector3i(-1, 1, -1)), data,
                        length, player, minZ);
                destroy(terrain, pos.plus(new Vector3i(0, 1, -1)), data, length,
                        player, minZ);
                destroy(terrain, pos.plus(new Vector3i(1, 1, -1)), data, length,
                        player, minZ);
            }
            destroy(terrain, pos.plus(new Vector3i(-1, -1, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(0, -1, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(1, -1, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(-1, 0, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(0, 0, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(1, 0, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(-1, 1, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(0, 1, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(1, 1, 0)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(-1, -1, 1)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(0, -1, 1)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(1, -1, 1)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(-1, 0, 1)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(0, 0, 1)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(1, 0, 1)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(-1, 1, 1)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(0, 1, 1)), data, length,
                    player, minZ);
            destroy(terrain, pos.plus(new Vector3i(1, 1, 1)), data, length,
                    player, minZ);
        }
    }

    @Override
    public float fuelTemperature(ItemStack item) {
        return 0.2f;
    }

    @Override
    public float fuelTime(ItemStack item) {
        return 100.0f;
    }

    @Override
    public int fuelTier(ItemStack item) {
        return 10;
    }
}
