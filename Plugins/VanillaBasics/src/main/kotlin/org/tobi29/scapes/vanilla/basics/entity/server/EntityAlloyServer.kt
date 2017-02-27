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
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.packets.PacketEntityChange
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.material.item.ItemIngot
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.readAlloy
import org.tobi29.scapes.vanilla.basics.util.writeAlloy

class EntityAlloyServer(world: WorldServer,
                        pos: Vector3d = Vector3d.ZERO) : EntityAbstractContainerServer(
        world, pos, Inventory(world.registry, 2)) {
    private var metals = Alloy()
    private var temperature = 0.0

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Alloy"] = TagMap { writeAlloy(metals, this) }
        map["Temperature"] = temperature
    }

    override fun read(map: TagMap) {
        super.read(map)
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        map["Alloy"]?.toMap()?.let { metals = readAlloy(plugin, it) }
        map["Temperature"]?.toDouble()?.let { temperature = it }
    }

    public override fun isValidOn(terrain: TerrainServer,
                                  x: Int,
                                  y: Int,
                                  z: Int): Boolean {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        return terrain.type(x, y, z) === materials.alloy
    }

    override fun update(delta: Double) {
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        temperature /= 1.002
        inventories.modify("Container") { inventory ->
            val input = inventory.item(0)
            val inputType = input.material()
            if (inputType is ItemIngot) {
                val alloy = inputType.alloy(input)
                val meltingPoint = alloy.meltingPoint()
                if (inputType.temperature(input) >= meltingPoint) {
                    val alloyType = alloy.type(plugin)
                    for ((key, value) in alloyType.ingredients()) {
                        metals.add(key, value)
                    }
                    input.metaData("Vanilla")["Temperature"]?.toDouble()?.let {
                        temperature = max(temperature, it)
                    }
                    input.clear()
                    input.setMaterial(materials.mold, 1)
                    world.send(PacketEntityChange(this))
                }
            }
            val output = inventory.item(1)
            val outputType = output.material()
            if (outputType === materials.mold && output.data() == 1) {
                input.metaData("Vanilla")["Temperature"]?.toDouble()?.let {
                    temperature = max(temperature, it)
                }
                output.setMaterial(materials.ingot, 0)
                output.metaData("Vanilla")["Temperature"] = temperature
                materials.ingot.setAlloy(output, metals.drain(1.0))
                world.send(PacketEntityChange(this))
            }
        }
    }
}
