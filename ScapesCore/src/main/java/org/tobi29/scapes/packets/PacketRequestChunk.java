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
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkClient;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteClient;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketRequestChunk extends Packet
        implements PacketServer, PacketClient {
    private int x, y;

    public PacketRequestChunk() {
    }

    public PacketRequestChunk(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putInt(x);
        stream.putInt(y);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        x = stream.getInt();
        y = stream.getInt();
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        if (world == null) {
            return;
        }
        Terrain terrain = world.getTerrain();
        if (terrain instanceof TerrainInfiniteServer) {
            Optional<TerrainInfiniteChunkServer> chunk =
                    ((TerrainInfiniteServer) terrain).chunkNoLoad(x, y);
            if (chunk.isPresent()) {
                player.send(new PacketSendChunk(chunk.get()));
            }
        }
        player.send(new PacketRequestChunk(x, y));
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putInt(x);
        stream.putInt(y);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        x = stream.getInt();
        y = stream.getInt();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        Terrain terrain = client.world().terrain();
        if (terrain instanceof TerrainInfiniteClient) {
            TerrainInfiniteClient terrainInfinite =
                    (TerrainInfiniteClient) terrain;
            terrainInfinite.changeRequestedChunks(-1);
            terrainInfinite.chunkNoLoad(x, y)
                    .ifPresent(TerrainInfiniteChunkClient::resetRequest);
        }
    }
}