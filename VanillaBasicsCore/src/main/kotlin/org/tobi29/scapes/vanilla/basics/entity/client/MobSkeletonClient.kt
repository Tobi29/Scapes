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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.math.AABB
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.client.MobLivingEquippedClient
import org.tobi29.scapes.entity.client.attachModel
import org.tobi29.scapes.entity.model.MobLivingModelHuman

class MobSkeletonClient(type: EntityType<*, *>,
                        world: WorldClient) : MobLivingEquippedClient(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 20.0, 30.0) {
    init {
        val texture = world.game.engine.graphics.textures["VanillaBasics:image/entity/mob/Skeleton"]
        registerComponent(CreatureType.COMPONENT, CreatureType.MONSTER)
        attachModel {
            MobLivingModelHuman(world.game.modelHumanShared(), this,
                    texture.getAsync(),
                    true, false)
        }
        onDeath[SKELETON_LISTENER_TOKEN] = {
            texture.tryGet()?.let { texture ->
                MobLivingModelHuman.particles(world.game.modelHumanShared(),
                        world.scene.particles(), pos.now(), speed.now(),
                        rot.now(), texture, true, false)
            }
        }
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.7)
    }

    override fun leftWeapon(): ItemStack {
        return ItemStack(world.plugins)
    }

    override fun rightWeapon(): ItemStack {
        return ItemStack(world.plugins)
    }
}

private val SKELETON_LISTENER_TOKEN = ListenerToken("VanillaBasics:Skeleton")
