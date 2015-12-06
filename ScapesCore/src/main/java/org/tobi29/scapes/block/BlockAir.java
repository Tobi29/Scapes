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

package org.tobi29.scapes.block;

import java8.util.Optional;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.server.MobServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockAir extends BlockType {
    protected BlockAir(GameRegistry registry) {
        super(registry, "core.block.Air");
    }

    @Override
    public void addPointerCollision(int data, Pool<PointerPane> pointerPanes,
            int x, int y, int z) {
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item,
            TerrainServer terrain, int x, int y, int z, Face face) {
        return 0.1;
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item, MobServer hit) {
        return 2.0;
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
    public double resistance(ItemStack item, int data) {
        return -1;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        return Collections.emptyList();
    }

    @Override
    public String footStepSound(int data) {
        return "";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "";
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.empty();
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
        return terrain.isBlockLoaded(x, y, z) ? (short) -1 : 100;
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
    }

    @Override
    public int itemID() {
        return 0;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
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
        return "";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return Integer.MAX_VALUE;
    }
}
