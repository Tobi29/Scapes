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

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.AtomicBoolean
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.packets.PacketEntityComponentData
import org.tobi29.scapes.packets.PacketEntityMetaData

interface EntityClient : Entity {
    val registry: Registries
    val world: WorldClient

    fun setEntityID(uuid: UUID)

    fun read(map: TagMap)

    fun metaData(category: String): MutableTagMap

    fun update(delta: Double) {}

    fun processPacket(packet: PacketEntityComponentData)

    fun processPacket(packet: PacketEntityMetaData)

    companion object {
        fun make(id: Int,
                 world: WorldClient): EntityClient {
            return world.registry.get<EntityType<*, *>>("Core",
                    "Entity")[id].createClient(world)
        }
    }
}

private val ATTACH_MODEL_LISTENER_TOKEN = ListenerToken("Scapes:EntityModel")
fun EntityClient.attachModel(supplier: suspend () -> EntityModel) {
    val mutex = Mutex()
    val attached = AtomicBoolean(false)
    onAddedToWorld[ATTACH_MODEL_LISTENER_TOKEN] = {
        launch(world.taskExecutor) {
            if (attached.compareAndSet(false, true)) {
                mutex.withLock {
                    registerComponent(EntityModel.COMPONENT, supplier())
                }
            }
        }
    }
    onRemovedFromWorld[ATTACH_MODEL_LISTENER_TOKEN] = {
        launch(world.taskExecutor) {
            if (attached.compareAndSet(true, false)) {
                mutex.withLock {
                    unregisterComponent(EntityModel.COMPONENT)
                }
            }
        }
    }
}
