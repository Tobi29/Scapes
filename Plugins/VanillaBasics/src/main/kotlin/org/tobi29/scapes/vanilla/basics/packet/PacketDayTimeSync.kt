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

package org.tobi29.scapes.vanilla.basics.packet

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentClimate
import java.io.IOException

class PacketDayTimeSync : PacketAbstract, PacketClient {
    private var day = 0L
    private var dayTime = 0.0

    constructor() {
    }

    constructor(dayTime: Double, day: Long) {
        this.dayTime = dayTime
        this.day = day
    }

    @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putFloat(dayTime.toFloat())
        stream.putLong(day)
    }

    @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        dayTime = stream.float.toDouble()
        day = stream.long
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
