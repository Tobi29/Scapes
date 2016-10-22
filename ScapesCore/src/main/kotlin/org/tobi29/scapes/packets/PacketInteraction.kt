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

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.server.InvalidPacketDataException
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.entity.client.MobPlayerClient
import org.tobi29.scapes.server.connection.PlayerConnection
import java.util.*

class PacketInteraction : PacketAbstract, PacketClient, PacketServer {
    private lateinit var uuid: UUID
    private var type: Byte = 0
    private var data: Byte = 0

    constructor() {
    }

    constructor(type: Byte, data: Byte = 0) {
        this.type = type
        this.data = data
    }

    constructor(uuid: UUID, type: Byte, data: Byte) {
        this.uuid = uuid
        this.type = type
        this.data = data
    }

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.put(type.toInt())
        stream.put(data.toInt())
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.long, stream.long)
        type = stream.get()
        data = stream.get()
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { entity ->
            if (entity is MobPlayerClient) {
                when (type) {
                    INVENTORY_SLOT_LEFT -> entity.inventorySelectLeft = data.toInt()
                    INVENTORY_SLOT_RIGHT -> entity.inventorySelectRight = data.toInt()
                }
            }
        }
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.put(type.toInt())
        stream.put(data.toInt())
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        type = stream.get()
        data = stream.get()
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            when (type) {
                INVENTORY_SLOT_LEFT -> {
                    if (data < 0 || data >= 10) {
                        throw InvalidPacketDataException(
                                "Invalid slot change data!")
                    }
                    mob.inventorySelectLeft = data.toInt()
                    mob.world.send(PacketInteraction(mob.getUUID(),
                            INVENTORY_SLOT_LEFT, data))
                }
                INVENTORY_SLOT_RIGHT -> {
                    if (data < 0 || data >= 10) {
                        throw InvalidPacketDataException(
                                "Invalid slot change data!")
                    }
                    mob.inventorySelectRight = data.toInt()
                    mob.world.send(PacketInteraction(mob.getUUID(),
                            INVENTORY_SLOT_RIGHT, data))
                }
                OPEN_INVENTORY -> player.send(PacketOpenGui(mob))
                CLOSE_INVENTORY -> {
                    mob.inventories().modify("Hold"
                    ) { inventory ->
                        inventory.item(0).take()?.let { mob.dropItem(it) }
                    }
                    player.send(PacketCloseGui())
                }
                else -> throw InvalidPacketDataException(
                        "Invalid interaction type!")
            }
        }
    }

    companion object {
        val INVENTORY_SLOT_LEFT: Byte = 0x00
        val INVENTORY_SLOT_RIGHT: Byte = 0x01
        val OPEN_INVENTORY: Byte = 0x10
        val CLOSE_INVENTORY: Byte = 0x11
    }
}
