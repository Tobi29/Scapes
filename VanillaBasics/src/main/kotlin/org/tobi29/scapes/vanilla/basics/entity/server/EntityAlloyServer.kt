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

package org.tobi29.scapes.vanilla.basics.entity.server

import org.tobi29.scapes.block.copy
import org.tobi29.scapes.block.data
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.packets.PacketEntityChange
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.ItemMetal
import org.tobi29.scapes.vanilla.basics.material.alloy
import org.tobi29.scapes.vanilla.basics.material.copy
import org.tobi29.scapes.vanilla.basics.material.item.tool.ItemMold
import org.tobi29.scapes.vanilla.basics.material.temperature
import org.tobi29.scapes.vanilla.basics.util.*
import org.tobi29.io.tag.ReadWriteTagMap
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toDouble
import org.tobi29.io.tag.toTag
import kotlin.collections.set
import kotlin.math.max

class EntityAlloyServer(
        type: EntityType<*, *>,
        world: WorldServer
) : EntityAbstractContainerServer(type, world, Vector3d.ZERO) {
    private var metals = Alloy()
    private var temperature = 0.0

    init {
        inventories.add("Container", 2)
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Alloy"] = metals.toTag()
        map["Temperature"] = temperature.toTag()
    }

    override fun read(map: TagMap) {
        super.read(map)
        val plugin = world.plugins.plugin<VanillaBasics>()
        map["Alloy"]?.toAlloy(plugin)?.let { metals = it }
        map["Temperature"]?.toDouble()?.let { temperature = it }
    }

    public override fun isValidOn(terrain: Terrain,
                                  x: Int,
                                  y: Int,
                                  z: Int): Boolean {
        val plugin = world.plugins.plugin<VanillaBasics>()
        val materials = plugin.materials
        return terrain.type(x, y, z) == materials.alloy
    }

    override fun update(delta: Double) {
        val plugin = world.plugins.plugin<VanillaBasics>()
        val materials = plugin.materials
        temperature /= 1.002
        inventories.modify("Container") { inventory ->
            inventory[0].kind<ItemMetal>()?.let { input ->
                val alloy = input.alloy
                val meltingPoint = alloy.meltingPoint
                val inputTemperature = input.temperature
                if (inputTemperature >= meltingPoint) {
                    metals += alloy
                    temperature = max(temperature, inputTemperature)
                    inventory[0] = null
                    world.send(PacketEntityChange(registry, this))
                }
            }
            inventory[1].kind<ItemMold>()?.let { item ->
                if (item.type == materials.mold && item.data == 1) {
                    val (drained, _) = metals.drain(1.0)
                            .also { metals = it.second }
                    if (drained != null) {
                        inventory[1] = drained.toIngot(plugin)
                                .copy(data = 1)
                                .copy(temperature = temperature)
                        world.send(PacketEntityChange(registry, this))
                    }
                }
            }
        }
    }
}
