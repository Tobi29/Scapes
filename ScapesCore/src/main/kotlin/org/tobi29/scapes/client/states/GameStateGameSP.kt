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

import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.Scene
import org.tobi29.scapes.engine.utils.IOException
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldSource

class GameStateGameSP(clientSupplier: (GameStateGameMP) -> ClientConnection,
                      loadScene: Scene,
                      val source: WorldSource,
                      val server: ScapesServer,
                      engine: ScapesEngine) : GameStateGameMP(clientSupplier,
        loadScene, engine) {
    override fun dispose() {
        this.scene?.dispose()
        client.plugins.removeFileSystems(engine.files)
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
        client.stop()
        terrainTextureRegistry.texture.markDisposed()
        engine.sounds.stop("music")
        engine.graphics.textures.clearCache()
        logger.info { "Stopped game!" }
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
