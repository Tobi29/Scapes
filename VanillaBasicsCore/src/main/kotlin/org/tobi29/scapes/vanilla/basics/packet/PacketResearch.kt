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

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.tag.map
import org.tobi29.scapes.engine.utils.tag.mapMut
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.toBoolean
import org.tobi29.scapes.packets.*
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityResearchTableClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityResearchTableServer
import org.tobi29.scapes.vanilla.basics.material.ItemResearch
import java.util.*

class PacketResearch : PacketAbstract, PacketServer {
    private lateinit var uuid: UUID

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                researchTable: EntityResearchTableClient) : super(type) {
        uuid = researchTable.getUUID()
    }

    constructor(registry: Registries,
                researchTable: EntityResearchTableClient) : this(
            Packet.make(registry, "vanilla.basics.packet.Research"),
            researchTable)

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.getLong(), stream.getLong())
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
                                        material.id)] = true
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
                                                player.server.plugins.registry,
                                                mob, "Vanilla"))
                                        player.send(
                                                PacketNotification(
                                                        player.server.plugins.registry,
                                                        "Research",
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
