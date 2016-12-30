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

package scapes.plugin.tobi29.vanilla.basics.packet

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.server.InvalidPacketDataException
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.server.connection.PlayerConnection
import scapes.plugin.tobi29.vanilla.basics.material.CraftingRecipe
import java.io.IOException

class PacketCrafting : PacketAbstract, PacketServer {
    private var id = 0

    constructor() {
    }

    constructor(id: Int) {
        this.id = id
    }

    @Throws(IOException::class)
    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putInt(id)
    }

    @Throws(IOException::class)
    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        id = stream.int
    }

    override fun runServer(player: PlayerConnection) {
        val recipe: CraftingRecipe
        try {
            recipe = CraftingRecipe[player.server.plugins.registry(), id]
        } catch (e: IllegalArgumentException) {
            throw InvalidPacketDataException(
                    "Invalid crafting recipe id: " + id)
        }

        player.mob { mob ->
            // TODO: Check if table nearby
            mob.inventories().modify("Container") { inventory ->
                val result = recipe.result()
                if (inventory.canAdd(result) >= result.amount()) {
                    recipe.takes(inventory)?.let { takes ->
                        takes.forEach { inventory.take(it) }
                        inventory.add(result)
                    }
                }
            }
        }
    }
}