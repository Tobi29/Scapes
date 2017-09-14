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

package org.tobi29.scapes.vanilla.basics.entity.client

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.EntityAbstractClient
import org.tobi29.scapes.entity.client.attachModel
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.model.EntityModelBellows
import org.tobi29.scapes.vanilla.basics.entity.server.EntityBellowsServer

class EntityBellowsClient(type: EntityType<*, *>,
                          world: WorldClient) : EntityAbstractClient(
        type, world, Vector3d.ZERO) {
    private val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
    val face
        get() = EntityBellowsServer.parseFace(world.terrain, pos.intX(),
                pos.intY(), pos.intZ(), plugin.materials.bellows)
    var scale = 0.0
        private set

    init {
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        attachModel { EntityModelBellows(plugin.modelBellowsShared(), this) }
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Scale"]?.toDouble()?.let { scale = it }
    }

    override fun update(delta: Double) {
        scale = (scale + 0.4 * delta) % 2.0
    }
}
