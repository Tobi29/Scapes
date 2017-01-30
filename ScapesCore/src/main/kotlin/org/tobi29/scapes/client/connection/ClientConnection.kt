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
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.connection.PlayConnection
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues
import org.tobi29.scapes.engine.server.Connection
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.packets.PacketServer
import org.tobi29.scapes.plugins.Plugins

abstract class ClientConnection(val game: GameStateGameMP,
                                val plugins: Plugins,
                                val loadingDistance: Int) : PlayConnection<PacketServer> {
    val profilerSent = ConnectionProfiler()
    val profilerReceived = ConnectionProfiler()
    protected val pingDebug: GuiWidgetDebugValues.Element
    protected val downloadDebug: GuiWidgetDebugValues.Element
    protected val uploadDebug: GuiWidgetDebugValues.Element
    protected var entity: MobPlayerClientMain? = null
    protected var world: WorldClient? = null

    init {
        val debugValues = game.engine.debugValues
        pingDebug = debugValues["Connection-Ping"]
        downloadDebug = debugValues["Connection-Down"]
        uploadDebug = debugValues["Connection-Up"]
    }

    abstract fun start()

    abstract fun stop()

    abstract suspend fun run(connection: Connection)

    fun mob(consumer: (MobPlayerClientMain) -> Unit) {
        entity?.let {
            it.world.taskExecutor.addTaskOnce({ consumer(it) }, "Player-Mob")
        }
    }

    abstract fun address(): RemoteAddress?

    fun changeWorld(world: WorldClient) {
        this.world = world
        entity = world.player
        game.switchScene(world.scene)
    }

    companion object : KLogging()
}
