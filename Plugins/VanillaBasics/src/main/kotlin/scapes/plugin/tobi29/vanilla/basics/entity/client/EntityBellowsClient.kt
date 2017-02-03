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

package scapes.plugin.tobi29.vanilla.basics.entity.client

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getFloat
import org.tobi29.scapes.engine.utils.io.tag.getInt
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.model.EntityModel
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.entity.model.EntityModelBellows

class EntityBellowsClient constructor(world: WorldClient,
                                      pos: Vector3d = Vector3d.ZERO,
                                      private var face: Face = Face.NONE) : EntityClient(
        world, pos) {
    private var scale = 0.0f

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getFloat("Scale")?.let { scale = it }
        tagStructure.getInt("Face")?.let { face = Face[it] }
    }

    override fun update(delta: Double) {
        scale += (0.4f * delta).toFloat()
        scale %= 2f
    }

    override fun createModel(): EntityModel? {
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        return EntityModelBellows(plugin.modelBellowsShared(), this)
    }

    fun scale(): Float {
        return scale
    }

    fun face(): Face {
        return face
    }
}
