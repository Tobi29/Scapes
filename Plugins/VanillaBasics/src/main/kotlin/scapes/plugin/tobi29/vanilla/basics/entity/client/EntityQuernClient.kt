/*
 * Copyright 2012-2016 Tobi29
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
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import scapes.plugin.tobi29.vanilla.basics.gui.GuiQuernInventory

class EntityQuernClient(world: WorldClient, pos: Vector3d = Vector3d.ZERO) : EntityAbstractContainerClient(
        world, pos, Inventory(world.registry, 2)) {

    override fun gui(player: MobPlayerClientMain): Gui? {
        if (player is MobPlayerClientMainVB) {
            return GuiQuernInventory(this, player, player.game.engine.guiStyle)
        }
        return null
    }
}
