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

package org.tobi29.scapes.client.connection

import org.tobi29.scapes.chunk.IDStorage
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.*
import org.tobi29.scapes.engine.utils.graphics.Image
import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.io.*
import org.tobi29.scapes.engine.utils.io.filesystem.FileCache
import org.tobi29.scapes.engine.utils.io.filesystem.exists
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.plugins.Plugins
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

class NewConnection(private val engine: ScapesEngine,
                    private val channel: PacketBundleChannel,
                    private val account: Account,
                    private var loadingDistance: Int) {
    private val loadingDistanceRequest: Int
    private val cache: FileCache
    private val pluginRequests = ArrayList<Int>()
    private val plugins = ArrayList<PluginFile?>()
    private val pluginStream = ByteBufferStream()
    private var idStorage: IDStorage? = null
    private var state: ((RandomReadableByteStream) -> String?)? = null
    private var status: String? = "Logging in..."

    init {
        loadingDistanceRequest = loadingDistance
        cache = engine.fileCache
        loginStep0()
    }

    private fun loginStep0(): String? {
        val output = channel.outputStream
        output.put(ConnectionInfo.header())
        output.put(ConnectionType.PLAY.data().toInt())
        channel.queueBundle()
        val keyPair = account.keyPair()
        val array = keyPair.public.encoded
        output.put(array)
        channel.queueBundle()
        state = { this.loginStep1(it) }
        return "Logging in..."
    }

    private fun loginStep1(input: RandomReadableByteStream): String? {
        var challenge = ByteArray(512)
        input[challenge]
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

        val length = input.int
        for (i in 0..length - 1) {
            val id = input.string
            val version: Version
            val scapesVersion: Version
            try {
                version = versionParse(input.string)
                scapesVersion = versionParse(input.string)
            } catch (e: VersionException) {
                throw IOException(e)
            }

            val checksum = input.byteArray
            val embedded = Plugins.embedded().stream().filter { plugin -> plugin.id() == id }.filter { plugin ->
                compare(plugin.version(), version).`in`(Comparison.LOWER_BUILD,
                        Comparison.HIGHER_MINOR)
            }.findAny()
            if (embedded.isPresent) {
                plugins.add(embedded.get())
            } else {
                val location = FileCache.Location("plugins", checksum)
                val file = cache.retrieve(location)
                if (file != null) {
                    plugins.add(PluginFile(file))
                } else {
                    pluginRequests.add(i)
                    plugins.add(null)
                }
            }
        }
        val output = channel.outputStream
        output.put(challenge)
        output.putString(account.nickname())
        output.putInt(pluginRequests.size)
        for (i in pluginRequests) {
            output.putInt(i)
        }
        channel.queueBundle()
        state = { this.loginStep2(it) }
        return "Logging in..."
    }

    private fun loginStep2(input: RandomReadableByteStream): String? {
        if (input.boolean) {
            throw ConnectionCloseException(input.string)
        }
        if (pluginRequests.isEmpty()) {
            return loginStep4()
        }
        state = { this.loginStep3(it) }
        return "Downloading plugins..."
    }

    private fun loginStep3(input: RandomReadableByteStream): String? {
        val request = pluginRequests[0]
        if (input.boolean) {
            pluginStream.buffer().flip()
            val file = cache.retrieve(cache.store(pluginStream, "plugins"))
            pluginStream.buffer().clear()
            if (file == null) {
                throw IllegalStateException(
                        "Concurrent cache modification")
            }
            plugins[request] = PluginFile(file)
            pluginRequests.removeAt(0)
            if (pluginRequests.isEmpty()) {
                return loginStep4()
            }
        } else {
            process(input, put(pluginStream))
        }
        return "Downloading plugins..."
    }

    private fun loginStep4(): String? {
        val output = channel.outputStream
        output.putInt(loadingDistanceRequest)
        sendSkin(output)
        channel.queueBundle()
        state = { this.loginStep5(it) }
        return "Receiving server info..."
    }

    private fun loginStep5(input: ReadableByteStream): String? {
        if (input.boolean) {
            throw ConnectionCloseException(input.string)
        }
        loadingDistance = input.int
        val idsTag = TagStructure()
        TagStructureBinary.read(input, idsTag)
        idStorage = IDStorage(idsTag)
        state = null
        return null
    }

    private fun sendSkin(output: WritableByteStream) {
        val path = engine.home.resolve("Skin.png")
        val image: Image
        if (exists(path)) {
            image = read(path) { decodePNG(it) { BufferCreator.bytes(it) } }
        } else {
            val reference = AtomicReference<Image>()
            engine.files["Scapes:image/entity/mob/Player.png"].read({ stream ->
                val defaultImage = decodePNG(stream) { BufferCreator.bytes(it) }
                reference.set(defaultImage)
            })
            image = reference.get()
        }
        if (image.width != 64 || image.height != 64) {
            throw ConnectionCloseException("Invalid skin!")
        }
        val skin = ByteArray(64 * 64 * 4)
        image.buffer.get(skin)
        output.put(skin)
    }

    fun login(): String? {
        if (channel.process({ state },
                { state, bundle -> status = state(bundle) })) {
            throw IOException("Connection closed before login")
        }
        return status
    }

    fun finish(): (GameStateGameMP) -> RemoteClientConnection {
        val plugins = Plugins(this.plugins.map { it!! }, idStorage!!)
        return { state ->
            RemoteClientConnection(state, channel, plugins, loadingDistance)
        }
    }
}
