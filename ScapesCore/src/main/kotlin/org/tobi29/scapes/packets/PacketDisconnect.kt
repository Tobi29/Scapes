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
import org.tobi29.scapes.client.states.GameStateServerDisconnect
import org.tobi29.scapes.engine.server.ConnectionEndException
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketDisconnect : PacketAbstract, PacketClient {
    private lateinit var reason: String
    private var time = 0.0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                reason: String,
                time: Double) : super(type) {
        this.reason = reason
        this.time = time
    }

    constructor(registry: Registries,
                reason: String,
                time: Double) : this(
            Packet.make(registry, "core.packet.Disconnect"), reason, time)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putString(reason)
        stream.putDouble(time)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        reason = stream.string
        time = stream.double
    }

    override fun runClient(client: ClientConnection) {
        val address = client.address()
        if (time >= 0) {
            client.game.engine.switchState(
                    GameStateServerDisconnect(reason, address,
                            client.game.engine, time))
        } else {
            client.game.engine.switchState(GameStateServerDisconnect(reason,
                    client.game.engine))
        }
        throw ConnectionEndException(reason)
    }
}
