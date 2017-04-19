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

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.packets.PacketEntityMetaData

interface EntityClient : Entity {
    val registry: Registries
    val world: WorldClient
    val uuid: UUID

    fun setEntityID(uuid: UUID)

    fun read(map: TagMap)

    fun metaData(category: String): MutableTagMap

    fun update(delta: Double) {
    }

    fun createModel(): EntityModel? {
        return null
    }

    fun processPacket(packet: PacketEntityMetaData)

    companion object {
        fun make(id: Int,
                 world: WorldClient): EntityClient {
            return world.registry.get<EntityType<*, *>>("Core",
                    "Entity")[id].createClient(world)
        }
    }
}
