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
import org.tobi29.scapes.engine.server.InvalidPacketDataException
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.entity.client.MobPlayerClient
import org.tobi29.scapes.server.connection.PlayerConnection
import java.util.*

class PacketInteraction : PacketAbstract, PacketBoth {
    private lateinit var uuid: UUID
    private var side: Byte = 0
    private var data: Byte = 0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                side: Byte,
                data: Byte) : super(type) {
        this.side = side
        this.data = data
    }

    constructor(type: PacketType,
                uuid: UUID,
                side: Byte,
                data: Byte) : super(type) {
        this.uuid = uuid
        this.side = side
        this.data = data
    }

    constructor(registry: Registries,
                side: Byte,
                data: Byte) : this(
            Packet.make(registry, "core.packet.Interaction"), side, data)

    constructor(registry: Registries,
                uuid: UUID,
                side: Byte,
                data: Byte) : this(
            Packet.make(registry, "core.packet.Interaction"), uuid, side,
            data)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.put(side.toInt())
        stream.put(data.toInt())
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.long, stream.long)
        side = stream.get()
        data = stream.get()
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { entity ->
            if (entity is MobPlayerClient) {
                when (side) {
                    INVENTORY_SLOT_LEFT -> entity.inventorySelectLeft = data.toInt()
                    INVENTORY_SLOT_RIGHT -> entity.inventorySelectRight = data.toInt()
                }
            }
        }
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.put(side.toInt())
        stream.put(data.toInt())
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        side = stream.get()
        data = stream.get()
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            when (side) {
                INVENTORY_SLOT_LEFT -> {
                    if (data < 0 || data >= 10) {
                        throw InvalidPacketDataException(
                                "Invalid slot change data!")
                    }
                    mob.inventorySelectLeft = data.toInt()
                    mob.world.send(PacketInteraction(type, mob.getUUID(),
                            INVENTORY_SLOT_LEFT, data))
                }
                INVENTORY_SLOT_RIGHT -> {
                    if (data < 0 || data >= 10) {
                        throw InvalidPacketDataException(
                                "Invalid slot change data!")
                    }
                    mob.inventorySelectRight = data.toInt()
                    mob.world.send(PacketInteraction(type, mob.getUUID(),
                            INVENTORY_SLOT_RIGHT, data))
                }
                OPEN_INVENTORY -> {
                    mob.onOpenInventory()
                    player.send(
                            PacketOpenGui(player.server.plugins.registry, mob))
                }
                CLOSE_INVENTORY -> {
                    mob.onCloseInventory()
                    player.send(
                            PacketCloseGui(player.server.plugins.registry))
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
