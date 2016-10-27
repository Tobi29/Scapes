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

package org.tobi29.scapes.server.connection

import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.utils.BufferCreator
import org.tobi29.scapes.engine.utils.graphics.Image
import org.tobi29.scapes.engine.utils.io.*
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.entity.skin.ServerSkin
import org.tobi29.scapes.packets.*
import org.tobi29.scapes.plugins.PluginFile
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.extension.event.MessageEvent
import java.io.IOException
import java.nio.channels.SelectionKey
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

class RemotePlayerConnection(private val worker: ConnectionWorker,
                             private val channel: PacketBundleChannel,
                             server: ServerConnection) : PlayerConnection(
        server) {
    private val sendQueue = ConcurrentLinkedQueue<() -> Unit>()
    private val sendQueueSize = AtomicInteger()
    private var state = State.LOGIN
    private var loginState: ((RandomReadableByteStream) -> Unit)?
    private var pingTimeout = 0L
    private var pingWait = 0L

    init {
        channel.register(worker.joiner, SelectionKey.OP_READ)
        pingTimeout = System.currentTimeMillis() + 30000
        loginState = { loginStep1(it) }
    }

    override fun send(packet: PacketClient) {
        if (sendQueueSize.get() > 128) {
            if (!packet.isVital) {
                return
            }
        }
        task({ sendPacket(packet) })
        if (packet.isImmediate) {
            worker.joiner.wake()
        }
    }

    private fun loginStep1(input: RandomReadableByteStream) {
        val array = ByteArray(550)
        input[array]
        id = checksum(array, Algorithm.SHA1).toString()
        val challenge = ByteArray(501)
        SecureRandom().nextBytes(challenge)
        val output = channel.outputStream
        try {
            val key = KeyFactory.getInstance("RSA").generatePublic(
                    X509EncodedKeySpec(array))
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            output.put(cipher.doFinal(challenge))
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
        } catch (e: InvalidKeySpecException) {
            throw IOException(e)
        }

        val plugins = server.plugins
        output.putInt(plugins.fileCount())
        val iterator = plugins.files().iterator()
        while (iterator.hasNext()) {
            sendPluginMetaData(iterator.next(), output)
        }
        channel.queueBundle()
        loginState = { loginStep2(it, challenge) }
    }

    private fun loginStep2(input: RandomReadableByteStream,
                           challengeExpected: ByteArray) {
        val plugins = server.plugins
        val challenge = ByteArray(challengeExpected.size)
        input[challenge]
        nickname = input.getString(1 shl 10)
        var length = input.int
        val requests = ArrayList<Int>(length)
        while (length-- > 0) {
            requests.add(input.int)
        }
        val output = channel.outputStream
        val response = generateResponse(
                Arrays.equals(challenge, challengeExpected))
        if (response != null) {
            output.putBoolean(true)
            output.putString(response)
            channel.queueBundle()
            throw ConnectionCloseException(response)
        }
        output.putBoolean(false)
        channel.queueBundle()
        for (request in requests) {
            sendPlugin(
                    plugins.file(request).file() ?: throw IllegalStateException(
                            "Trying to send embedded plugin"), output)
        }
        loginState = { loginStep3(it) }
    }

    private fun loginStep3(input: RandomReadableByteStream) {
        loadingRadius = clamp(input.int, 10, server.server.maxLoadingRadius())
        val buffer = BufferCreator.bytes(64 * 64 * 4)
        input[buffer]
        buffer.flip()
        skin = ServerSkin(Image(64, 64, buffer))
        val output = channel.outputStream
        val response = server.addPlayer(this)
        if (response != null) {
            output.putBoolean(true)
            output.putString(response)
            channel.queueBundle()
            throw ConnectionCloseException(response)
        }
        added = true
        setWorld()
        output.putBoolean(false)
        output.putInt(loadingRadius)
        TagStructureBinary.write(
                output,
                server.server.plugins.registry().idStorage().save())
        channel.queueBundle()
        val currentTime = System.currentTimeMillis()
        pingWait = currentTime + 1000
        pingTimeout = currentTime + 10000
        server.events.fireLocal(
                MessageEvent(this, MessageLevel.SERVER_INFO,
                        "Player connected: $id ($nickname) on $channel"))
        loginState = null
        state = State.OPEN
    }

    override fun transmit(packet: PacketClient) {
        val output = channel.outputStream
        output.putShort(packet.id(server.plugins.registry()))
        packet.sendClient(this, output)
    }

    @Synchronized override fun close() {
        super.close()
        channel.close()
        state = State.CLOSED
    }

    override fun disconnect(reason: String,
                            time: Double) {
        removeEntity()
        task({
            sendPacket(PacketDisconnect(reason, time))
            throw ConnectionCloseException(reason)
        })
    }

    fun updatePing(ping: Long) {
        pingTimeout = ping + 10000
    }

    private fun sendPluginMetaData(plugin: PluginFile,
                                   output: WritableByteStream) {
        val checksum = plugin.checksum().array()
        output.putString(plugin.id())
        output.putString(plugin.version().toString())
        output.putString(plugin.scapesVersion().toString())
        output.putByteArray(checksum)
    }

    private fun sendPlugin(path: FilePath,
                           output: WritableByteStream) {
        read(path) { stream ->
            process(stream, { buffer ->
                output.putBoolean(false)
                output.put(buffer)
                channel.queueBundle()
            }, 1 shl 10 shl 10)
        }
        output.putBoolean(true)
        channel.queueBundle()
    }

    private fun task(runnable: () -> Unit) {
        sendQueueSize.incrementAndGet()
        sendQueue.add(runnable)
    }

    override fun tick(worker: ConnectionWorker) {
        try {
            when (state) {
                RemotePlayerConnection.State.LOGIN -> channel.process(
                        { loginState }, { obj, t -> obj(t) })
                RemotePlayerConnection.State.OPEN -> try {
                    val currentTime = System.currentTimeMillis()
                    if (pingWait < currentTime) {
                        pingWait = currentTime + 1000
                        sendPacket(PacketPingServer(currentTime))
                    }
                    while (!sendQueue.isEmpty()) {
                        sendQueue.poll()()
                        sendQueueSize.decrementAndGet()
                        if (channel.bundleSize() > 1 shl 10 shl 4) {
                            break
                        }
                    }
                    if (channel.bundleSize() > 0) {
                        channel.queueBundle()
                    }
                    if (channel.process({ bundle ->
                        while (bundle.hasRemaining()) {
                            val packet = PacketAbstract.make(registry,
                                    bundle.short) as PacketServer
                            packet.parseServer(this, bundle)
                            packet.runServer(this)
                        }
                        true
                    })) {
                        state = State.CLOSED
                    }
                } catch (e: ConnectionCloseException) {
                    if (channel.bundleSize() > 0) {
                        channel.queueBundle()
                    }
                }

                RemotePlayerConnection.State.CLOSING -> if (channel.processVoid()) {
                    state = State.CLOSED
                }
                else -> throw IllegalStateException("Unknown state: " + state)
            }
        } catch (e: ConnectionCloseException) {
            server.events.fireLocal(
                    MessageEvent(this, MessageLevel.SERVER_INFO,
                            "Disconnecting player: $nickname"))
            channel.requestClose()
            state = State.CLOSING
        } catch (e: InvalidPacketDataException) {
            server.events.fireLocal(
                    MessageEvent(this, MessageLevel.SERVER_INFO,
                            "Disconnecting player: $nickname"))
            channel.requestClose()
            state = State.CLOSING
        } catch (e: IOException) {
            server.events.fireLocal(
                    MessageEvent(this, MessageLevel.SERVER_INFO,
                            "Player disconnected: $nickname ($e)"))
            state = State.CLOSED
        }

    }

    override val isClosed: Boolean
        get() = pingTimeout < System.currentTimeMillis() || state == State.CLOSED

    internal enum class State {
        LOGIN,
        OPEN,
        CLOSING,
        CLOSED
    }
}
