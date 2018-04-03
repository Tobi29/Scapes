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

import org.tobi29.io.ReadableByteStream
import org.tobi29.io.WritableByteStream
import org.tobi29.scapes.block.*
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.entity.server.viewers
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.packets.Packet
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.packets.PacketType
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAnvilClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityAnvilServer
import org.tobi29.scapes.vanilla.basics.material.alloy
import org.tobi29.scapes.vanilla.basics.material.item.ItemIngot
import org.tobi29.scapes.vanilla.basics.material.item.ItemOreChunk
import org.tobi29.scapes.vanilla.basics.material.meltingPoint
import org.tobi29.scapes.vanilla.basics.material.temperature
import org.tobi29.scapes.vanilla.basics.util.createTool
import org.tobi29.server.InvalidPacketDataException
import org.tobi29.uuid.Uuid

class PacketAnvil : PacketAbstract,
        PacketServer {
    private lateinit var uuid: Uuid
    private var id = 0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                anvil: EntityAnvilClient,
                id: Int) : super(type) {
        uuid = anvil.uuid
        this.id = id
    }

    constructor(registry: Registries,
                anvil: EntityAnvilClient,
                id: Int) : this(
            Packet.make(registry, "vanilla.basics.packet.Anvil"), anvil,
            id)

    // TODO: @Throws(IOException::class)
    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putInt(id)
    }

    // TODO: @Throws(IOException::class)
    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        uuid = Uuid(stream.getLong(), stream.getLong())
        id = stream.getInt()
    }

    override fun runServer(player: PlayerConnection) {
        if (id !in 0..7) throw InvalidPacketDataException(
                "Invalid anvil action id: $id")
        player.mob { mob ->
            mob.world.getEntity(uuid)?.let { anvil ->
                if (anvil is EntityAnvilServer) {
                    if (mob in anvil.viewers) {
                        val plugin = mob.world.plugins.plugin<VanillaBasics>()
                        anvil.inventories.modify("Container") { anvilI ->
                            if ("Hammer" != anvilI[1].kind<ItemTypeTool>()?.toolType()) {
                                return@modify
                            }
                            mob.world.playSound(
                                    "VanillaBasics:sound/blocks/Metal.ogg",
                                    anvil)
                            anvilI[0].kind<ItemIngot>()?.let { ingot ->
                                val meltingPoint = ingot.meltingPoint
                                val temperature = ingot.temperature
                                if (temperature >= meltingPoint || temperature < meltingPoint * 0.7f) {
                                    return@modify
                                }
                                if (id == 0) {
                                    anvilI[0] = ingot.copy(data = 1)
                                } else {
                                    if (ingot.data == 0) {
                                        return@modify
                                    }
                                    anvilI[0] = createTool(plugin, id,
                                            ingot.alloy, ingot.temperature)
                                }
                            }
                            anvilI[0].kind<ItemOreChunk>()?.let { ore ->
                                if (id == 0 && ore.data == 8) {
                                    val meltingPoint = ore.type.meltingPoint(
                                            ore)
                                    val temperature = ore.temperature
                                    if (temperature >= meltingPoint || temperature < meltingPoint * 0.7f) {
                                        return@modify
                                    }
                                    anvilI[0] = ore.copy(data = 9)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
