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
package org.tobi29.scapes.vanilla.basics.packet

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.packets.Packet
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.packets.PacketType
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityQuernClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityQuernServer

class PacketQuern : PacketAbstract, PacketServer {
    private lateinit var uuid: UUID

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                quern: EntityQuernClient) : super(type) {
        uuid = quern.getUUID()
    }

    constructor(registry: Registries,
                quern: EntityQuernClient) : this(
            Packet.make(registry, "vanilla.basics.packet.Quern"), quern)

    // TODO: @Throws(IOException::class)
    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
    }

    // TODO: @Throws(IOException::class)
    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.getLong(), stream.getLong())
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            mob.world.getEntity(uuid)?.let { quern ->
                if (quern is EntityQuernServer) {
                    if (mob in quern.viewers) {
                        val plugin = mob.world.plugins.plugin(
                                "VanillaBasics") as VanillaBasics
                        val materials = plugin.materials
                        quern.inventories().modify("Container") { inventory ->
                            val item = inventory.item(0)
                            if (item.material() == materials.cropDrop) {
                                item.setMaterial(materials.grain)
                                item.setAmount(item.amount() shl 2)
                            }
                        }
                    }
                }
            }
        }
    }
}
