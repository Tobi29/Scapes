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
package org.tobi29.scapes.packets

import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteClient
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketRequestChunk : PacketAbstract, PacketBoth {
    private var x = 0
    private var y = 0

    constructor()

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putInt(x)
        stream.putInt(y)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        x = stream.int
        y = stream.int
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            val terrain = mob.world.terrain
            if (terrain is TerrainInfiniteServer) {
                val chunk = terrain.chunkNoLoad(x, y)
                if (chunk != null && terrain.isBlockSendable(mob,
                        chunk.posBlock.x, chunk.posBlock.y, chunk.posBlock.z,
                        true)) {
                    player.send(PacketSendChunk(chunk))
                    return@mob
                }
            }
            player.send(PacketRequestChunk(x, y))
        }
    }

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putInt(x)
        stream.putInt(y)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        x = stream.int
        y = stream.int
    }

    override fun runClient(client: ClientConnection) {
        client.mob { mob ->
            val terrain = mob.world.terrain
            if (terrain is TerrainInfiniteClient) {
                terrain.changeRequestedChunks(-1)
                val chunk = terrain.chunkNoLoad(x, y)
                chunk?.resetRequest()
            }
        }
    }
}
