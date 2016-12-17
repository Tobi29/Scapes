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

import mu.KLogging
import org.tobi29.scapes.connection.ConnectionInfo
import org.tobi29.scapes.connection.ConnectionType
import org.tobi29.scapes.connection.GetInfoConnection
import org.tobi29.scapes.connection.ServerInfo
import org.tobi29.scapes.engine.server.Connection
import org.tobi29.scapes.engine.server.ConnectionWorker
import org.tobi29.scapes.engine.server.PacketBundleChannel
import org.tobi29.scapes.engine.utils.ByteBuffer
import java.io.IOException
import java.nio.channels.SelectionKey

class GetInfoOutConnection(worker: ConnectionWorker,
                           private val channel: PacketBundleChannel,
                           private val error: (IOException) -> Unit,
                           private val callback: (ServerInfo) -> Unit) : Connection {
    private val startup: Long
    private var state = State.OPEN

    init {
        channel.register(worker.joiner, SelectionKey.OP_READ)
        startup = System.nanoTime()
        val output = channel.outputStream
        output.put(ConnectionInfo.header())
        output.put(ConnectionType.GET_INFO.data().toInt())
        channel.queueBundle()
    }

    override fun tick(worker: ConnectionWorker) {
        try {
            if (channel.process({ bundle ->
                if (state != State.OPEN) {
                    return@process true
                }
                val infoBuffer = ByteBuffer(bundle.remaining())
                bundle[infoBuffer]
                infoBuffer.flip()
                val serverInfo = ServerInfo(infoBuffer)
                state = State.CLOSING
                channel.requestClose()
                callback(serverInfo)
                true
            })) {
                state = State.CLOSED
            }
        } catch (e: IOException) {
            GetInfoConnection.logger.info { "Error in info connection: $e" }
            error(e)
            state = State.CLOSED
        }
    }

    override val isClosed: Boolean
        get() = System.nanoTime() - startup > 10000000000L || state == State.CLOSED

    override fun requestClose() {
        channel.requestClose()
        state = State.CLOSING
    }

    override fun close() {
        channel.close()
    }

    private enum class State {
        OPEN,
        CLOSING,
        CLOSED
    }

    companion object : KLogging()
}
