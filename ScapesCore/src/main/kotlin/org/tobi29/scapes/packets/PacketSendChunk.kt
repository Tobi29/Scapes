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

import mu.KLogging
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkServer
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketSendChunk : PacketAbstract, PacketClient {
    private var x = 0
    private var y = 0
    private lateinit var tag: TagStructure

    constructor()

    constructor(chunk: TerrainInfiniteChunkServer) {
        x = chunk.pos.x
        y = chunk.pos.y
        tag = chunk.save(true)
    }

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putInt(x)
        stream.putInt(y)
        TagStructureBinary.write(stream, tag)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        x = stream.int
        y = stream.int
        tag = TagStructure()
        TagStructureBinary.read(stream, tag)
    }

    override fun localClient() {
        tag = tag.copy()
    }

    override fun runClient(client: ClientConnection) {
        // TODO: Do we want that in the update thread?
        client.mob { mob ->
            val terrain = mob.world.terrain
            if (terrain is TerrainInfiniteClient) {
                terrain.changeRequestedChunks(-1)
                val chunk = terrain.chunkNoLoad(x, y)
                if (chunk != null) {
                    if (chunk.isLoaded) {
                        logger.warn { "Chunk received twice: $x/$y" }
                    }
                    chunk.load(tag)
                    chunk.setLoaded()
                    chunk.resetRequest()
                    for (x in -1..1) {
                        for (y in -1..1) {
                            val geomRenderer = terrain.chunkNoLoad(
                                    chunk.pos.x + x,
                                    chunk.pos.y + y)
                            if (geomRenderer != null) {
                                geomRenderer.rendererChunk().setGeometryDirty()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object : KLogging()
}
