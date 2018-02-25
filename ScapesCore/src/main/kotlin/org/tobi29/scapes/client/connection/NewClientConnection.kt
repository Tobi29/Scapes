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

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.server.ConnectionCloseException
import org.tobi29.server.PacketBundleChannel
import org.tobi29.server.receive
import org.tobi29.utils.Version
import org.tobi29.utils.VersionException
import org.tobi29.utils.compare
import org.tobi29.graphics.decodePNG
import org.tobi29.io.*
import org.tobi29.io.filesystem.FileCache
import org.tobi29.io.filesystem.FilePath
import org.tobi29.io.filesystem.exists
import org.tobi29.io.tag.binary.readBinary
import org.tobi29.utils.versionParse
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.io.tag.toMutTag
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
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
        val scapes = engine[ScapesClient.COMPONENT]

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
        val plugins = ArrayList<PluginFile?>(length)
        val pluginRequests = ArrayList<Int>(length)
        for (i in 0 until length) {
            val id = channel.inputStream.getString()
            val version: Version
            val scapesVersion: Version
            try {
                version = versionParse(channel.inputStream.getString())
                scapesVersion = versionParse(channel.inputStream.getString())
            } catch (e: VersionException) {
                throw IOException(e)
            }

            val checksum = channel.inputStream.getByteArray()
            val embedded = Plugins.embedded().asSequence()
                    .filter { it.id() == id }
                    .filter {
                        compare(it.version, version).inside(
                                Version.Comparison.LOWER_REVISION,
                                Version.Comparison.HIGHER_MINOR)
                    }.firstOrNull()
            if (embedded != null) {
                plugins.add(embedded)
            } else {
                val location = FileCache.Location(checksum)
                val file = FileCache.retrieve(scapes.pluginCache, location)
                if (file != null) {
                    plugins.add(PluginFile.loadFile(file))
                } else {
                    pluginRequests.add(i)
                    plugins.add(null)
                }
            }
        }
        channel.outputStream.put(challenge.view)
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
        if (channel.inputStream.getBoolean()) {
            throw ConnectionCloseException(channel.inputStream.getString())
        }
        var pluginI = 0
        val pluginL = pluginRequests.size
        val pluginStream = MemoryViewStreamDefault()
        while (!pluginRequests.isEmpty()) {
            progress("Downloading plugins ($pluginI/$pluginL)...")
            if (channel.receive()) {
                return null
            }
            val request = pluginRequests[0]
            if (channel.inputStream.getBoolean()) {
                pluginStream.flip()
                val file = FileCache.retrieve(scapes.pluginCache,
                        FileCache.store(scapes.pluginCache, pluginStream))
                pluginStream.reset()
                if (file == null) {
                    throw IllegalStateException(
                            "Concurrent cache modification")
                }
                plugins[request] = PluginFile.loadFile(file)
                pluginRequests.removeAt(0)
                pluginI++
            } else {
                channel.inputStream.process { pluginStream.put(it) }
            }
        }
        channel.outputStream.putInt(loadingDistance)
        sendSkin(scapes.home.resolve("Skin.png"), channel.outputStream, engine)
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
        val plugins2 = Plugins(plugins.map {
            it ?: throw IllegalStateException("Failed to receive plugin")
        }, idStorage.toMutTag())
        return Pair(plugins2, loadingDistanceServer)
    }

    private suspend fun sendSkin(path: FilePath,
                                 output: WritableByteStream,
                                 engine: ScapesEngine) {
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