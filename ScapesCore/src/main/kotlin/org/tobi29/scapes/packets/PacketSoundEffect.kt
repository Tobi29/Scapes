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

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketSoundEffect : PacketAbstract, PacketClient {
    private lateinit var audio: String
    private lateinit var position: Vector3d
    private lateinit var velocity: Vector3d
    private var pitch = 0.0f
    private var gain = 0.0f

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                audio: String,
                position: Vector3d,
                velocity: Vector3d,
                pitch: Float,
                gain: Float,
                range: Float) : super(type, position, range.toDouble()) {
        this.position = position
        this.velocity = velocity
        this.audio = audio
        this.pitch = pitch
        this.gain = gain
    }

    constructor(registry: GameRegistry,
                audio: String,
                position: Vector3d,
                velocity: Vector3d,
                pitch: Float,
                gain: Float,
                range: Float) : this(
            Packet.make(registry, "core.packet.SoundEffect"), audio,
            position, velocity, pitch, gain, range)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putString(audio)
        stream.putDouble(position.x)
        stream.putDouble(position.y)
        stream.putDouble(position.z)
        stream.putDouble(velocity.x)
        stream.putDouble(velocity.y)
        stream.putDouble(velocity.z)
        stream.putFloat(pitch)
        stream.putFloat(gain)
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        audio = stream.string
        position = Vector3d(stream.double, stream.double,
                stream.double)
        velocity = Vector3d(stream.double, stream.double,
                stream.double)
        pitch = stream.float
        gain = stream.float
    }

    override fun runClient(client: ClientConnection) {
        client.mob {
            it.world.playSound(audio, position, velocity, pitch, gain)
        }
    }
}
