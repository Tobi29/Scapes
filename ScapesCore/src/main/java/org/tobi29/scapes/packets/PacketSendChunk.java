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
package org.tobi29.scapes.packets;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkClient;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketSendChunk extends Packet implements PacketClient {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PacketSendChunk.class);
    private int x, y;
    private TagStructure tag;

    public PacketSendChunk() {
    }

    public PacketSendChunk(TerrainInfiniteChunkServer chunk) {
        super(new Vector3d(chunk.blockX(), chunk.blockY(), 0), true);
        x = chunk.x();
        y = chunk.y();
        tag = chunk.save(true);
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putInt(x);
        stream.putInt(y);
        TagStructureBinary.write(tag, stream);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        x = stream.getInt();
        y = stream.getInt();
        tag = new TagStructure();
        TagStructureBinary.read(tag, stream);
    }

    @Override
    public void localClient() {
        tag = tag.copy();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        Terrain terrain = client.world().terrain();
        if (terrain instanceof TerrainInfiniteClient) {
            TerrainInfiniteClient terrainInfinite =
                    (TerrainInfiniteClient) terrain;
            Optional<TerrainInfiniteChunkClient> chunk =
                    terrainInfinite.chunkNoLoad(x, y);
            if (chunk.isPresent()) {
                TerrainInfiniteChunkClient chunk2 = chunk.get();
                if (chunk2.isLoaded()) {
                    LOGGER.warn("Chunk received twice: {}/{}", x, y);
                }
                chunk2.load(tag);
                chunk2.setLoaded();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        Optional<TerrainInfiniteChunkClient> geomRenderer =
                                terrainInfinite.chunkNoLoad(chunk2.x() + x,
                                        chunk2.y() + y);
                        if (geomRenderer.isPresent()) {
                            geomRenderer.get().rendererChunk()
                                    .setGeometryDirty();
                        }
                    }
                }
            }
        }
    }
}
