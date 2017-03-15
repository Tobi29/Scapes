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

class PacketMobChangeState : PacketAbstract, PacketBoth {
    private lateinit var uuid: UUID
    private var ground = false
    private var slidingWall = false
    private var inWater = false
    private var swimming = false

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                uuid: UUID,
                pos: Vector3d?,
                ground: Boolean,
                slidingWall: Boolean,
                inWater: Boolean,
                swimming: Boolean) : super(type, pos, 32.0, false, false) {
        this.uuid = uuid
        this.ground = ground
        this.slidingWall = slidingWall
        this.inWater = inWater
        this.swimming = swimming
    }

    constructor(registry: Registries,
                uuid: UUID,
                pos: Vector3d?,
                ground: Boolean,
                slidingWall: Boolean,
                inWater: Boolean,
                swimming: Boolean) : this(
            Packet.make(registry, "core.packet.MobChangeState"), uuid,
            pos, ground, slidingWall, inWater, swimming)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putLong(uuid.mostSignificantBits)
        stream.putLong(uuid.leastSignificantBits)
        val b = (if (ground) 1 else 0) or (if (slidingWall) 2 else 0) or (if (inWater) 4 else 0) or
                if (swimming) 8 else 0
        stream.put(b)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        uuid = UUID(stream.long, stream.long)
        val value = stream.get().toInt()
        ground = value and 1 == 1
        slidingWall = value and 2 == 2
        inWater = value and 4 == 4
        swimming = value and 8 == 8
    }

    override fun runClient(client: ClientConnection) {
        client.getEntity(uuid) { entity ->
            if (entity is MobileEntityClient) {
                entity.positionReceiver.receiveState(ground, slidingWall,
                        inWater, swimming)
            }
        }
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        val b = (if (ground) 1 else 0) or (if (slidingWall) 2 else 0) or (if (inWater) 4 else 0) or
                if (swimming) 8 else 0
        stream.put(b)
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        val value = stream.get().toInt()
        ground = value and 1 == 1
        slidingWall = value and 2 == 2
        inWater = value and 4 == 4
        swimming = value and 8 == 8
    }

    override fun runServer(player: PlayerConnection) {
        player.mob { mob ->
            mob.positionReceiver.receiveState(ground, slidingWall, inWater,
                    swimming)
        }
    }
}
