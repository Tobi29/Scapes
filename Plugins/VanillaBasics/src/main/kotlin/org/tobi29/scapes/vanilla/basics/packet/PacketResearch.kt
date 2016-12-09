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
import org.tobi29.scapes.packets.PacketEntityMetaData
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityResearchTableClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityResearchTableServer
import org.tobi29.scapes.vanilla.basics.material.item.ItemResearch
import java.io.IOException
import java.util.*

class PacketResearch : PacketAbstract, PacketServer {
    private lateinit var uuid: UUID

    constructor() {
    }

    constructor(researchTable: EntityResearchTableClient) {
        uuid = researchTable.getUUID()
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
            mob.world.getEntity(uuid)?.let { researchTable ->
                if (researchTable is EntityResearchTableServer) {
                    if (mob in researchTable.viewers) {
                        val plugin = mob.world.plugins.plugin(
                                "VanillaBasics") as VanillaBasics
                        researchTable.inventories().modify(
                                "Container") { researchTableI ->
                            val item = researchTableI.item(0)
                            val material = item.material()
                            if (material is ItemResearch) {
                                for (identifier in material.identifiers(item)) {
                                    mob.metaData("Vanilla").structure(
                                            "Research").structure(
                                            "Items").setBoolean(identifier,
                                            true)
                                }
                            } else {
                                mob.metaData("Vanilla").structure(
                                        "Research").structure(
                                        "Items").setBoolean(Integer.toHexString(
                                        material.itemID()),
                                        true)
                            }
                            plugin.researchRecipes().forEach { recipe ->
                                if (!(mob.metaData("Vanilla").structure(
                                        "Research").structure(
                                        "Finished").getBoolean(
                                        recipe.name()) ?: false)) {
                                    if (!recipe.items().filter { requirement ->
                                        !(mob.metaData("Vanilla").structure(
                                                "Research").structure(
                                                "Items").getBoolean(
                                                requirement) ?: false)
                                    }.findAny().isPresent) {
                                        mob.metaData("Vanilla").structure(
                                                "Research").structure(
                                                "Finished").setBoolean(
                                                recipe.name(),
                                                true)
                                        mob.world.send(PacketEntityMetaData(
                                                mob,
                                                "Vanilla"))
                                        player.send(
                                                PacketNotification(
                                                        "Research",
                                                        recipe.text()))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
