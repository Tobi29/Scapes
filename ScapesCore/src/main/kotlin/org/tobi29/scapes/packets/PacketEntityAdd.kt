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

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.io.tag.binary.readBinary
import org.tobi29.scapes.engine.utils.io.tag.binary.writeBinary
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketEntityAdd : PacketAbstract, PacketClient {
    private lateinit var uuid: UUID
    private var id = 0
    private lateinit var tag: TagMap

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                entity: EntityServer) : super(type, entity.getCurrentPos()) {
        uuid = entity.getUUID()
        id = entity.type.id
        tag = TagMap { entity.write(this) }
    }

    constructor(registry: Registries,
                entity: EntityServer) : this(
            Packet.make(registry, "core.packet.EntityAdd"), entity)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putInt(id)
        tag.writeBinary(stream)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.getLong(), stream.getLong())
        id = stream.getInt()
        tag = readBinary(stream)
    }

    override fun runClient(client: ClientConnection) {
        client.mob { mob ->
            val entity = mob.world.getEntity(uuid)
            if (entity != null) {
                entity.read(tag)
            } else {
                val newEntity = EntityClient.make(id, mob.world)
                newEntity.setEntityID(uuid)
                newEntity.read(tag)
                mob.world.terrain.addEntity(newEntity)
            }
        }
    }
}
