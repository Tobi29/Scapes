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
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.math.ceilToInt
import org.tobi29.scapes.entity.client.EntityContainerClient
import org.tobi29.scapes.entity.server.EntityContainerServer
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketInventoryInteraction : PacketAbstract, PacketServer {
    private lateinit var uuid: UUID
    private var slot = 0
    private lateinit var id: String
    private var side = 0.toByte()

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                chest: EntityContainerClient,
                side: Byte,
                id: String,
                slot: Int) : super(type) {
        uuid = chest.uuid
        this.side = side
        this.id = id
        this.slot = slot
    }

    constructor(registry: Registries,
                chest: EntityContainerClient,
                side: Byte,
                id: String,
                slot: Int) : this(
            Packet.make(registry, "core.packet.InventoryInteraction"),
            chest, side, id, slot)

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.put(side)
        stream.putString(id)
        stream.putInt(slot)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.getLong(), stream.getLong())
        side = stream.get()
        id = stream.getString()
        slot = stream.getInt()
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            val world = mob.world
            world.getEntity(uuid) { entity ->
                if (entity is EntityContainerServer) {
                    if (mob in entity.viewers) {
                        synchronized(entity) {
                            entity.inventories().modify(id) { chestI ->
                                mob.inventories().modify(
                                        "Hold") { playerI ->
                                    val hold = playerI.item(0)
                                    val item = chestI.item(slot)
                                    when (side) {
                                        LEFT -> if (hold.isEmpty) {
                                            chestI.item(slot).take()?.let {
                                                hold.stack(it)
                                            }
                                        } else {
                                            if (item.stack(hold) == 0) {
                                                val swap = item.take()
                                                item.stack(hold)
                                                swap?.let { hold.stack(it) }
                                            }
                                        }
                                        RIGHT -> if (hold.isEmpty) {
                                            item.take(
                                                    (item.amount() / 2.0).ceilToInt())?.let {
                                                hold.stack(it)
                                            }
                                        } else {
                                            hold.take(1)?.let {
                                                item.stack(it)
                                            }
                                        }
                                    }
                                    Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val LEFT: Byte = 0
        val RIGHT: Byte = 1
    }
}
