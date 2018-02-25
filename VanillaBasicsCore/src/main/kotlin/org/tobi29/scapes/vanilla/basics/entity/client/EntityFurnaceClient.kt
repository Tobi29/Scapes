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
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.GUI_COMPONENT
import org.tobi29.scapes.vanilla.basics.gui.GuiFurnaceInventory

class EntityFurnaceClient(
        type: EntityType<*, *>,
        world: WorldClient
) : EntityAbstractFurnaceClient(type, world, Vector3d.ZERO, 4, 3) {
    init {
        inventories.add("Container", 8)
        registerComponent(GUI_COMPONENT) { player ->
            if (player is MobPlayerClientMainVB) {
                GuiFurnaceInventory(this, player, player.game.engine.guiStyle)
            } else null
        }
    }
}
