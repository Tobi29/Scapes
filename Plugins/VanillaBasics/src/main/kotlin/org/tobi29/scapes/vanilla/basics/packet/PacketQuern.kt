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
package org.tobi29.scapes.vanilla.basics.packet

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityQuernClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityQuernServer
import java.io.IOException
import java.util.*

class PacketQuern : PacketAbstract, PacketServer {
    private lateinit var uuid: UUID

    constructor() {
    }

    constructor(quern: EntityQuernClient) {
        uuid = quern.getUUID()
    }

    @Throws(IOException::class)
    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
    }

    @Throws(IOException::class)
    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.long, stream.long)
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            mob.world.getEntity(uuid)?.let { quern ->
                if (quern is EntityQuernServer) {
                    if (quern.viewers().filter { quern === mob }.findAny().isPresent) {
                        val plugin = mob.world.plugins.plugin(
                                "VanillaBasics") as VanillaBasics
                        val materials = plugin.materials
                        quern.inventories().modify("Container") { inventory ->
                            val item = inventory.item(0)
                            if (item.material() === materials.cropDrop) {
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
