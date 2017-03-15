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
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.server.connection.RemotePlayerConnection

class PacketPingServer : PacketPing {
    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                timestamp: Long) : super(type, timestamp)

    constructor(registry: Registries,
                timestamp: Long) : this(
            Packet.make(registry, "core.packet.PingServer"), timestamp)

    override fun runServer(player: PlayerConnection) {
        if (player is RemotePlayerConnection) {
            player.updatePing(timestamp)
        }
    }

    override fun runClient(client: ClientConnection) {
        client.send(PacketPingServer(type, timestamp))
    }
}
