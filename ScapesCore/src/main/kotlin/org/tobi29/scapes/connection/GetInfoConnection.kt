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

package org.tobi29.scapes.connection

import mu.KLogging
import org.tobi29.scapes.engine.server.Connection
import org.tobi29.scapes.engine.server.ConnectionManager
import org.tobi29.scapes.engine.server.ConnectionWorker
import org.tobi29.scapes.engine.server.PacketBundleChannel
import java.io.IOException
import java.nio.channels.SelectionKey

class GetInfoConnection(worker: ConnectionWorker,
                        private val channel: PacketBundleChannel,
                        serverInfo: ServerInfo) : Connection {
    private val startup: Long
    private var state = State.OPEN

    init {
        channel.register(worker.joiner, SelectionKey.OP_READ)
        startup = System.nanoTime()
        val output = channel.outputStream
        output.put(serverInfo.getBuffer())
        channel.queueBundle()
        channel.requestClose()
    }

    override fun tick(worker: ConnectionWorker) {
        try {
            if (channel.process({ true })) {
                state = State.CLOSED
            }
        } catch (e: IOException) {
            logger.info { "Error in info connection: $e" }
            state = State.CLOSED
        }

    }

    override val isClosed: Boolean
        get() = System.nanoTime() - startup > 10000000000L || state == State.CLOSED

    override fun requestClose() {
        channel.requestClose()
    }

    override fun close() {
        channel.close()
    }

    private enum class State {
        OPEN,
        CLOSED
    }

    companion object : KLogging()
}
