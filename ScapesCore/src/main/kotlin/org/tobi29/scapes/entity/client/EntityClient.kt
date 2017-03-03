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
package org.tobi29.scapes.entity.client

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.ListenerOwner
import org.tobi29.scapes.engine.utils.ListenerOwnerHandle
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.packets.PacketEntityMetaData
import java.util.*

open class EntityClient(id: String,
                        val world: WorldClient,
                        pos: Vector3d) : Entity, ListenerOwner {
    val registry = world.registry
    override val type = Entity.of(registry, id)
    protected val pos = MutableVector3d(pos)
    override val listenerOwner = ListenerOwnerHandle {
        !world.disposed() && world.hasEntity(this)
    }
    protected var uuid: UUID = UUID.randomUUID()
    protected var metaData = MutableTagMap()

    override fun getUUID(): UUID {
        return uuid
    }

    override fun getCurrentPos(): Vector3d {
        return pos.now()
    }

    fun setEntityID(uuid: UUID) {
        this.uuid = uuid
    }

    fun world(): WorldClient {
        return world
    }

    open fun read(map: TagMap) {
        map["Pos"]?.toMap()?.let { pos.set(it) }
        map["MetaData"]?.toMap()?.let { metaData = it.toMutTag() }
    }

    fun metaData(category: String) = metaData.mapMut(category)

    open fun update(delta: Double) {
    }

    open fun createModel(): EntityModel? {
        return null
    }

    fun processPacket(packet: PacketEntityMetaData) {
        metaData[packet.category] = packet.tag
    }

    companion object {

        fun make(id: Int,
                 world: WorldClient): EntityClient {
            return world.registry.get<EntityType>("Core",
                    "Entity")[id].createClient(world)
        }
    }
}
