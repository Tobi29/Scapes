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

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.ConcurrentMap
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.TagMapWrite
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken

interface EntityServer : Entity, TagMapWrite {
    val registry: Registries
    val world: WorldServer
    val onSpawn: ConcurrentMap<ListenerToken, () -> Unit>
    val onUpdate: ConcurrentMap<ListenerToken, (Double) -> Unit>

    fun setEntityID(uuid: UUID)

    fun setPos(pos: Vector3d)

    fun read(map: TagMap)

    fun metaData(category: String): MutableTagMap

    fun componentData(): TagMap

    fun updateListeners(delta: Double)

    fun update(delta: Double) {}

    fun updateTile(terrain: TerrainServer,
                   x: Int,
                   y: Int,
                   z: Int,
                   data: Int) {
    }

    fun tickSkip(oldTick: Long,
                 newTick: Long) {
    }

    fun spawn()

    fun onUnload() {}

    companion object {
        fun make(id: Int,
                 world: WorldServer): EntityServer {
            return world.registry.get<EntityType<*, *>>("Core",
                    "Entity")[id].createServer(world)
        }
    }
}
