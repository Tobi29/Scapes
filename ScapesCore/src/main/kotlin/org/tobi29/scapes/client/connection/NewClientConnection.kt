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

package org.tobi29.scapes.client.connection

import org.tobi29.graphics.decodePNG
import org.tobi29.io.IOException
import org.tobi29.io.WritableByteStream
import org.tobi29.io.filesystem.FilePath
import org.tobi29.io.filesystem.exists
import org.tobi29.io.tag.binary.readBinary
import org.tobi29.io.tag.toMutTag
import org.tobi29.io.view
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.scapes.plugins.spi.PluginHandle
import org.tobi29.scapes.plugins.spi.PluginReference
import org.tobi29.scapes.plugins.spi.matches
import org.tobi29.server.ConnectionCloseException
import org.tobi29.server.PacketBundleChannel
import org.tobi29.server.receive
import org.tobi29.utils.VersionException
import org.tobi29.utils.versionParse
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

object NewClientConnection {
    suspend fun run(
        channel: PacketBundleChannel,
        engine: ScapesEngine,
        account: Account,
        loadingDistance: Int,
        skin: FilePath,
        progress: (String) -> Unit
    ): Pair<Plugins, Int>? {
        // Send header
        channel.outputStream.put(ConnectionInfo.header().view)
        channel.outputStream.put(ConnectionType.PLAY.data())
        channel.queueBundle()

        // Send account info
        progress("Logging in...")
        val keyPair = account.keyPair()
        val array = keyPair.public.encoded
        channel.outputStream.put(array.view)
        channel.queueBundle()

        // Authenticate account and retrieve plugins
        if (channel.receive()) {
            return null
        }
        var challenge = ByteArray(512)
        channel.inputStream.get(challenge.view)
        try {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, account.keyPair().private)
            challenge = cipher.doFinal(challenge)
        } catch (e: NoSuchAlgorithmException) {
            throw IOException(e)
        } catch (e: NoSuchPaddingException) {
            throw IOException(e)
        } catch (e: IllegalBlockSizeException) {
            throw IOException(e)
        } catch (e: BadPaddingException) {
            throw IOException(e)
        } catch (e: InvalidKeyException) {
            throw IOException(e)
        }
        val length = channel.inputStream.getInt()
        val plugins = ArrayList<PluginHandle>(length)
        for (i in 0 until length) {
            val id = channel.inputStream.getString()
            val version = try {
                versionParse(channel.inputStream.getString())
            } catch (e: VersionException) {
                throw IOException(e)
            }
            val reference = PluginReference(id, version)

            val embedded = Plugins.available().asSequence()
                .filter { it.first.matches(reference) }
                .maxBy { it.first.version }
                    ?: throw IOException("Missing plugin required by server: $reference")
            plugins.add(embedded)
        }
        channel.outputStream.put(challenge.view)
        channel.outputStream.putString(account.nickname())
        channel.outputStream.putInt(loadingDistance)
        sendSkin(skin, channel.outputStream, engine)
        channel.queueBundle()

        // Receive server info
        progress("Receiving server info...")
        if (channel.receive()) {
            return null
        }
        if (channel.inputStream.getBoolean()) {
            throw ConnectionCloseException(channel.inputStream.getString())
        }
        val loadingDistanceServer = channel.inputStream.getInt()
        val idStorage = readBinary(channel.inputStream)
        val plugins2 = Plugins(plugins, idStorage.toMutTag())
        return Pair(plugins2, loadingDistanceServer)
    }

    private suspend fun sendSkin(
        path: FilePath,
        output: WritableByteStream,
        engine: ScapesEngine
    ) {
        val image = if (exists(path)) {
            path.readAsync { decodePNG(it) }
        } else {
            engine.files["Scapes:image/entity/mob/Player.png"].readAsync {
                decodePNG(it)
            }
        }
        if (image.width != 64 || image.height != 64) {
            throw ConnectionCloseException("Invalid skin!")
        }
        output.put(image.view)
    }
}