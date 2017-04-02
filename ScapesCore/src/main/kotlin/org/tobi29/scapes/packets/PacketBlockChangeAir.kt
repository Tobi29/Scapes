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
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketBlockChangeAir : PacketBlockChange {
    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                x: Int,
                y: Int,
                z: Int) : super(type, x, y, z, 0, 0)

    constructor(registry: Registries,
                x: Int,
                y: Int,
                z: Int) : this(
            Packet.make(registry, "core.packet.BlockChangeAir"), x, y, z)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putInt(x)
        stream.putInt(y)
        stream.putInt(z)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        x = stream.getInt()
        y = stream.getInt()
        z = stream.getInt()
    }
}
