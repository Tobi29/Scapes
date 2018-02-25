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
import org.tobi29.io.ReadableByteStream
import org.tobi29.io.WritableByteStream
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.uuid.Uuid

class PacketRequestEntity : PacketAbstract,
        PacketServer {
    private lateinit var uuid: Uuid

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                uuid: Uuid) : super(type) {
        this.uuid = uuid
    }

    constructor(registry: Registries,
                uuid: Uuid) : this(
            Packet.make(registry, "core.packet.RequestEntity"), uuid)

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        uuid = Uuid(stream.getLong(), stream.getLong())
    }

    override fun runServer(player: PlayerConnection) {
        player.getEntity(uuid) { entity ->
            player.send(PacketEntityAdd(entity.world.plugins.registry, entity))
        }
    }
}
