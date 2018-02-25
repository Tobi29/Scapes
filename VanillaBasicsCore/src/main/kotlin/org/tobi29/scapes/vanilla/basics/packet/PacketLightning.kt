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
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.threadLocalRandom
import org.tobi29.scapes.packets.Packet
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketType
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleEmitterLightning

class PacketLightning : PacketAbstract, PacketClient {
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                x: Double,
                y: Double,
                z: Double) : super(type) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(registry: Registries,
                x: Double,
                y: Double,
                z: Double) : this(
            Packet.make(registry, "vanilla.basics.packet.Lightning"), x,
            y, z)

    // TODO: @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putDouble(x)
        stream.putDouble(y)
        stream.putDouble(z)
    }

    // TODO: @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        x = stream.getDouble()
        y = stream.getDouble()
        z = stream.getDouble()
    }

    override fun runClient(client: ClientConnection) {
        client.mob { mob ->
            val pos = Vector3d(x, y, z)
            val emitter = mob.world.scene.particles().emitter(
                    ParticleEmitterLightning::class.java)
            emitter.add { instance ->
                val random = threadLocalRandom()
                instance.pos.set(pos)
                instance.speed.set(Vector3d.ZERO)
                instance.time = 0.5f
                instance.vao = random.nextInt(emitter.maxVAO())
            }
            val random = threadLocalRandom()
            mob.world.playSound(SOUNDS[random.nextInt(3)], pos, Vector3d.ZERO,
                    1.0, 64.0)
        }
    }

    companion object {
        private val SOUNDS = arrayOf(
                "VanillaBasics:sound/entity/particle/thunder/Close1.ogg",
                "VanillaBasics:sound/entity/particle/thunder/Close2.ogg",
                "VanillaBasics:sound/entity/particle/thunder/Close3.ogg")
    }
}
