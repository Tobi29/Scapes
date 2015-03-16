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

package org.tobi29.scapes.chunk.terrain.infinite;

import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.packets.PacketRequestChunk;

import java.util.Optional;

public class TerrainInfiniteChunkClient extends TerrainInfiniteChunk {
    private final Optional<TerrainInfiniteChunkClient> optional =
            Optional.of(this);
    private final TerrainInfiniteClient terrain;
    private final TerrainInfiniteRendererChunk rendererChunk;
    private boolean requested;

    public TerrainInfiniteChunkClient(Vector2i pos,
            TerrainInfiniteClient terrain, int zSize,
            TerrainInfiniteRenderer renderer) {
        super(pos, terrain, terrain.getWorld(), zSize);
        this.terrain = terrain;
        rendererChunk = new TerrainInfiniteRendererChunk(this, renderer);
    }

    public Optional<TerrainInfiniteChunkClient> getOptional() {
        return optional;
    }

    public void updateClient() {
        if (state.id < State.LOADED.id) {
            TerrainInfiniteClient terrainClient = terrain;
            if (!requested && terrainClient.getRequestedChunks() < 2) {
                requested = true;
                terrainClient.changeRequestedChunks(1);
                terrain.getWorld().getConnection()
                        .send(new PacketRequestChunk(pos.intX(), pos.intY()));
            }
        }
    }

    public void disposeClient() {
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void setLoaded() {
        state = State.LOADED;
    }

    @Override
    public void update(int x, int y, int z, boolean updateTile) {
        terrain.getTerrainRenderer()
                .blockChange(x + posBlock.intX(), y + posBlock.intY(), z);
        if (state.id >= State.LOADED.id) {
            terrain.getLighting()
                    .updateLight(x + posBlock.intX(), y + posBlock.intY(), z);
        }
    }

    @Override
    public void setSunLight(int x, int y, int z, int light) {
        super.setSunLight(x, y, z, light);
        terrain.getTerrainRenderer()
                .blockChange(x + posBlock.intX(), y + posBlock.intY(), z);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int light) {
        super.setBlockLight(x, y, z, light);
        terrain.getTerrainRenderer()
                .blockChange(x + posBlock.intX(), y + posBlock.intY(), z);
    }

    public TerrainInfiniteRendererChunk getRendererChunk() {
        return rendererChunk;
    }

    public void resetRequest() {
        requested = false;
    }

    public void load(TagStructure tagStructure) {
        bID.load(tagStructure.getList("BlockID"));
        bData.load(tagStructure.getList("BlockData"));
        bLight.load(tagStructure.getList("BlockLight"));
        initHeightMap();
        for (TagStructure tag : tagStructure.getList("Entities")) {
            EntityClient entity =
                    EntityClient.make(tag.getInteger("ID"), terrain.getWorld());
            entity.read(tag.getStructure("Data"));
            terrain.getWorld().addEntity(entity, tag.getInteger("EntityID"));
        }
        metaData = tagStructure.getStructure("MetaData");
        initHeightMap();
    }
}
