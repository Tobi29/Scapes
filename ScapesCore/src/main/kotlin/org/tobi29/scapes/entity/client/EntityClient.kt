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
package org.tobi29.scapes.entity.client

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.ListenerOwner
import org.tobi29.scapes.engine.utils.ListenerOwnerHandle
import org.tobi29.scapes.engine.utils.io.tag.MultiTag
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.packets.PacketEntityMetaData
import java.util.*

open class EntityClient(val world: WorldClient, pos: Vector3d) : Entity, MultiTag.Readable, ListenerOwner {
    protected val registry: GameRegistry
    protected val pos: MutableVector3d
    override val listenerOwner = ListenerOwnerHandle {
        !world.disposed() && world.hasEntity(this)
    }
    protected var uuid: UUID = UUID.randomUUID()
    protected var metaData = TagStructure()

    init {
        registry = world.registry
        this.pos = MutableVector3d(pos)
    }

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

    override fun read(tagStructure: TagStructure) {
        tagStructure.getMultiTag("Pos", pos)
        tagStructure.getStructure("MetaData")?.let { metaData = it }
    }

    fun metaData(category: String): TagStructure {
        return metaData.structure(category)
    }

    open fun update(delta: Double) {
    }

    open fun createModel(): EntityModel? {
        return null
    }

    fun processPacket(packet: PacketEntityMetaData) {
        metaData.setStructure(packet.category(), packet.tagStructure())
    }

    companion object {

        fun make(id: Int,
                 world: WorldClient): EntityClient {
            return world.registry.getAsymSupplier<WorldServer, EntityServer, WorldClient, EntityClient>(
                    "Core", "Entity").get2(id)(world)
        }
    }
}
