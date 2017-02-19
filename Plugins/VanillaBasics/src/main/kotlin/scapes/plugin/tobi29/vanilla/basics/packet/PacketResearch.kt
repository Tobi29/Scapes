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
import org.tobi29.scapes.engine.utils.io.tag.map
import org.tobi29.scapes.engine.utils.io.tag.mapMut
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.toBoolean
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketEntityMetaData
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.server.connection.PlayerConnection
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.entity.client.EntityResearchTableClient
import scapes.plugin.tobi29.vanilla.basics.entity.server.EntityResearchTableServer
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemResearch
import java.util.*

class PacketResearch : PacketAbstract, PacketServer {
    private lateinit var uuid: UUID

    constructor()

    constructor(researchTable: EntityResearchTableClient) {
        uuid = researchTable.getUUID()
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
    }

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
                                    mob.metaData("Vanilla").mapMut(
                                            "Research").mapMut(
                                            "Items")[identifier] = true
                                }
                            } else {
                                mob.metaData("Vanilla").mapMut(
                                        "Research").mapMut(
                                        "Items")[Integer.toHexString(
                                        material.itemID())] = true
                            }
                            plugin.researchRecipes.forEach { recipe ->
                                if (!(mob.metaData("Vanilla").map(
                                        "Research")?.map(
                                        "Finished")?.get(
                                        recipe.name)?.toBoolean() ?: false)) {
                                    if (!recipe.items.filter { requirement ->
                                        !(mob.metaData("Vanilla").map(
                                                "Research")?.map("Items")?.get(
                                                requirement)?.toBoolean() ?: false)
                                    }.any()) {
                                        mob.metaData("Vanilla").mapMut(
                                                "Research").mapMut(
                                                "Finished")[recipe.name] = true
                                        mob.world.send(PacketEntityMetaData(
                                                mob, "Vanilla"))
                                        player.send(
                                                PacketNotification("Research",
                                                        recipe.text))
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
