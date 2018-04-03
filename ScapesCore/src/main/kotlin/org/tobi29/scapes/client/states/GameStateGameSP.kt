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

package org.tobi29.scapes.client.states

import kotlinx.coroutines.experimental.runBlocking
import org.tobi29.io.IOException
import org.tobi29.logging.KLogging
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.Playlist
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.server.RemoteAddress

class GameStateGameSP(
    clientSupplier: (GameStateGameMP) -> ClientConnection,
    config: WorldClient.Config,
    playlist: Playlist,
    loadScene: Scene,
    val source: WorldSource,
    val server: ScapesServer,
    onClose: () -> Unit,
    onError: (String, RemoteAddress?, Double?) -> Unit,
    engine: ScapesEngine
) : GameStateGameMP(
    clientSupplier, config, playlist, loadScene, onClose, onError, engine
) {
    override fun dispose() {
        runBlocking { scene?.dispose() }
        try {
            server.stop(ScapesServer.ShutdownReason.ERROR)
        } catch (e: IOException) {
            logger.error(e) { "Error stopping internal server" }
        }
        try {
            source.close()
        } catch (e: IOException) {
            logger.error(e) { "Error closing world source" }
        }
        logger.info { "Stopped internal server!" }
        client.stop()
        terrainTextureRegistry.texture.markDisposed()
        engine.sounds.stop("music")
        engine.graphics.textures.clearCache()
        engine.sounds.clearCache()
        logger.info { "Stopped game!" }
    }

    override fun step(delta: Double) {
        super.step(delta)
        if (server.shouldStop()) {
            onError("Server stopping", null, null)
        }
    }

    companion object : KLogging()
}
