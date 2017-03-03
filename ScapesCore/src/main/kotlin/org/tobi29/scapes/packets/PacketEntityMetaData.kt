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

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.binary.readBinary
import org.tobi29.scapes.engine.utils.io.tag.binary.writeBinary
import org.tobi29.scapes.engine.utils.io.tag.toTag
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.server.connection.PlayerConnection
import java.io.IOException
import java.util.*

class PacketEntityMetaData : PacketAbstract, PacketClient {
    private lateinit var uuid: UUID
    lateinit var category: String
        private set
    lateinit var tag: TagMap
        private set

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                entity: EntityServer,
                category: String) : super(type, entity.getCurrentPos()) {
        uuid = entity.getUUID()
        this.category = category
        tag = entity.metaData(category).toTag()
    }

    constructor(registry: GameRegistry,
                entity: EntityServer,
                category: String) : this(
            Packet.make(registry, "core.packet.EntityMetaData"), entity,
            category)

    @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putString(category)
        tag.writeBinary(stream)
    }

    @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.long, stream.long)
        category = stream.string
        tag = readBinary(stream)
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { it.processPacket(this) }
    }
}
