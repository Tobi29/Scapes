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

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.server.connection.PlayerConnection
import java.io.IOException
import java.util.*

class PacketEntityMetaData : PacketAbstract, PacketClient {
    private lateinit var uuid: UUID
    private lateinit var category: String
    private lateinit var tag: TagStructure

    constructor()

    constructor(entity: EntityServer,
                category: String) : super(
            entity.getCurrentPos()) {
        uuid = entity.getUUID()
        this.category = category
        tag = entity.metaData(category)
    }

    @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putString(category)
        TagStructureBinary.write(stream, tag)
    }

    @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.long, stream.long)
        category = stream.string
        tag = TagStructure()
        TagStructureBinary.read(stream, tag)
    }

    override fun localClient() {
        tag = tag.copy()
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { it.processPacket(this) }
    }

    fun category(): String {
        return category
    }

    fun tagStructure(): TagStructure {
        return tag
    }
}
