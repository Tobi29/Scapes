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
import org.tobi29.scapes.server.connection.PlayerConnection
import java.io.IOException

open class PacketBlockChange : PacketAbstract, PacketClient {
    protected var x = 0
    protected var y = 0
    protected var z = 0
    protected var id = 0
    protected var data = 0

    constructor()

    constructor(x: Int, y: Int, z: Int, id: Int, data: Int) : super(
            Vector3d(x + 0.5, y + 0.5, z + 0.5), true) {
        this.x = x
        this.y = y
        this.z = z
        this.id = id
        this.data = data
    }

    @Throws(IOException::class)
    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.putInt(x)
        stream.putInt(y)
        stream.putInt(z)
        stream.putShort(id)
        stream.putShort(data)
    }

    @Throws(IOException::class)
    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        x = stream.int
        y = stream.int
        z = stream.int
        id = stream.short.toInt()
        data = stream.short.toInt()
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
