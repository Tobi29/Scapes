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

package scapes.plugin.tobi29.vanilla.basics.entity.server

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.ReadWriteTagMap
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.toFloat
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.packets.PacketEntityChange
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemFuel
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemHeatable

abstract class EntityAbstractFurnaceServer(world: WorldServer,
                                           pos: Vector3d,
                                           inventory: Inventory,
                                           fuel: Int,
                                           protected val items: Int,
                                           protected var maximumTemperature: Float,
                                           protected val temperatureFalloff: Float,
                                           protected val fuelHeat: Int,
                                           protected val fuelTier: Int) : EntityAbstractContainerServer(
        world, pos, inventory) {
    protected val fuel: FloatArray
    protected val fuelTemperature: FloatArray
    protected var temperature = 0.0f
    private var heatWait = 0.05

    init {
        this.fuel = FloatArray(fuel)
        fuelTemperature = FloatArray(fuel)
    }

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
            map["Fuel$i"]?.toFloat()?.let { fuel[i] = it }
        }
        for (i in fuelTemperature.indices) {
            map["FuelTemperature$i"]?.toFloat()?.let { fuelTemperature[i] = it }
        }
        map["Temperature"]?.toFloat()?.let { temperature = it }
    }

    fun temperature(): Float {
        return temperature
    }

    override fun update(delta: Double) {
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        heatWait -= delta
        while (heatWait <= 0.0) {
            heatWait += 0.05
            temperature /= temperatureFalloff
            temperature = max(10f, temperature)
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
                                world.send(PacketEntityChange(this))
                            }
                        }
                    }
                }
                val max = items + fuel.size + 1
                for (i in fuel.size + 1..max - 1) {
                    if (inventory.item(i).amount() == 1) {
                        val type = inventory.item(i).material()
                        if (type is ItemHeatable) {
                            type.heat(inventory.item(i), temperature)
                        }
                    } else if (inventory.item(i).isEmpty && !inventory.item(
                            fuel.size).isEmpty) {
                        val j = i
                        inventory.item(fuel.size).take(1)?.let { item ->
                            inventory.item(j).stack(item)
                            world.send(PacketEntityChange(this))
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
    }
}
