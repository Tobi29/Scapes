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

import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.server.ConnectionCloseException
import org.tobi29.scapes.engine.server.PacketBundleChannel
import org.tobi29.scapes.engine.server.receive
import org.tobi29.scapes.engine.utils.*
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.ByteBufferStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.io.filesystem.FileCache
import org.tobi29.scapes.engine.utils.io.filesystem.exists
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.process
import org.tobi29.scapes.engine.utils.io.put
import org.tobi29.scapes.engine.utils.io.tag.binary.readBinary
import org.tobi29.scapes.engine.utils.io.tag.toMutTag
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

object NewClientConnection {
    suspend fun run(channel: PacketBundleChannel,
                    engine: ScapesEngine,
                    account: Account,
                    loadingDistance: Int,
                    progress: (String) -> Unit): Pair<Plugins, Int>? {
        // Send header
        channel.outputStream.put(ConnectionInfo.header())
        channel.outputStream.put(ConnectionType.PLAY.data().toInt())
        channel.queueBundle()

        // Send account info
        progress("Logging in...")
        val keyPair = account.keyPair()
        val array = keyPair.public.encoded
        channel.outputStream.put(array)
        channel.queueBundle()

        // Authenticate account and retrieve plugins
        if (channel.receive()) {
            return null
        }
        var challenge = ByteArray(512)
        channel.inputStream[challenge]
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
        val length = channel.inputStream.int
        val plugins = ArrayList<PluginFile?>(length)
        val pluginRequests = ArrayList<Int>(length)
        for (i in 0..length - 1) {
            val id = channel.inputStream.string
            val version: Version
            val scapesVersion: Version
            try {
                version = versionParse(channel.inputStream.string)
                scapesVersion = versionParse(channel.inputStream.string)
            } catch (e: VersionException) {
                throw IOException(e)
            }

            val checksum = channel.inputStream.byteArray
            val embedded = Plugins.embedded().asSequence()
                    .filter { it.id() == id }
                    .filter {
                        compare(it.version, version).inside(
                                Comparison.LOWER_BUILD,
                                Comparison.HIGHER_MINOR)
                    }.firstOrNull()
            if (embedded != null) {
                plugins.add(embedded)
            } else {
                val location = FileCache.Location("plugins", checksum)
                val file = engine.fileCache.retrieve(location)
                if (file != null) {
                    plugins.add(PluginFile(file))
                } else {
                    pluginRequests.add(i)
                    plugins.add(null)
                }
            }
        }
        channel.outputStream.put(challenge)
        channel.outputStream.putString(account.nickname())
        channel.outputStream.putInt(pluginRequests.size)
        for (i in pluginRequests) {
            channel.outputStream.putInt(i)
        }
        channel.queueBundle()

        // Receive missing plugins
        progress("Downloading plugins...")
        if (channel.receive()) {
            return null
        }
        if (channel.inputStream.boolean) {
            throw ConnectionCloseException(channel.inputStream.string)
        }
        var pluginI = 0
        val pluginL = pluginRequests.size
        val pluginStream = ByteBufferStream()
        while (!pluginRequests.isEmpty()) {
            progress("Downloading plugins ($pluginI/$pluginL)...")
            if (channel.receive()) {
                return null
            }
            val request = pluginRequests[0]
            if (channel.inputStream.boolean) {
                pluginStream.buffer().flip()
                val file = engine.fileCache.retrieve(
                        engine.fileCache.store(pluginStream, "plugins"))
                pluginStream.buffer().clear()
                if (file == null) {
                    throw IllegalStateException(
                            "Concurrent cache modification")
                }
                plugins[request] = PluginFile(file)
                pluginRequests.removeAt(0)
                pluginI++
            } else {
                process(channel.inputStream, put(pluginStream))
            }
        }
        channel.outputStream.putInt(loadingDistance)
        sendSkin(channel.outputStream, engine)
        channel.queueBundle()

        // Receive server info
        progress("Receiving server info...")
        if (channel.receive()) {
            return null
        }
        if (channel.inputStream.boolean) {
            throw ConnectionCloseException(channel.inputStream.string)
        }
        val loadingDistanceServer = channel.inputStream.int
        val idStorage = readBinary(channel.inputStream)
        val plugins2 = Plugins(plugins.map {
            it ?: throw IllegalStateException("Failed to receive plugin")
        }, idStorage.toMutTag())
        return Pair(plugins2, loadingDistanceServer)
    }

    private fun sendSkin(output: WritableByteStream,
                         engine: ScapesEngine) {
        val path = engine.home.resolve("Skin.png")
        val image = if (exists(path)) {
            read(path) { decodePNG(it) }
        } else {
            engine.files["Scapes:image/entity/mob/Player.png"].get().read {
                decodePNG(it)
            }
        }
        if (image.width != 64 || image.height != 64) {
            throw ConnectionCloseException("Invalid skin!")
        }
        val skin = ByteArray(64 * 64 * 4)
        image.buffer.get(skin)
        output.put(skin)
    }
}