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

import org.tobi29.scapes.Debug
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.PlayConnection
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.ListenerOwnerHandle
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.math.vector.distanceSqr
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.skin.ServerSkin
import org.tobi29.scapes.packets.PacketChat
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketSetWorld
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.server.extension.event.PlayerAuthenticateEvent
import java.io.IOException

abstract class PlayerConnection(val server: ServerConnection) : PlayConnection<PacketClient>, Executor {
    override val events = EventDispatcher(server.events)
    protected val registry: GameRegistry
    protected var entity: MobPlayerServer? = null
    protected var skin: ServerSkin? = null
    protected var id: String? = null
    protected var nickname = "_Error_"
    protected var added = false
    protected var loadingRadius = 0
    var isClosed = false
        protected set
    var permissionLevel = 0
        get() {
            if (Debug.enabled()) {
                return 10
            }
            return field
        }
    override val listenerOwner = ListenerOwnerHandle { !isClosed }

    init {
        registry = server.plugins.registry()
        events.listenerGlobal<MessageEvent>(this) { event ->
            if (event.level.level >= MessageLevel.CHAT.level) {
                send(PacketChat(event.message))
            }

        }
    }

    fun mob(consumer: (MobPlayerServer) -> Unit) {
        entity?.let {
            it.world.taskExecutor.addTaskOnce({ consumer(it) }, "Player-Mob")
        }
    }

    fun skin(): ServerSkin {
        return skin ?: throw IllegalStateException("Player not initialized")
    }

    fun id(): String {
        return id ?: throw IllegalStateException("Player not initialized")
    }

    fun server(): ServerConnection {
        return server
    }

    @Synchronized fun setWorld(world: WorldServer? = null,
                               pos: Vector3d? = null) {
        removeEntity()
        val player = server.server.getPlayer(id())
        permissionLevel = player.permissions()
        val newEntity = player.createEntity(this, world, pos)
        val isNew: Boolean
        val entity: MobPlayerServer
        if (newEntity != null) {
            entity = newEntity
            isNew = false
        } else {
            val spawnWorld = server.server.defaultWorld()
            if (spawnWorld == null) {
                disconnect("Unable to spawn into a world")
                return
            }
            val spawnPos = Vector3d(spawnWorld.spawn + Vector3i(0, 0, 1))
            entity = spawnWorld.plugins.worldType.newPlayer(spawnWorld,
                    spawnPos, Vector3d.ZERO, 0.0, 0.0, name(), skin().checksum,
                    this)
            isNew = true
        }
        this.entity = entity
        send(PacketSetWorld(entity.world, entity))
        entity.world.addPlayer(entity, isNew)
    }

    @Synchronized protected fun save() {
        server.server.savePlayer(id(),
                entity ?: throw IllegalStateException("Player not initialized"),
                permissionLevel)
    }

    protected fun generateResponse(challengeMatch: Boolean): String? {
        if (!server.doesAllowJoin()) {
            return "Server not public!"
        }
        if (!challengeMatch) {
            return "Invalid protected key!"
        }
        if (!server.server.playerExists(id())) {
            if (!server.doesAllowCreation()) {
                return "This server does not allow account creation!"
            }
        }
        Account.isNameValid(nickname)?.let { return it }
        val event = PlayerAuthenticateEvent(this)
        server.server.events.fire(event)
        if (!event.success) {
            return event.reason
        }
        return null
    }

    fun loadingRadius(): Int {
        return loadingRadius
    }

    override fun send(packet: PacketClient) {
        val entity = this.entity ?: return
        val pos3d = packet.pos()
        if (pos3d != null) {
            val world = entity.world
            if (!world.terrain.isBlockSendable(entity,
                    pos3d.intX(), pos3d.intY(),
                    pos3d.intZ(), packet.isChunkContent)) {
                return
            }
            val range = packet.range()
            if (range > 0.0) {
                if (pos3d.distanceSqr(entity.getCurrentPos()) > range * range) {
                    return
                }
            }
        }
        transmit(packet)
    }

    @Throws(IOException::class)
    protected abstract fun transmit(packet: PacketClient)

    protected fun removeEntity() {
        val entity = this.entity
        if (entity != null) {
            entity.world.removePlayer(entity)
            save()
        }
    }

    override fun playerName(): String? {
        return nickname
    }

    override fun name(): String {
        return nickname
    }

    override fun permissionLevel(): Int {
        return permissionLevel
    }

    fun disconnect(reason: String) {
        disconnect(reason, -1.0)
    }

    abstract fun disconnect(reason: String,
                            time: Double)
}
