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
package scapes.plugin.tobi29.vanilla.basics.packet

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.server.connection.PlayerConnection
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.entity.client.EntityAnvilClient
import scapes.plugin.tobi29.vanilla.basics.entity.server.EntityAnvilServer
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemHeatable
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemIngot
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemOreChunk
import scapes.plugin.tobi29.vanilla.basics.util.createTool
import java.io.IOException
import java.util.*

class PacketAnvil : PacketAbstract, PacketServer {
    private lateinit var uuid: UUID
    private var id = 0

    constructor()

    constructor(anvil: EntityAnvilClient,
                id: Int) {
        uuid = anvil.getUUID()
        this.id = id
    }

    @Throws(IOException::class)
    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putInt(id)
    }

    @Throws(IOException::class)
    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.long, stream.long)
        id = stream.int
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            mob.world.getEntity(uuid)?.let { anvil ->
                if (anvil is EntityAnvilServer) {
                    if (mob in anvil.viewers) {
                        val plugin = mob.world.plugins.plugin(
                                "VanillaBasics") as VanillaBasics
                        anvil.inventories().modify("Container") { anvilI ->
                            if ("Hammer" != anvilI.item(1).material().toolType(
                                    anvilI.item(1))) {
                                return@modify
                            }
                            mob.world.playSound(
                                    "VanillaBasics:sound/blocks/Metal.ogg",
                                    anvil)
                            val ingredient = anvilI.item(0)
                            val type = ingredient.material()
                            if (type is ItemIngot) {
                                val meltingPoint = (type as ItemHeatable).meltingPoint(
                                        ingredient)
                                val temperature = type.temperature(ingredient)
                                if (temperature >= meltingPoint || temperature < meltingPoint * 0.7f) {
                                    return@modify
                                }
                                if (id == 0) {
                                    ingredient.setData(1)
                                } else {
                                    if (ingredient.data() == 0) {
                                        return@modify
                                    }
                                    ingredient.setData(0)
                                    createTool(plugin, ingredient, id)
                                }
                            } else if (type is ItemOreChunk) {
                                if (id == 0 && ingredient.data() == 8) {
                                    val meltingPoint = (type as ItemHeatable).meltingPoint(
                                            ingredient)
                                    val temperature = type.temperature(
                                            ingredient)
                                    if (temperature >= meltingPoint || temperature < meltingPoint * 0.7f) {
                                        return@modify
                                    }
                                    ingredient.setData(9)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
