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

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.launch
import org.tobi29.scapes.Debug
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.connection.PlayConnection
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.distanceSqr
import org.tobi29.math.vector.plus
import org.tobi29.utils.EventDispatcher
import org.tobi29.stdex.math.floorToInt
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.skin.ServerSkin
import org.tobi29.scapes.packets.PacketChat
import org.tobi29.scapes.packets.PacketClient
import org.tobi29.scapes.packets.PacketSetWorld
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.scapes.server.extension.event.PlayerAuthenticateEvent

abstract class PlayerConnection(val server: ServerConnection) : PlayConnection<PacketClient>,
        Executor {
    protected val registry = server.plugins.registry
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
    override val events = EventDispatcher(server.server.events) {
        listen<MessageEvent>({
            (it.target == null || it.target == this@PlayerConnection) &&
                    (it.targetWorld == null || it.targetWorld == entity?.world)
        }) { event ->
            if (event.level.level >= MessageLevel.CHAT.level) {
                send(PacketChat(registry, event.message))
            }
        }
    }.apply { enable() }

    fun mob(consumer: (MobPlayerServer) -> Unit) {
        entity?.let {
            launch(it.world + CoroutineName("Player-Mob")) { consumer(it) }
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

    @Synchronized
    fun setWorld(world: WorldServer? = null,
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
            val spawnPos = Vector3d(spawnWorld.spawn) + Vector3d(0.5, 0.5, 1.5)
            entity = spawnWorld.plugins.worldType.newPlayer(spawnWorld,
                    name(), skin().checksum, this)
            entity.setPos(spawnPos)
            isNew = true
        }
        this.entity = entity
        send(PacketSetWorld(registry, entity.world, entity))
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
        events.fire(event)
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
                    pos3d.x.floorToInt(), pos3d.y.floorToInt(),
                    pos3d.z.floorToInt(), packet.isChunkContent)) {
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

    // TODO: @Throws(IOException::class)
    protected abstract fun transmit(packet: PacketClient)

    protected fun removeEntity() {
        val entity = this.entity
        if (entity != null) {
            entity.world.removePlayer(entity)
            save()
        }
    }

    protected fun close() {
        removeEntity()
        events.disable()
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
