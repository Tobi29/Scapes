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
import org.tobi29.scapes.engine.utils.ComponentStorage
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.math.vector.MutableVector3d
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.entity.ComponentEntity
import org.tobi29.scapes.entity.ComponentSerializable
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.packets.PacketEntityComponentData
import org.tobi29.scapes.packets.PacketEntityMetaData

open class EntityAbstractClient(override final val type: EntityType<*, *>,
                                override final val world: WorldClient,
                                pos: Vector3d) : EntityClient {
    override final val registry = world.registry
    override val componentStorage = ComponentStorage<ComponentEntity>()
    protected val pos = MutableVector3d(pos)
    override final var uuid: UUID = UUID.randomUUID()
        protected set
    protected var metaData = MutableTagMap()
    override final val onAddedToWorld = ConcurrentHashMap<ListenerToken, () -> Unit>()
    override final val onRemovedFromWorld = ConcurrentHashMap<ListenerToken, () -> Unit>()

    override fun getCurrentPos(): Vector3d {
        return pos.now()
    }

    override fun setEntityID(uuid: UUID) {
        this.uuid = uuid
    }

    override fun read(map: TagMap) {
        map["Pos"]?.toMap()?.let {
            pos.set(it)
            getOrNull(EntityModel.COMPONENT)?.setPos(pos.now())
        }
        map["MetaData"]?.toMap()?.let { metaData = it.toMutTag() }
        map["Components"]?.toMap()?.let { componentData ->
            for (component in components) {
                if (component is ComponentSerializable) {
                    componentData[component.id]?.toMap()?.let {
                        component.read(it)
                    }
                }
            }
        }
    }

    override fun metaData(category: String) = metaData.mapMut(category)

    override fun update(delta: Double) {
    }

    override fun processPacket(packet: PacketEntityComponentData) {
        val componentData = packet.tag
        for (component in components) {
            if (component is ComponentSerializable) {
                componentData[component.id]?.toMap()?.let {
                    component.read(it)
                }
            }
        }
    }

    override fun processPacket(packet: PacketEntityMetaData) {
        metaData[packet.category] = packet.tag
    }

    companion object {

        fun make(id: Int,
                 world: WorldClient): EntityClient {
            return world.registry.get<EntityType<*, *>>("Core",
                    "Entity")[id].createClient(world)
        }
    }
}
