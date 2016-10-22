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

package org.tobi29.scapes.client.states

import mu.KLogging
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldSource
import java.io.IOException

class GameStateGameSP @Throws(IOException::class)
constructor(
        clientSupplier: (GameStateGameMP) -> ClientConnection,
        private val source: WorldSource, private val server: ScapesServer, scene: Scene,
        engine: ScapesEngine) : GameStateGameMP(clientSupplier, scene, engine) {

    fun source(): WorldSource {
        return source
    }

    fun server(): ScapesServer {
        return server
    }

    override fun dispose() {
        try {
            server.stop(ScapesServer.ShutdownReason.STOP)
        } catch (e: IOException) {
            logger.error(e) { "Error stopping internal server" }
        }

        try {
            source.close()
        } catch (e: IOException) {
            logger.error(e) { "Error closing world source" }
        }

        logger.info { "Stopped internal server!" }
        super.dispose()
    }

    override fun step(delta: Double) {
        super.step(delta)
        if (server.shouldStop()) {
            engine.switchState(
                    GameStateServerDisconnect("Server stopping", engine))
        }
    }

    companion object : KLogging()
}
