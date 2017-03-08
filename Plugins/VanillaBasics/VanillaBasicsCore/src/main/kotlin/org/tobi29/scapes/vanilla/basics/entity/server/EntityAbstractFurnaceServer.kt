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

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.ReadWriteTagMap
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.toDouble
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.packets.PacketEntityChange
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.ItemFuel
import org.tobi29.scapes.vanilla.basics.material.ItemHeatable

abstract class EntityAbstractFurnaceServer(
        type: EntityType<*, *>,
        world: WorldServer,
        pos: Vector3d,
        inventory: Inventory,
        fuel: Int,
        protected val items: Int,
        protected var maximumTemperature: Double,
        protected val temperatureFalloff: Double,
        protected val fuelHeat: Double,
        protected val fuelTier: Int,
        private val beforeHeatUpdate: (Inventory, ItemStack) -> Unit = { _, _ -> }
) : EntityAbstractContainerServer(type, world, pos, inventory) {
    protected val fuel = DoubleArray(fuel)
    protected val fuelTemperature = DoubleArray(fuel)
    var temperature = 0.0
        protected set
    private var heatWait = 0.05
    private var updateWait = 1.0

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        for (i in fuel.indices) {
            map["Fuel$i"] = fuel[i]
        }
        for (i in fuelTemperature.indices) {
            map["FuelTemperature$i"] = fuelTemperature[i]
        }
        map["Temperature"] = temperature
    }

    override fun read(map: TagMap) {
        super.read(map)
        for (i in fuel.indices) {
            map["Fuel$i"]?.toDouble()?.let { fuel[i] = it }
        }
        for (i in fuelTemperature.indices) {
            map["FuelTemperature$i"]?.toDouble()?.let { fuelTemperature[i] = it }
        }
        map["Temperature"]?.toDouble()?.let { temperature = it }
    }

    override fun update(delta: Double) {
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        heatWait -= delta
        while (heatWait <= 0.0) {
            heatWait += 0.05
            temperature /= temperatureFalloff
            temperature = max(10.0, temperature)
            inventories.modify("Container") { inventory ->
                for (i in fuel.indices) {
                    if (fuel[i] > 0) {
                        temperature += fuelTemperature[i]
                        fuel[i]--
                    } else {
                        val item = inventory.item(i)
                        val material = item.material()
                        if (material is ItemFuel) {
                            if (material.fuelTier(item) >= fuelTier) {
                                this.fuel[i] = material.fuelTime(
                                        item) * fuelHeat
                                fuelTemperature[i] = material.fuelTemperature(
                                        item) * fuelHeat
                                inventory.item(i).take(1)
                            }
                        }
                    }
                }
                val beforeUpdate: (ItemStack) -> Unit = {
                    beforeHeatUpdate(inventory, it)
                }
                for (i in fuel.size + 1..fuel.size + items) {
                    if (inventory.item(i).amount() == 1) {
                        val type = inventory.item(i).material()
                        if (type is ItemHeatable) {
                            type.heat(inventory.item(i), temperature,
                                    beforeUpdate = beforeUpdate)
                        }
                    } else if (inventory.item(i).isEmpty && !inventory.item(
                            fuel.size).isEmpty) {
                        val j = i
                        inventory.item(fuel.size).take(1)?.let { item ->
                            inventory.item(j).stack(item)
                        }
                    }
                }
            }
            if (temperature > maximumTemperature) {
                world.terrain.queue { handler ->
                    handler.typeData(pos.intX(), pos.intY(), pos.intZ(),
                            materials.air, 0)
                }
            }
        }
        updateWait -= delta
        while (updateWait <= 0.0) {
            updateWait += 0.05
            world.send(PacketEntityChange(registry, this))
        }
    }
}
