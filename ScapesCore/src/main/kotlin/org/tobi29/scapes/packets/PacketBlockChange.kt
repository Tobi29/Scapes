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
import org.tobi29.io.ReadableByteStream
import org.tobi29.io.WritableByteStream
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.server.connection.PlayerConnection

open class PacketBlockChange : PacketAbstract, PacketClient {
    protected var x = 0
    protected var y = 0
    protected var z = 0
    protected var id = 0
    protected var data = 0

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                x: Int,
                y: Int,
                z: Int,
                id: Int,
                data: Int) : super(type, Vector3d(x + 0.5, y + 0.5, z + 0.5),
            true) {
        this.x = x
        this.y = y
        this.z = z
        this.id = id
        this.data = data
    }

    constructor(registry: Registries,
                x: Int,
                y: Int,
                z: Int,
                id: Int,
                data: Int) : this(
            Packet.make(registry, "core.packet.BlockChange"), x, y, z, id,
            data)

    // TODO: @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putInt(x)
        stream.putInt(y)
        stream.putInt(z)
        stream.putShort(id.toShort())
        stream.putShort(data.toShort())
    }

    // TODO: @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        x = stream.getInt()
        y = stream.getInt()
        z = stream.getInt()
        id = stream.getShort().toInt()
        data = stream.getShort().toInt()
    }

    override fun runClient(client: ClientConnection) {
        client.mob { it.world.terrain.process(this) }
    }

    fun x(): Int {
        return x
    }

    fun y(): Int {
        return y
    }

    fun z(): Int {
        return z
    }

    fun id(): Int {
        return id
    }

    fun data(): Int {
        return data
    }
}
