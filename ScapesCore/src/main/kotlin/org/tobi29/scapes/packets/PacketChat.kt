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
import org.tobi29.scapes.engine.server.InvalidPacketDataException
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.server.extension.event.MessageEvent

class PacketChat : PacketAbstract, PacketBoth {
    private lateinit var text: String

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                text: String) : super(type) {
        this.text = text
    }

    constructor(registry: Registries,
                text: String) : this(
            Packet.make(registry, "core.packet.Chat"), text)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putString(text)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        text = stream.getString()
    }

    override fun runClient(client: ClientConnection) {
        client.game.chatHistory.addLine(text)
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putString(text)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        text = stream.getString(1 shl 10)
    }

    override fun runServer(player: PlayerConnection) {
        if (text.isEmpty() || text.length > 64) {
            throw InvalidPacketDataException("Invalid chat text length!")
        }
        if (text[0] == '/') {
            player.server.server.commandRegistry()[text.substring(
                    1), player].execute().forEach { output ->
                player.events.fire(
                        MessageEvent(player, MessageLevel.FEEDBACK_ERROR,
                                output.toString(), player))
            }
        } else {
            player.events.fire(
                    MessageEvent(player, MessageLevel.CHAT,
                            '<' + player.name() + "> " + text))
        }
    }
}
