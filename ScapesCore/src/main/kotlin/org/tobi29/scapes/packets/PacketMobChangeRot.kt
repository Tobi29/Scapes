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
package org.tobi29.scapes.packets

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.client.MobileEntityClient
import org.tobi29.scapes.server.connection.PlayerConnection
import java.util.*

class PacketMobChangeRot : PacketAbstract, PacketBoth {
    private lateinit var uuid: UUID
    private var x = 0.0f
    private var y = 0.0f
    private var z = 0.0f

    constructor() {
    }

    constructor(uuid: UUID, pos: Vector3d?, x: Float, y: Float,
                z: Float) : super(pos, 0.0, false, false) {
        this.uuid = uuid
        this.x = x
        this.y = y
        this.z = z
    }

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        stream.putFloat(x)
        stream.putFloat(y)
        stream.putFloat(z)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.long, stream.long)
        x = stream.float
        y = stream.float
        z = stream.float
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { entity ->
            if (entity is MobileEntityClient) {
                entity.positionReceiver.receiveRotation(x.toDouble(),
                        y.toDouble(),
                        z.toDouble())
            }
        }
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putFloat(x)
        stream.putFloat(y)
        stream.putFloat(z)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        x = stream.float
        y = stream.float
        z = stream.float
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            mob.positionReceiver.receiveRotation(x.toDouble(), y.toDouble(),
                    z.toDouble())
        }
    }
}