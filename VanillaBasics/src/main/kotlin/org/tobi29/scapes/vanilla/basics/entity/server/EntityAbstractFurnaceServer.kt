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

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.inventory.*
import org.tobi29.scapes.packets.PacketEntityChange
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.*
import org.tobi29.io.tag.ReadWriteTagMap
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toDouble
import org.tobi29.io.tag.toTag
import org.tobi29.stdex.math.floorToInt
import kotlin.collections.indices
import kotlin.collections.set
import kotlin.math.max

abstract class EntityAbstractFurnaceServer(
        type: EntityType<*, *>,
        world: WorldServer,
        pos: Vector3d,
        fuel: Int,
        protected val items: Int,
        protected var maximumTemperature: Double,
        protected val temperatureFalloff: Double,
        protected val fuelHeat: Double,
        protected val fuelTier: Int,
        private val beforeHeatUpdate: (Inventory, TypedItem<ItemTypeHeatable>) -> Item? = { _, it -> it }
) : EntityAbstractContainerServer(type, world, pos) {
    protected val fuel = DoubleArray(fuel)
    protected val fuelTemperature = DoubleArray(fuel)
    var temperature = 0.0
        protected set
    private var heatWait = 0.05
    private var updateWait = 1.0

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        for (i in fuel.indices) {
            map["Fuel$i"] = fuel[i].toTag()
        }
        for (i in fuelTemperature.indices) {
            map["FuelTemperature$i"] = fuelTemperature[i].toTag()
        }
        map["Temperature"] = temperature.toTag()
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
        val plugin = world.plugins.plugin<VanillaBasics>()
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
                        inventory[i].kind<ItemTypeFuel>()?.let { item ->
                            if (item.fuelTier >= fuelTier) {
                                this.fuel[i] = item.fuelTime * fuelHeat
                                fuelTemperature[i] = item.fuelTemperature * fuelHeat
                                inventory[i] = inventory[i].split(1).second
                            }
                        }
                    }
                }
                val beforeUpdate: (TypedItem<ItemTypeHeatable>) -> Item? = {
                    beforeHeatUpdate(inventory, it)
                }
                for (i in fuel.size + 1..fuel.size + items) {
                    val item = inventory[i]
                    item.kind<ItemTypeHeatable>()?.let { itemHeatable ->
                        if (itemHeatable.amount == 1) {
                            inventory[i] = itemHeatable.heat(
                                    temperature = temperature,
                                    beforeUpdate = beforeUpdate)
                        }
                    }
                    if (item.isEmpty()) {
                        val (stack, remaining) = inventory[fuel.size].split(1)
                        inventory[i] = stack
                        inventory[fuel.size] = remaining
                    }
                }
            }
            if (temperature > maximumTemperature) {
                world.terrain.modify(pos.x.floorToInt(), pos.y.floorToInt(),
                        pos.z.floorToInt()) { terrain ->
                    if (isValidOn(terrain, pos.x.floorToInt(),
                            pos.y.floorToInt(), pos.z.floorToInt())) {
                        terrain.typeData(pos.x.floorToInt(), pos.y.floorToInt(),
                                pos.z.floorToInt(), materials.air, 0)
                    }
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
