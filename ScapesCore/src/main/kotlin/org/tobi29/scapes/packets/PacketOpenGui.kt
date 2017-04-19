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
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.entity.client.EntityContainerClient
import org.tobi29.scapes.entity.server.EntityContainerServer
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketOpenGui : PacketAbstract, PacketClient {
    private lateinit var uuid: UUID

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                entity: EntityContainerServer) : super(type) {
        uuid = entity.getUUID()
    }

    constructor(registry: Registries,
                entity: EntityContainerServer) : this(
            Packet.make(registry, "core.packet.OpenGui"), entity)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.getLong(), stream.getLong())
    }

    override fun runClient(client: ClientConnection) {
        client.mob { mob ->
            client.getEntity(uuid) { entity ->
                if (entity is EntityContainerClient) {
                    entity.gui(mob)?.let { mob.openGui(it) }
                }
            }
        }
    }
}
