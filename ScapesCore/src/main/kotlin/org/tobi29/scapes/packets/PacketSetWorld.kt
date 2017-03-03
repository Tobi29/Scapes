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

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.chunk.EnvironmentClient
import org.tobi29.scapes.chunk.EnvironmentServer
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.binary.readBinary
import org.tobi29.scapes.engine.utils.io.tag.binary.writeBinary
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.server.connection.PlayerConnection
import java.util.*

class PacketSetWorld : PacketAbstract, PacketClient {
    private lateinit var tag: TagMap
    private var seed: Long = 0
    private lateinit var uuid: UUID
    private var environment = 0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                world: WorldServer,
                player: MobPlayerServer) : super(type) {
        tag = TagMap { player.write(this) }
        seed = world.seed
        uuid = player.getUUID()
        environment = world.registry.getAsymSupplier<Any, Any, Any, Any>("Core",
                "Environment").id(world.environment)
    }

    constructor(registry: GameRegistry,
                world: WorldServer,
                player: MobPlayerServer) : this(
            Packet.make(registry, "core.packet.SetWorld"), world, player)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        tag.writeBinary(stream)
        stream.putLong(seed)
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putInt(environment)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        tag = readBinary(stream)
        seed = stream.long
        uuid = UUID(stream.long, stream.long)
        environment = stream.int
    }

    override fun runClient(client: ClientConnection) {
        val environmentRegistry = client.plugins.registry().getAsymSupplier<WorldServer, EnvironmentServer, WorldClient, EnvironmentClient>(
                "Core", "Environment")
        client.changeWorld(
                WorldClient(client,
                        Cam(0.01f, client.loadingDistance.toFloat()),
                        seed, {
                    TerrainInfiniteClient(it,
                            client.loadingDistance shr 4, 512,
                            client.game.engine.taskExecutor, it.air)
                }, { environmentRegistry.get2(environment)(it) }, tag,
                        uuid))
    }
}
