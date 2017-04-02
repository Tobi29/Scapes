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
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.client.MobileEntityClient
import org.tobi29.scapes.server.connection.PlayerConnection
import java.util.*

class PacketMobMoveRelative : PacketAbstract, PacketBoth {
    private lateinit var uuid: UUID
    private var x: Byte = 0
    private var y: Byte = 0
    private var z: Byte = 0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                uuid: UUID,
                pos: Vector3d?,
                x: Byte,
                y: Byte,
                z: Byte) : super(type, pos, 32.0, false, false) {
        this.uuid = uuid
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(registry: Registries,
                uuid: UUID,
                pos: Vector3d?,
                x: Byte,
                y: Byte,
                z: Byte) : this(
            Packet.make(registry, "core.packet.MobMoveRelative"), uuid,
            pos, x, y, z)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.put(x)
        stream.put(y)
        stream.put(z)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.getLong(), stream.getLong())
        x = stream.get()
        y = stream.get()
        z = stream.get()
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { entity ->
            if (entity is MobileEntityClient) {
                entity.positionReceiver.receiveMoveRelative(x, y, z)
            }
        }
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.put(x)
        stream.put(y)
        stream.put(z)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        x = stream.get()
        y = stream.get()
        z = stream.get()
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { it.positionReceiver.receiveMoveRelative(x, y, z) }
    }
}
