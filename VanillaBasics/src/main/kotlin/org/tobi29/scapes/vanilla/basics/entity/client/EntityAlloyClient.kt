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

import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toDouble
import org.tobi29.io.tag.toMap
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.EntityAbstractClient
import org.tobi29.scapes.entity.client.GUI_COMPONENT
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.gui.GuiAlloyInventory
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.toAlloy

class EntityAlloyClient(
        type: EntityType<*, *>,
        world: WorldClient
) : EntityAbstractClient(type, world, Vector3d.ZERO) {
    var alloy = Alloy()
        private set
    private var temperature = 0.0

    init {
        inventories.add("Container", 2)
        registerComponent(GUI_COMPONENT) { player ->
            if (player is MobPlayerClientMainVB) {
                GuiAlloyInventory(this, player, player.game.engine.guiStyle)
            } else null
        }
    }

    override fun read(map: TagMap) {
        super.read(map)
        val plugin = world.plugins.plugin<VanillaBasics>()
        map["Alloy"]?.toMap()?.toAlloy(plugin)?.let { alloy = it }
        map["Temperature"]?.toDouble()?.let { temperature = it }
    }
}
