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
import org.tobi29.io.tag.map
import org.tobi29.io.tag.mapMut
import org.tobi29.io.tag.toBoolean
import org.tobi29.io.tag.toTag
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.entity.server.viewers
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.packets.*
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityResearchTableClient
import org.tobi29.scapes.vanilla.basics.entity.server.EntityResearchTableServer
import org.tobi29.scapes.vanilla.basics.material.ItemResearch
import org.tobi29.scapes.vanilla.basics.material.identifiers
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem
import org.tobi29.uuid.Uuid
import kotlin.collections.set

class PacketResearch : PacketAbstract,
        PacketServer {
    private lateinit var uuid: Uuid

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                researchTable: EntityResearchTableClient) : super(type) {
        uuid = researchTable.uuid
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
        uuid = Uuid(stream.getLong(), stream.getLong())
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            mob.world.getEntity(uuid)?.let { researchTable ->
                if (researchTable is EntityResearchTableServer) {
                    if (mob in researchTable.viewers) {
                        val plugin = mob.world.plugins.plugin<VanillaBasics>()
                        researchTable.inventories.modify(
                                "Container") { researchTableI ->
                            val item = researchTableI[0].kind<VanillaItem>()
                            val itemResearch = item.kind<ItemResearch>()
                            if (itemResearch != null) {
                                for (identifier in itemResearch.identifiers) {
                                    mob.metaData("Vanilla")
                                            .mapMut("Research")
                                            .mapMut("Items")[identifier] = true.toTag()
                                }
                            } else if (item != null) {
                                mob.metaData("Vanilla")
                                        .mapMut("Research")
                                        .mapMut("Items")[Integer.toHexString(
                                        item.type.id)] = true.toTag()
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
                                                "Finished")[recipe.name] = true.toTag()
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
