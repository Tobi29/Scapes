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
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.io.ReadableByteStream
import org.tobi29.io.WritableByteStream
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.server.viewers
import org.tobi29.scapes.inventory.isEmpty
import org.tobi29.scapes.inventory.split
import org.tobi29.scapes.inventory.stack
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.uuid.Uuid

class PacketInventoryInteraction : PacketAbstract,
        PacketServer {
    private lateinit var uuid: Uuid
    private var slot = 0
    private lateinit var id: String
    private var side = 0.toByte()

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                chest: EntityClient,
                side: Byte,
                id: String,
                slot: Int) : super(type) {
        uuid = chest.uuid
        this.side = side
        this.id = id
        this.slot = slot
    }

    constructor(registry: Registries,
                chest: EntityClient,
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
        uuid = Uuid(stream.getLong(), stream.getLong())
        side = stream.get()
        id = stream.getString()
        slot = stream.getInt()
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            val world = mob.world
            world.getEntity(uuid) { entity ->
                if (mob == entity || mob in entity.viewers) {
                    entity.inventories.modify(id) { chestI ->
                        mob.inventories.modify(
                                "Hold") { playerI ->
                            when (side) {
                                LEFT -> if (playerI[0].isEmpty()) {
                                    playerI[0] = chestI[slot]
                                    chestI[slot] = null
                                } else {
                                    val (stack, remaining) = chestI[slot].stack(
                                            playerI[0])
                                    if (stack == chestI[slot] && remaining == playerI[0]) {
                                        val swap = playerI[0]
                                        playerI[0] = chestI[slot]
                                        chestI[slot] = swap
                                    } else {
                                        chestI[slot] = stack
                                        playerI[0] = remaining
                                    }
                                }
                                RIGHT -> if (playerI[0].isEmpty()) {
                                    val (stack, remaining) = chestI[slot].split(
                                            0.5)
                                    playerI[0] = stack
                                    chestI[slot] = remaining
                                } else {
                                    val (stack, remaining) = playerI[0].split(1)
                                    val (stacked, overflow) = chestI[slot].stack(
                                            stack)
                                    if (overflow.isEmpty()) {
                                        playerI[0] = remaining
                                        chestI[slot] = stacked
                                    }
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
