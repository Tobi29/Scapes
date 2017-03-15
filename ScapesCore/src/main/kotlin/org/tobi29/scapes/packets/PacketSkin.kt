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
import org.tobi29.scapes.engine.utils.ByteBuffer
import org.tobi29.scapes.engine.utils.Checksum
import org.tobi29.scapes.engine.utils.graphics.Image
import org.tobi29.scapes.engine.utils.io.Algorithm
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.server.connection.PlayerConnection
import java.io.IOException

class PacketSkin : PacketAbstract, PacketBoth {
    private lateinit var image: Image
    private lateinit var checksum: Checksum

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                checksum: Checksum) : super(type) {
        this.checksum = checksum
    }

    constructor(type: PacketType,
                image: Image,
                checksum: Checksum) : super(type) {
        this.image = image
        this.checksum = checksum
    }

    constructor(registry: Registries,
                checksum: Checksum) : this(
            Packet.make(registry, "core.packet.Skin"), checksum)

    constructor(registry: Registries,
                image: Image,
                checksum: Checksum) : this(
            Packet.make(registry, "core.packet.Skin"), image, checksum)

    override fun sendClient(player: PlayerConnection,
                            stream: WritableByteStream) {
        stream.put(image.buffer)
        stream.putString(checksum.algorithm.name)
        stream.put(checksum.array())
    }

    override fun parseClient(client: ClientConnection,
                             stream: ReadableByteStream) {
        val buffer = ByteBuffer(64 * 64 * 4)
        stream[buffer]
        buffer.flip()
        image = Image(64, 64, buffer)
        val algorithm: Algorithm
        try {
            algorithm = Algorithm.valueOf(stream.getString(16))
        } catch (e: IllegalArgumentException) {
            throw IOException(e)
        }

        val array = ByteArray(algorithm.bytes)
        stream[array]
        checksum = Checksum(algorithm, array)
    }

    override fun runClient(client: ClientConnection) {
        client.mob {
            it.world.scene.skinStorage().addSkin(checksum, image)
        }
    }

    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putString(checksum.algorithm.name)
        stream.put(checksum.array())
    }

    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        val algorithm: Algorithm
        try {
            algorithm = Algorithm.valueOf(stream.getString(16))
        } catch (e: IllegalArgumentException) {
            throw IOException(e)
        }

        val array = ByteArray(algorithm.bytes)
        stream[array]
        checksum = Checksum(algorithm, array)
    }

    override fun runServer(player: PlayerConnection) {
        player.server.skin(checksum)?.let { skin ->
            player.send(PacketSkin(type, skin.image, checksum))
        }
    }
}
