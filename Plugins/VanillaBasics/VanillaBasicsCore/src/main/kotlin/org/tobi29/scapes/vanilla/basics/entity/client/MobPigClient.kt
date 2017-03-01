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
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.client.MobLivingClient
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.model.MobLivingModelPig

class MobPigClient(world: WorldClient,
                   pos: Vector3d = Vector3d.ZERO,
                   speed: Vector3d = Vector3d.ZERO,
                   xRot: Double = 0.0,
                   zRot: Double = 0.0) : MobLivingClient(
        world, pos, speed, AABB(-0.45, -0.45, -0.6875, 0.45, 0.45, 0.375), 20.0,
        30.0) {

    init {
        rot.setX(xRot)
        rot.setZ(zRot)
    }

    override fun creatureType(): CreatureType {
        return CreatureType.CREATURE
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.2)
    }

    override fun createModel(): MobLivingModelPig? {
        val texture = world.game.engine.graphics.textures()["VanillaBasics:image/entity/mob/Pig"]
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        return MobLivingModelPig(plugin.modelPigShared(), this, texture)
    }
}
