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

package org.tobi29.scapes.entity.model

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.math.AABB
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.entity.ComponentEntity
import org.tobi29.scapes.entity.ComponentTypeRegisteredEntity
import org.tobi29.scapes.entity.client.EntityClient

interface EntityModel : ComponentEntity {
    val entity: EntityClient

    override fun init() {
        entity.world.addEntityModel(this)
    }

    override fun dispose() {
        entity.world.removeEntityModel(this)
    }

    fun pos(): Vector3d

    fun setPos(pos: Vector3d)

    fun shapeAABB(aabb: AABB)

    fun renderUpdate(delta: Double)

    fun render(gl: GL,
               world: WorldClient,
               cam: Cam,
               shader: Shader)

    companion object {
        val COMPONENT = ComponentTypeRegisteredEntity<EntityClient, EntityModel>()
    }
}
