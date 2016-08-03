/*
 * Copyright 2012-2016 Tobi29
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
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityForgeServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlockContainer;

public class BlockForge extends VanillaBlockContainer {
    private TerrainTexture textureOn, textureOff;
    private BlockModel modelOn, modelOff;

    public BlockForge(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.Forge");
    }

    @Override
    protected EntityContainerServer placeEntity(TerrainServer terrain, int x,
            int y, int z) {
        EntityForgeServer entity = new EntityForgeServer(terrain.world(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5));
        entity.onSpawn();
        terrain.world().addEntity(entity);
        return entity;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return "Shovel".equals(item.material().toolType(item)) ? 4 : 12;
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
        return Optional.of(terrain.data(x, y, z) > 0 ? textureOn : textureOff);
    }

    @Override
    public byte lightEmit(Terrain terrain, int x, int y, int z) {
        return terrain.data(x, y, z) > 0 ? (byte) 15 : 0;
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        if (data == 1) {
            modelOff.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f,
                    1.0f, 1.0f, 1.0f, lod);
        } else {
            modelOn.addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, 1.0f,
                    1.0f, 1.0f, 1.0f, lod);
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureOn = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ForgeOn.png");
        textureOff = registry.registerTexture(
                "VanillaBasics:image/terrain/device/ForgeOff.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelOn =
                new BlockModelSimpleBlock(this, registry, textureOn, textureOn,
                        textureOn, textureOn, textureOn, textureOn, 1.0f, 1.0f,
                        1.0f, 1.0f);
        modelOff = new BlockModelSimpleBlock(this, registry, textureOff,
                textureOff, textureOff, textureOff, textureOff, textureOff,
                1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader) {
        modelOff.render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader) {
        modelOff.renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return "Forge";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 1;
    }
}
