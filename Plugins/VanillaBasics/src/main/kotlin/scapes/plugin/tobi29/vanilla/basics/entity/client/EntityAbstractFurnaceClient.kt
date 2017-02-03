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
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getFloat
import org.tobi29.scapes.engine.utils.io.tag.getInt
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import scapes.plugin.tobi29.vanilla.basics.material.item.ItemHeatable

abstract class EntityAbstractFurnaceClient protected constructor(world: WorldClient,
                                                                 pos: Vector3d,
                                                                 inventory: Inventory,
                                                                 fuel: Int,
                                                                 protected val items: Int,
                                                                 protected var maximumTemperature: Float,
                                                                 protected val temperatureFalloff: Float,
                                                                 protected val fuelHeat: Int,
                                                                 protected val fuelTier: Int) : EntityAbstractContainerClient(
        world, pos, inventory) {
    protected val fuel: IntArray
    protected val fuelTemperature: FloatArray
    protected var temperature = 0.0f
    private var heatWait = 0.05

    init {
        this.fuel = IntArray(fuel)
        fuelTemperature = FloatArray(fuel)
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        for (i in fuel.indices) {
            tagStructure.getInt("Fuel" + i)?.let { fuel[i] = it }
        }
        for (i in fuelTemperature.indices) {
            tagStructure.getFloat(
                    "FuelTemperature" + i)?.let { fuelTemperature[i] = it }
        }
        tagStructure.getFloat("Temperature")?.let { temperature = it }
    }

    fun temperature(): Float {
        return temperature
    }

    override fun update(delta: Double) {
        heatWait -= delta
        while (heatWait <= 0.0) {
            heatWait += 0.05
            temperature /= temperatureFalloff
            temperature = max(10.0f, temperature)
            for (i in fuel.indices) {
                if (fuel[i] > 0) {
                    temperature += fuelTemperature[i]
                    fuel[i]--
                }
            }
            val max = items + fuel.size + 1
            inventories.modify("Container") { inventory ->
                for (i in fuel.size + 1..max - 1) {
                    if (inventory.item(i).amount() == 1) {
                        val type = inventory.item(i).material()
                        if (type is ItemHeatable) {
                            type.heat(inventory.item(i), temperature)
                        }
                    }
                }
            }
        }
    }
}
