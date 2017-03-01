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
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.toDouble
import org.tobi29.scapes.engine.utils.io.tag.toInt
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.model.EntityModel
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.model.EntityModelBellows

class EntityBellowsClient(world: WorldClient,
                          pos: Vector3d = Vector3d.ZERO,
                          face: Face = Face.NONE) : EntityClient(
        world, pos) {
    var face: Face = face
        private set
    var scale = 0.0
        private set

    override fun read(map: TagMap) {
        super.read(map)
        map["Scale"]?.toDouble()?.let { scale = it }
        map["Face"]?.toInt()?.let { face = Face[it] }
    }

    override fun update(delta: Double) {
        scale = (scale + 0.4 * delta) % 2.0
    }

    override fun createModel(): EntityModel? {
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        return EntityModelBellows(plugin.modelBellowsShared(), this)
    }
}
