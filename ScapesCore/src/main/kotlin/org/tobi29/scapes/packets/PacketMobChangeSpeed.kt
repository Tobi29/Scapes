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
import org.tobi29.math.vector.Vector3d
import org.tobi29.io.ReadableByteStream
import org.tobi29.io.WritableByteStream
import org.tobi29.scapes.entity.client.MobileEntityClient
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.stdex.math.clamp
import org.tobi29.uuid.Uuid

class PacketMobChangeSpeed : PacketAbstract,
        PacketBoth {
    private lateinit var uuid: Uuid
    private var x: Short = 0
    private var y: Short = 0
    private var z: Short = 0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                uuid: Uuid,
                pos: Vector3d?,
                x: Double,
                y: Double,
                z: Double) : super(type, pos, 0.0, false, false) {
        this.uuid = uuid
        this.x = clamp(x * 100.0, Short.MIN_VALUE.toDouble(),
                Short.MAX_VALUE.toDouble()).toShort()
        this.y = clamp(y * 100.0, Short.MIN_VALUE.toDouble(),
                Short.MAX_VALUE.toDouble()).toShort()
        this.z = clamp(z * 100.0, Short.MIN_VALUE.toDouble(),
                Short.MAX_VALUE.toDouble()).toShort()
    }

    constructor(registry: Registries,
                uuid: Uuid,
                pos: Vector3d?,
                x: Double,
                y: Double,
                z: Double) : this(
            Packet.make(registry, "core.packet.MobChangeSpeed"), uuid,
            pos, x, y, z)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putShort(x)
        stream.putShort(y)
        stream.putShort(z)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = Uuid(stream.getLong(), stream.getLong())
        x = stream.getShort()
        y = stream.getShort()
        z = stream.getShort()
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { entity ->
            if (entity is MobileEntityClient) {
                entity.positionReceiver.receiveSpeed(x / 100.0, y / 100.0,
                        z / 100.0)
            }
        }
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putShort(x)
        stream.putShort(y)
        stream.putShort(z)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        x = stream.getShort()
        y = stream.getShort()
        z = stream.getShort()
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            mob.positionReceiver.receiveSpeed(x / 100.0, y / 100.0, z / 100.0)
        }
    }
}
