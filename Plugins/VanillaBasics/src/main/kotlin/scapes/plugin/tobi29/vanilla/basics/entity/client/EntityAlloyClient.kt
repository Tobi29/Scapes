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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.gui.GuiAlloyInventory
import scapes.plugin.tobi29.vanilla.basics.util.Alloy
import scapes.plugin.tobi29.vanilla.basics.util.read

class EntityAlloyClient(world: WorldClient, pos: Vector3d = Vector3d.ZERO) : EntityAbstractContainerClient(
        world, pos, Inventory(world.registry, 2)) {
    private var alloy = Alloy()
    private var temperature = 0.0

    override fun gui(player: MobPlayerClientMain): Gui? {
        if (player is MobPlayerClientMainVB) {
            return GuiAlloyInventory(this, player, player.game.engine.guiStyle)
        }
        return null
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        tagStructure.getStructure("Alloy")?.let { alloy = read(plugin, it) }
        tagStructure.getDouble("Temperature")?.let { temperature = it }
    }

    fun alloy(): Alloy {
        return alloy
    }
}
