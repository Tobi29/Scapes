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

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.toMap
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.client.EntityContainerClient
import org.tobi29.scapes.entity.client.MobPlayerClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.model.MobLivingModelHuman

class MobPlayerClientVB constructor(world: WorldClient,
                                    pos: Vector3d = Vector3d.ZERO,
                                    speed: Vector3d = Vector3d.ZERO,
                                    xRot: Double = 0.0,
                                    zRot: Double = 0.0,
                                    nickname: String = "") : MobPlayerClient(
        world, pos, speed, AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 100.0, 100.0,
        nickname), EntityContainerClient {
    private val inventories: InventoryContainer

    init {
        inventories = InventoryContainer()
        inventories.add("Container", Inventory(registry, 40))
        inventories.add("Hold", Inventory(registry, 1))
        rot.setX(xRot)
        rot.setZ(zRot)
    }

    override fun leftWeapon(): ItemStack {
        return inventories.access("Container"
        ) { inventory -> inventory.item(inventorySelectLeft) }
    }

    override fun rightWeapon(): ItemStack {
        return inventories.access("Container"
        ) { inventory -> inventory.item(inventorySelectRight) }
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

    override fun gui(player: MobPlayerClientMain): Gui? {
        // TODO: Trade or steal UI maybe?
        return null
    }

    override fun inventories(): InventoryContainer {
        return inventories
    }

    override fun createModel(): MobLivingModelHuman? {
        val texture = world.scene.skinStorage()[skin]
        return MobLivingModelHuman(world.game.modelHumanShared(), this, texture)
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toMap()?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag[id]?.toMap()?.let {
                    inventory.read(it)
                }
            }
        }
    }
}
