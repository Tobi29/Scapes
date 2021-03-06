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

import org.tobi29.scapes.client.gui.GuiDisconnected
import org.tobi29.scapes.client.states.scenes.SceneError
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.renderScene
import org.tobi29.server.RemoteAddress

class GameStateServerDisconnect(
    message: String,
    private val address: RemoteAddress?,
    reconnectTimer: Double?,
    engine: ScapesEngine
) : GameState(
    engine
) {
    private val gui = GuiDisconnected(this, message, engine.guiStyle)
    private val scene = SceneError(engine)
    private var reconnectTimer = reconnectTimer ?: -1.0

    override fun init() {
        engine.guiStack.add("10-Menu", gui)
        switchPipeline { gl ->
            renderScene(gl, scene)
        }
    }

    override fun dispose() {
        engine.guiStack.remove(gui)
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
        if (address != null && reconnectTimer > 0.0) {
            reconnectTimer -= delta
            if (reconnectTimer <= 0.0) {
                engine.switchState(GameStateLoadMP(address, engine, scene))
            } else {
                gui.setReconnectTimer(reconnectTimer)
            }
        }
    }
}
