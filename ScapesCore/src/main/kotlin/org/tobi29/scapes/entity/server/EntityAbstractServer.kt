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

package org.tobi29.scapes.entity.server

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.ComponentStorage
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.assert
import org.tobi29.scapes.engine.math.vector.MutableVector3d
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.entity.ComponentEntity
import org.tobi29.scapes.entity.ComponentSerializable
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken

open class EntityAbstractServer(override final val type: EntityType<*, *>,
                                override final val world: WorldServer,
                                pos: Vector3d) : EntityServer {
    override final val registry = world.registry
    override val componentStorage = ComponentStorage<ComponentEntity>()
    protected val pos = MutableVector3d(pos)
    override final var uuid: UUID = UUID.randomUUID()
        protected set
    protected var metaData = MutableTagMap()
    override final val onAddedToWorld = ConcurrentHashMap<ListenerToken, () -> Unit>()
    override final val onRemovedFromWorld = ConcurrentHashMap<ListenerToken, () -> Unit>()
    override final val onSpawn = ConcurrentHashMap<ListenerToken, () -> Unit>()
    override final val onUpdate = ConcurrentHashMap<ListenerToken, (Double) -> Unit>()

    override fun setEntityID(uuid: UUID) {
        this.uuid = uuid
    }

    override fun getCurrentPos(): Vector3d {
        return pos.now()
    }

    override fun setPos(pos: Vector3d) {
        assert { !world.hasEntity(this) }
        synchronized(this.pos) {
            this.pos.set(pos)
        }
    }

    override fun write(map: ReadWriteTagMap) {
        map["Pos"] = pos.now().toTag()
        map["MetaData"] = metaData.toTag()
        map["Components"] = componentData()
    }

    override fun read(map: TagMap) {
        map["Pos"]?.toMap()?.let { pos.set(it) }
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

    override fun componentData() = TagMap {
        for (component in components) {
            if (component is ComponentSerializable) {
                this[component.id] = component.toTag()
            }
        }
    }

    override fun metaData(category: String) = metaData.mapMut(category)

    override final fun updateListeners(delta: Double) {
        onUpdate.values.forEach { it(delta) }
    }

    override final fun spawn() {
        onSpawn.values.forEach { it() }
    }

    companion object {
        fun make(id: Int,
                 world: WorldServer): EntityServer {
            return world.registry.get<EntityType<*, *>>("Core",
                    "Entity")[id].createServer(world)
        }
    }
}
