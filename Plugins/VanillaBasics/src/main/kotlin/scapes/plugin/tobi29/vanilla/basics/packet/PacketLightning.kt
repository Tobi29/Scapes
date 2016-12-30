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

package scapes.plugin.tobi29.vanilla.basics.packet

import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.server.connection.PlayerConnection
import scapes.plugin.tobi29.vanilla.basics.entity.particle.ParticleEmitterLightning
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom

class PacketLightning : PacketAbstract, PacketClient {
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    constructor() {
    }

    constructor(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putDouble(x)
        stream.putDouble(y)
        stream.putDouble(z)
    }

    @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        x = stream.double
        y = stream.double
        z = stream.double
    }

    override fun runClient(client: ClientConnection) {
        client.mob { mob ->
            val pos = Vector3d(x, y, z)
            val emitter = mob.world.scene.particles().emitter(
                    ParticleEmitterLightning::class.java)
            emitter.add { instance ->
                val random = ThreadLocalRandom.current()
                instance.pos.set(pos)
                instance.speed.set(Vector3d.ZERO)
                instance.time = 0.5f
                instance.vao = random.nextInt(emitter.maxVAO())
            }
            val random = ThreadLocalRandom.current()
            mob.world.playSound(SOUNDS[random.nextInt(3)], pos, Vector3d.ZERO,
                    1.0f,
                    64.0f)
        }
    }

    companion object {
        private val SOUNDS = arrayOf(
                "VanillaBasics:sound/entity/particle/thunder/Close1.ogg",
                "VanillaBasics:sound/entity/particle/thunder/Close2.ogg",
                "VanillaBasics:sound/entity/particle/thunder/Close3.ogg")
    }
}