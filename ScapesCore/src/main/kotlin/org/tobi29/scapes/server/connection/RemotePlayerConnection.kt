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

package org.tobi29.scapes.server.connection

import kotlinx.coroutines.experimental.yield
import org.tobi29.checksums.ChecksumAlgorithm
import org.tobi29.checksums.checksum
import org.tobi29.graphics.Image
import org.tobi29.io.IOException
import org.tobi29.io.WritableByteStream
import org.tobi29.io.filesystem.FilePath
import org.tobi29.io.filesystem.read
import org.tobi29.io.process
import org.tobi29.io.tag.binary.writeBinary
import org.tobi29.io.view
import org.tobi29.logging.KLogging
import org.tobi29.scapes.entity.skin.ServerSkin
import org.tobi29.scapes.packets.*
import org.tobi29.scapes.plugins.spi.PluginDescription
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.server.extension.event.PlayerJoinEvent
import org.tobi29.scapes.server.extension.event.PlayerLeaveEvent
import org.tobi29.server.*
import org.tobi29.stdex.atomic.AtomicInt
import org.tobi29.stdex.math.clamp
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentLinkedQueue
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

class RemotePlayerConnection(
    private val worker: ConnectionWorker,
    private val channel: PacketBundleChannel,
    server: ServerConnection
) : PlayerConnection(
    server
) {
    // TODO: Port away
    private val sendQueue = ConcurrentLinkedQueue<PacketClient>()
    private val sendQueueSize = AtomicInt()
    private var pingWait = 0L
    private var pingHandler: (Long) -> Unit = {}

    suspend fun run(connection: Connection) {
        pingHandler = { connection.increaseTimeout(10000L - it) }
        try {
            val output = channel.outputStream
            val input = channel.inputStream
            if (channel.receive()) {
                return
            }
            val array = ByteArray(550)
            input.get(array.view)
            id = checksum(array, ChecksumAlgorithm.Sha256).toString()
            val challenge = ByteArray(501)
            SecureRandom().nextBytes(challenge)
            try {
                val key = KeyFactory.getInstance("RSA").generatePublic(
                    X509EncodedKeySpec(array)
                )
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                output.put(cipher.doFinal(challenge).view)
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
            output.putInt(plugins.files.size)
            plugins.files.forEach { sendPluginMetaData(it.first, output) }
            channel.queueBundle()

            if (channel.receive()) {
                return
            }
            val challengeReceived = ByteArray(challenge.size)
            input.get(challengeReceived.view)
            nickname = input.getString(1 shl 10)
            loadingRadius = clamp(
                input.getInt(), 10,
                server.server.maxLoadingRadius
            )
            val buffer = ByteArray(64 * 64 * 4).view
            input.get(buffer)
            skin = ServerSkin(Image(64, 64, buffer))

            val response = generateResponse(
                challengeReceived contentEquals challenge
            ) ?: server.addPlayer(this@RemotePlayerConnection)
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
            server.server.plugins.registry.writeIDStorage().writeBinary(output)
            channel.queueBundle()
            pingWait = System.currentTimeMillis() + 1000
            events.fire(PlayerJoinEvent(this@RemotePlayerConnection))
            events.fire(
                MessageEvent(
                    this@RemotePlayerConnection,
                    MessageLevel.SERVER_INFO,
                    "Player connected: $id ($nickname) on $channel"
                )
            )
            while (!connection.shouldClose) {
                if (connection.shouldClose) {
                    send(PacketDisconnect(registry, "Server closed", 5.0))
                    channel.queueBundle()
                    return
                }
                try {
                    val currentTime = System.currentTimeMillis()
                    if (pingWait < currentTime) {
                        pingWait = currentTime + 1000
                        send(PacketPingServer(registry, currentTime))
                    }
                    while (!sendQueue.isEmpty()) {
                        val packet = sendQueue.poll() ?: continue
                        sendQueueSize.decrementAndGet()
                        // This packet is not registered as it is just for
                        // internal use
                        if (packet !is PacketDisconnectSelf) {
                            channel.outputStream.putShort(
                                packet.type.id.toShort()
                            )
                        }
                        packet.sendClient(this, channel.outputStream)
                        if (channel.bundleSize() > 1 shl 10 shl 4) {
                            break
                        }
                    }
                    if (channel.bundleSize() > 0) {
                        channel.queueBundle()
                    }
                    loop@ while (true) {
                        when (channel.process()) {
                            PacketBundleChannel.FetchResult.CLOSED -> {
                                events.fire(
                                    MessageEvent(
                                        this@RemotePlayerConnection,
                                        MessageLevel.SERVER_INFO,
                                        "Player disconnected: $nickname"
                                    )
                                )
                                return
                            }
                            PacketBundleChannel.FetchResult.YIELD -> break@loop
                            PacketBundleChannel.FetchResult.BUNDLE -> {
                                while (channel.inputStream.hasRemaining()) {
                                    val packet = PacketAbstract.make(
                                        registry,
                                        channel.inputStream.getShort().toInt()
                                    ).createServer()
                                    packet.parseServer(
                                        this@RemotePlayerConnection,
                                        channel.inputStream
                                    )
                                    packet.runServer(
                                        this@RemotePlayerConnection
                                    )
                                }
                            }
                        }
                    }
                } catch (e: ConnectionCloseException) {
                    if (channel.bundleSize() > 0) {
                        channel.queueBundle()
                    }
                    throw e
                }
                yield()
            }
        } catch (e: ConnectionCloseException) {
            events.fire(
                MessageEvent(
                    this@RemotePlayerConnection,
                    MessageLevel.SERVER_INFO,
                    "Disconnecting player: $nickname"
                )
            )
        } catch (e: InvalidPacketDataException) {
            events.fire(
                MessageEvent(
                    this@RemotePlayerConnection,
                    MessageLevel.SERVER_INFO,
                    "Disconnecting player: $nickname"
                )
            )
        } catch (e: IOException) {
            events.fire(
                MessageEvent(
                    this@RemotePlayerConnection,
                    MessageLevel.SERVER_INFO,
                    "Player disconnected: $nickname ($e)"
                )
            )
            throw e
        } finally {
            events.fire(PlayerLeaveEvent(this@RemotePlayerConnection))
            isClosed = true
            if (added) {
                server.removePlayer(this)
                added = false
            }
            close()
            try {
                channel.close()
            } catch (e: IOException) {
                logger.warn(e) { "Failed to close socket" }
            }
        }
    }

    override fun transmit(packet: PacketClient) {
        if (sendQueueSize.get() > 128) {
            if (!packet.isVital) {
                return
            }
        }
        sendQueueSize.incrementAndGet()
        sendQueue.add(packet)
        if (packet.isImmediate) {
            worker.wake()
        }
    }

    override fun disconnect(
        reason: String,
        time: Double
    ) {
        removeEntity()
        send(PacketDisconnect(registry, reason, time))
        send(PacketDisconnectSelf(registry, reason))
    }

    fun updatePing(ping: Long) {
        pingHandler(ping)
    }

    private fun sendPluginMetaData(
        plugin: PluginDescription,
        output: WritableByteStream
    ) {
        output.putString(plugin.id)
        output.putString(plugin.version.toString())
    }

    private fun sendPlugin(
        path: FilePath,
        output: WritableByteStream
    ) {
        read(path) { stream ->
            stream.process(1 shl 10 shl 10) {
                output.putBoolean(false)
                output.put(it)
                channel.queueBundle()
            }
        }
        output.putBoolean(true)
        channel.queueBundle()
    }

    companion object : KLogging()
}
