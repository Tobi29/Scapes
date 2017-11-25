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
package org.tobi29.scapes.entity

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.engine.utils.*
import org.tobi29.scapes.engine.math.AABB
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.TagMapWrite

interface Entity : ComponentHolder<ComponentEntity> {
    val type: EntityType<*, *>
    override val componentStorage: ComponentStorage<ComponentEntity>
    val uuid: UUID

    val onAddedToWorld: ConcurrentMap<ListenerToken, () -> Unit>
    val onRemovedFromWorld: ConcurrentMap<ListenerToken, () -> Unit>

    fun getCurrentPos(): Vector3d

    fun getAABB(): AABB {
        val aabb = AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)
        val pos = getCurrentPos()
        aabb.add(pos.x, pos.y, pos.z)
        return aabb
    }

    fun addedToWorld() {
        onAddedToWorld.values.forEach { it() }
    }

    fun removedFromWorld() {
        onRemovedFromWorld.values.forEach { it() }
    }

    companion object {
        fun of(registry: Registries,
               id: Int) =
                registry.get<EntityType<*, *>>("Core", "Entity")[id]

        fun of(registry: Registries,
               id: String) =
                registry.get<EntityType<*, *>>("Core", "Entity")[id]
    }
}

typealias ComponentTypeRegisteredEntity<E, T> = ComponentTypeRegistered<E, T, ComponentEntity>

interface ComponentEntity : ComponentRegistered

interface ComponentSerializable : TagMapWrite {
    val id: String

    fun read(map: TagMap)
}

class ListenerToken(val id: String) {
    override fun toString() = id
}
