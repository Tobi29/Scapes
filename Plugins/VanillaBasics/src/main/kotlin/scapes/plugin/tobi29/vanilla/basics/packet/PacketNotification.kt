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

package scapes.plugin.tobi29.vanilla.basics.packet

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.gui.GuiInGameMessage
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketNotification : PacketAbstract, PacketClient {
    private lateinit var title: String
    private lateinit var text: String

    constructor()

    constructor(title: String, text: String) {
        this.title = title
        this.text = text
    }

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putString(title)
        stream.putString(text)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        title = stream.string
        text = stream.string
    }

    override fun runClient(client: ClientConnection) {
        client.mob {
            it.openGui(GuiInGameMessage(client.game, title, text,
                    client.game.engine.guiStyle))
        }
    }
}
