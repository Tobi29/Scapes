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

package org.tobi29.scapes.vanilla.basics.packet

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.io.ReadableByteStream
import org.tobi29.io.WritableByteStream
import org.tobi29.scapes.packets.Packet
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketType
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.world.EnvironmentClimate

class PacketDayTimeSync : PacketAbstract, PacketClient {
    private var day = 0L
    private var dayTime = 0.0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                dayTime: Double,
                day: Long) : super(type) {
        this.dayTime = dayTime
        this.day = day
    }

    constructor(registry: Registries,
                dayTime: Double,
                day: Long) : this(
            Packet.make(registry, "vanilla.basics.packet.DayTimeSync"),
            dayTime, day)

    // TODO: @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putFloat(dayTime.toFloat())
        stream.putLong(day)
    }

    // TODO: @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        dayTime = stream.getFloat().toDouble()
        day = stream.getLong()
    }

    override fun runClient(client: ClientConnection) {
        client.mob { mob ->
            val environment = mob.world.environment
            if (environment is EnvironmentClimate) {
                val climateGenerator = environment.climate()
                climateGenerator.setDayTime(dayTime)
                climateGenerator.setDay(day)
            }
        }
    }
}
