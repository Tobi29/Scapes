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

package org.tobi29.scapes.entity.skin

import org.tobi29.checksums.Checksum
import org.tobi29.graphics.Image
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.packets.PacketSkin
import org.tobi29.stdex.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class ClientSkinStorage(private val engine: ScapesEngine,
                        private val defaultTexture: Texture) {
    private val skins = ConcurrentHashMap<Checksum, ClientSkin>()
    // TODO: Port away
    private val skinRequests = ConcurrentLinkedQueue<Checksum>()
    private val defaultSkin = defaultTexture.buffer(0)
            ?: throw IllegalArgumentException("Default skin texture is empty")

    fun update(connection: ClientConnection) {
        val oldSkins = skins.values.filter { it.increaseTicks() > 1200 }
        oldSkins.forEach { skin -> skins.remove(skin.checksum) }
        while (!skinRequests.isEmpty()) {
            skinRequests.poll()?.let { request ->
                connection.send(
                        PacketSkin(connection.plugins.registry, request))
            }
        }
    }

    fun addSkin(checksum: Checksum,
                image: Image) {
        val skin = skins[checksum]
        skin?.setImage(image.view)
    }

    operator fun get(checksum: Checksum?): Texture {
        if (checksum == null) {
            return defaultTexture
        }
        var skin: ClientSkin? = skins[checksum]
        if (skin == null) {
            skin = ClientSkin(engine, defaultSkin, checksum)
            skins.put(checksum, skin)
            skinRequests.add(checksum)
        }
        return skin.texture
    }
}
