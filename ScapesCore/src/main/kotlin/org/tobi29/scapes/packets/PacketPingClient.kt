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
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.connection.RemoteClientConnection
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketPingClient : PacketPing {
    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                timestamp: Long) : super(type, timestamp)

    constructor(registry: GameRegistry,
                timestamp: Long) : this(
            Packet.make(registry, "core.packet.PingClient"), timestamp)

    override fun runServer(player: PlayerConnection) {
        player.send(PacketPingClient(type, timestamp))
    }

    override fun runClient(client: ClientConnection) {
        if (client is RemoteClientConnection) {
            client.updatePing(timestamp)
        }
    }
}
