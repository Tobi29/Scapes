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
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.assert
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.entity.EntityType

open class EntityAbstractServer(override val type: EntityType<*, *>,
                                override val world: WorldServer,
                                pos: Vector3d) : EntityServer {
    protected val spawnListeners: MutableMap<String, () -> Unit> = ConcurrentHashMap()
    protected val updateListeners: MutableMap<String, (Double) -> Unit> = ConcurrentHashMap()
    override val registry = world.registry
    protected val pos = MutableVector3d(pos)
    override var uuid: UUID = UUID.randomUUID()
        protected set
    protected var metaData = MutableTagMap()

    override fun getUUID(): UUID {
        return uuid
    }


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
    }

    override fun read(map: TagMap) {
        map["Pos"]?.toMap()?.let { pos.set(it) }
        map["MetaData"]?.toMap()?.let { metaData = it.toMutTag() }
    }

    override fun metaData(category: String) = metaData.mapMut(category)

    override fun updateListeners(delta: Double) {
        updateListeners.values.forEach { it(delta) }
    }

    override fun onSpawn(id: String,
                         listener: () -> Unit) {
        spawnListeners[id] = listener
    }

    override fun onSpawn() {
        spawnListeners.values.forEach { it() }
    }

    override fun onUpdate(id: String,
                          listener: (Double) -> Unit) {
        updateListeners[id] = listener
    }

    companion object {
        fun make(id: Int,
                 world: WorldServer): EntityServer {
            return world.registry.get<EntityType<*, *>>("Core",
                    "Entity")[id].createServer(world)
        }
    }
}
