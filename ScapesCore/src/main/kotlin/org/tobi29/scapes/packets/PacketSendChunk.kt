/*
 * Copyright 2012-2017 Tobi29
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

import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunkServer
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.io.tag.binary.readBinary
import org.tobi29.scapes.engine.utils.io.tag.binary.writeBinary
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketSendChunk : PacketAbstract, PacketClient {
    private var x = 0
    private var y = 0
    private lateinit var tag: TagMap

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                chunk: TerrainInfiniteChunkServer) : super(type) {
        x = chunk.pos.x
        y = chunk.pos.y
        tag = TagMap { chunk.write(this, true) }
    }

    constructor(registry: Registries,
                chunk: TerrainInfiniteChunkServer) : this(
            Packet.make(registry, "core.packet.SendChunk"), chunk)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putInt(x)
        stream.putInt(y)
        tag.writeBinary(stream)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        x = stream.getInt()
        y = stream.getInt()
        tag = readBinary(stream)
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
                    chunk.read(tag)
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
