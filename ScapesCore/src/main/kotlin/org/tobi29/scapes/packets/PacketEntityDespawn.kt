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
import org.tobi29.scapes.entity.client.MobLivingClient
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.uuid.Uuid

class PacketEntityDespawn : PacketAbstract,
        PacketClient {
    private lateinit var uuid: Uuid
    private var dead = false

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                entity: EntityServer) : super(type) {
        uuid = entity.uuid
        if (entity is MobLivingServer) {
            dead = entity.isDead
        }
    }

    constructor(registry: Registries,
                entity: EntityServer) : this(
            Packet.make(registry, "core.packet.EntityDespawn"), entity)

    // TODO: @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putBoolean(dead)
    }

    // TODO: @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = Uuid(stream.getLong(), stream.getLong())
        dead = stream.getBoolean()
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { entity ->
            if (dead && entity is MobLivingClient) {
                entity.death()
            }
            entity.world.removeEntity(entity)
        }
    }
}
