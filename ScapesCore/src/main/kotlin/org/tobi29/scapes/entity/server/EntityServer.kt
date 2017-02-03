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

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.io.tag.MultiTag
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.client.EntityClient
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class EntityServer(val world: WorldServer,
                        pos: Vector3d) : Entity, MultiTag.ReadAndWrite {
    protected val spawnListeners: MutableMap<String, () -> Unit> = ConcurrentHashMap()
    protected val updateListeners: MutableMap<String, (Double) -> Unit> = ConcurrentHashMap()
    protected val registry: GameRegistry
    protected val pos: MutableVector3d
    var uuid: UUID = UUID.randomUUID()
        protected set
    protected var metaData = TagStructure()

    init {
        registry = world.registry
        this.pos = MutableVector3d(pos)
    }

    override fun getUUID(): UUID {
        return uuid
    }


    fun setEntityID(uuid: UUID) {
        this.uuid = uuid
    }

    fun id(registry: GameRegistry): Int {
        return registry.getAsymSupplier<Any, Any, Any, Any>("Core",
                "Entity").id(this)
    }

    fun world(): WorldServer {
        return world
    }

    override fun getCurrentPos(): Vector3d {
        return pos.now()
    }

    override fun write(): TagStructure {
        val tag = TagStructure()
        tag.setMultiTag("Pos", pos)
        tag.setStructure("MetaData", metaData)
        return tag
    }

    override fun read(tagStructure: TagStructure) {
        tagStructure.getMultiTag("Pos", pos)
        tagStructure.getStructure("MetaData")?.let { metaData = it }
    }

    fun metaData(category: String): TagStructure {
        return metaData.structure(category)
    }

    fun updateListeners(delta: Double) {
        updateListeners.values.forEach { it(delta) }
    }

    open fun update(delta: Double) {
    }

    open fun updateTile(terrain: TerrainServer,
                        x: Int,
                        y: Int,
                        z: Int,
                        data: Int) {
    }

    open fun tickSkip(oldTick: Long,
                      newTick: Long) {
    }

    fun onSpawn(id: String,
                listener: () -> Unit) {
        spawnListeners[id] = listener
    }

    fun onSpawn() {
        spawnListeners.values.forEach { it() }
    }

    fun onUpdate(id: String,
                 listener: (Double) -> Unit) {
        updateListeners[id] = listener
    }

    fun onUnload() {
    }

    companion object {
        fun make(id: Int,
                 world: WorldServer): EntityServer {
            return world.registry.getAsymSupplier<WorldServer, EntityServer, WorldClient, EntityClient>(
                    "Core", "Entity").get1(id)(world)
        }

        fun make(id: Int?,
                 world: WorldServer): EntityServer? {
            if (id == null) {
                return null
            }
            return world.registry.getAsymSupplier<WorldServer, EntityServer, WorldClient, EntityClient>(
                    "Core", "Entity").get1(id)(world)
        }
    }
}
