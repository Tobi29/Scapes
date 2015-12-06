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
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.server.MobServer;
import org.tobi29.scapes.packets.PacketUpdateInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BlockType extends Material {
    public static final Collision STANDARD_COLLISION = new Collision();
    private static final AABB SELECTION = new AABB(0, 0, 0, 1, 1, 1);

    protected BlockType(GameRegistry registry, String nameID) {
        super(registry, nameID);
    }

    public short id() {
        return (short) id;
    }

    public void addPointerCollision(int data, Pool<PointerPane> pointerPanes,
            int x, int y, int z) {
        pointerPanes.push().set(SELECTION, Face.UP, x, y, z);
        pointerPanes.push().set(SELECTION, Face.DOWN, x, y, z);
        pointerPanes.push().set(SELECTION, Face.NORTH, x, y, z);
        pointerPanes.push().set(SELECTION, Face.EAST, x, y, z);
        pointerPanes.push().set(SELECTION, Face.SOUTH, x, y, z);
        pointerPanes.push().set(SELECTION, Face.WEST, x, y, z);
    }

    public boolean click(TerrainServer terrain, int x, int y, int z, Face face,
            MobPlayerServer player) {
        return false;
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item,
            TerrainServer terrain, int x, int y, int z, Face face) {
        Vector3 place = face.getDelta().plus(new Vector3i(x, y, z));
        terrain.queue(handler -> {
            if (handler.type(place.intX(), place.intY(), place.intZ())
                    .isReplaceable(handler, place.intX(), place.intY(),
                            place.intZ())) {
                List<AABBElement> aabbs =
                        collision(item.data(), place.intX(), place.intY(),
                                place.intZ());
                boolean flag = true;
                AABB coll = entity.aabb();
                for (AABBElement element : aabbs) {
                    if (coll.overlay(element.aabb) &&
                            element.collision.isSolid()) {
                        flag = false;
                    }
                }
                if (flag) {
                    handler.data(place.intX(), place.intY(), place.intZ(),
                            item.data());
                    if (place(handler, place.intX(), place.intY(), place.intZ(),
                            face, entity)) {
                        handler.type(place.intX(), place.intY(), place.intZ(),
                                this);
                        item.setAmount(item.amount() - 1);
                        handler.world().send(new PacketUpdateInventory(entity,
                                "Container"));
                        entity.connection().statistics()
                                .blockPlace(this, item.data());
                    }
                }
            }
        });
        return 0;
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item, MobServer hit) {
        return 0;
    }

    @Override
    public String toolType(ItemStack item) {
        return "Block";
    }

    public void addCollision(Pool<AABBElement> aabbs, Terrain terrain, int x,
            int y, int z) {
        aabbs.push().set(x, y, z, x + 1, y + 1, z + 1, STANDARD_COLLISION);
    }

    public List<AABBElement> collision(int data, int x, int y, int z) {
        List<AABBElement> aabbs = new ArrayList<>();
        aabbs.add(new AABBElement(new AABB(x, y, z, x + 1, y + 1, z + 1),
                STANDARD_COLLISION));
        return aabbs;
    }

    public boolean isReplaceable(Terrain terrain, int x, int y, int z) {
        return false;
    }

    public boolean place(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player) {
        return true;
    }

    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        return true;
    }

    public abstract double resistance(ItemStack item, int data);

    public List<ItemStack> drops(ItemStack item, int data) {
        return Collections.singletonList(new ItemStack(this, data));
    }

    public abstract String footStepSound(int data);

    public abstract String breakSound(ItemStack item, int data);

    public float particleColorR(Face face, TerrainClient terrain, int x, int y,
            int z) {
        return 1.0f;
    }

    public float particleColorG(Face face, TerrainClient terrain, int x, int y,
            int z) {
        return 1.0f;
    }

    public float particleColorB(Face face, TerrainClient terrain, int x, int y,
            int z) {
        return 1.0f;
    }

    public abstract Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z);

    public boolean isLiquid() {
        return false;
    }

    public boolean isSolid(Terrain terrain, int x, int y, int z) {
        return true;
    }

    public boolean isTransparent(Terrain terrain, int x, int y, int z) {
        return false;
    }

    public byte lightEmit(Terrain terrain, int x, int y, int z) {
        return 0;
    }

    public byte lightTrough(Terrain terrain, int x, int y, int z) {
        return -15;
    }

    public short connectStage(TerrainClient terrain, int x, int y, int z) {
        return 4;
    }

    public abstract void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha,
            int data, TerrainClient terrain, TerrainRenderInfo info, int x,
            int y, int z, float xx, float yy, float zz, boolean lod);

    public void update(TerrainServer.TerrainMutable terrain, int x, int y,
            int z) {
    }

    public boolean causesTileUpdate() {
        return false;
    }
}
