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

import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.block.read
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.math.AABB
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.client.MobPlayerClient
import org.tobi29.scapes.entity.client.attachModel
import org.tobi29.scapes.entity.model.MobLivingModelHuman
import org.tobi29.scapes.vanilla.basics.entity.server.ComponentMobLivingServerCondition
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toMap

class MobPlayerClientVB(
        type: EntityType<*, *>,
        world: WorldClient
) : MobPlayerClient(type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 100.0, 100.0, "") {
    init {
        inventories.add("Container", 40)
        inventories.add("Hold", 1)
        val texture = world.scene.skinStorage()[skin]
        registerComponent(
                ComponentMobLivingServerCondition.COMPONENT,
                ComponentMobLivingServerCondition(this))
        attachModel {
            MobLivingModelHuman(world.game.modelHumanShared(), this, texture)
        }
    }

    override fun leftWeapon() =
            inventories.access("Container") {
                it[inventorySelectLeft]
            }

    override fun rightWeapon() =
            inventories.access("Container") {
                it[inventorySelectRight]
            }

    override fun wieldMode(): WieldMode {
        return if (inventorySelectLeft == inventorySelectRight)
            WieldMode.RIGHT
        else
            WieldMode.DUAL
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.63)
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toMap()?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag[id]?.toMap()?.let {
                    inventory.read(world.plugins, it)
                }
            }
        }
    }
}
