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
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketSoundEffect : PacketAbstract, PacketClient {
    private lateinit var audio: String
    private lateinit var position: Vector3d
    private lateinit var velocity: Vector3d
    private var pitch = 0.0
    private var gain = 0.0
    private var referenceDistance = 0.0
    private var rolloffFactor = 0.0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                audio: String,
                position: Vector3d,
                velocity: Vector3d,
                pitch: Double,
                gain: Double,
                referenceDistance: Double,
                rolloffFactor: Double) : super(type, position,
            calculateDistance(0.05, referenceDistance, rolloffFactor)) {
        this.position = position
        this.velocity = velocity
        this.audio = audio
        this.pitch = pitch
        this.gain = gain
        this.referenceDistance = referenceDistance
        this.rolloffFactor = rolloffFactor
    }

    constructor(registry: Registries,
                audio: String,
                position: Vector3d,
                velocity: Vector3d,
                pitch: Double,
                gain: Double,
                referenceDistance: Double,
                rolloffFactor: Double) : this(
            Packet.make(registry, "core.packet.SoundEffect"), audio,
            position, velocity, pitch, gain, referenceDistance, rolloffFactor)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putString(audio)
        stream.putDouble(position.x)
        stream.putDouble(position.y)
        stream.putDouble(position.z)
        stream.putFloat(velocity.x.toFloat())
        stream.putFloat(velocity.y.toFloat())
        stream.putFloat(velocity.z.toFloat())
        stream.putFloat(pitch.toFloat())
        stream.putFloat(gain.toFloat())
        stream.putFloat(referenceDistance.toFloat())
        stream.putFloat(rolloffFactor.toFloat())
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        audio = stream.getString()
        position = Vector3d(stream.getDouble(), stream.getDouble(),
                stream.getDouble())
        velocity = Vector3d(stream.getFloat().toDouble(),
                stream.getFloat().toDouble(), stream.getFloat().toDouble())
        pitch = stream.getFloat().toDouble()
        gain = stream.getFloat().toDouble()
        referenceDistance = stream.getFloat().toDouble()
        rolloffFactor = stream.getFloat().toDouble()
    }

    override fun runClient(client: ClientConnection) {
        client.mob {
            it.world.playSound(audio, position, velocity, pitch, gain,
                    referenceDistance, rolloffFactor)
        }
    }

    companion object {
        private fun calculateGain(distance: Double,
                                  referenceDistance: Double,
                                  rolloffFactor: Double) =
                referenceDistance / (referenceDistance +
                        rolloffFactor * (distance - referenceDistance))

        private fun calculateDistance(gain: Double,
                                      referenceDistance: Double,
                                      rolloffFactor: Double) =
                (referenceDistance / gain - referenceDistance) /
                        rolloffFactor + referenceDistance
    }
}
