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

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkClient;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

public class PacketSendChunk extends Packet implements PacketClient {
    private int x, y;
    private TagStructure tag;

    public PacketSendChunk() {
    }

    public PacketSendChunk(TerrainInfiniteChunkServer chunk) {
        super(new Vector3d(chunk.getX() << 4, chunk.getY() << 4, 0), true);
        x = chunk.getX();
        y = chunk.getY();
        tag = chunk.save(true);
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(x);
        streamOut.writeInt(y);
        TagStructureBinary.write(tag, streamOut);
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        x = streamIn.readInt();
        y = streamIn.readInt();
        tag = new TagStructure();
        TagStructureBinary.read(tag, streamIn);
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        Terrain terrain = client.getWorld().getTerrain();
        if (terrain instanceof TerrainInfiniteClient) {
            TerrainInfiniteClient terrainInfinite =
                    (TerrainInfiniteClient) terrain;
            Optional<TerrainInfiniteChunkClient> chunk =
                    terrainInfinite.getChunkNoLoad(x, y);
            if (chunk.isPresent()) {
                TerrainInfiniteChunkClient chunk2 = chunk.get();
                chunk2.load(tag);
                chunk2.setLoaded();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        Optional<TerrainInfiniteChunkClient> geomRenderer =
                                terrainInfinite
                                        .getChunkNoLoad(chunk2.getX() + x,
                                                chunk2.getY() + y);
                        if (geomRenderer.isPresent()) {
                            geomRenderer.get().getRendererChunk()
                                    .setGeometryDirty();
                        }
                    }
                }
            }
        }
    }
}
