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
import org.tobi29.scapes.engine.server.ConnectionEndException
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketDisconnectSelf : PacketAbstract, PacketBoth {
    val reason: String

    constructor(type: PacketType,
                reason: String) : super(type) {
        this.reason = reason
    }

    constructor(registry: Registries,
                reason: String) : this(
            Packet.make(registry, "core.packet.DisconnectSelf"), reason)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        throw ConnectionEndException(reason)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
    }

    override fun runClient(client: ClientConnection) {
        throw ConnectionEndException(reason)
    }

    override fun localClient() {
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        throw ConnectionEndException(reason)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
    }

    override fun runServer(player: PlayerConnection) {
        throw ConnectionEndException(reason)
    }

    override fun localServer() {
    }
}
