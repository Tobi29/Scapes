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
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.packets.PacketEntityChange
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics

class EntityBloomeryServer(world: WorldServer,
                           pos: Vector3d = Vector3d.ZERO) : EntityAbstractFurnaceServer(
        world, pos, Inventory(world.registry, 14), 4, 9, 800.0f, 1.004f, 4,
        50) {
    private var hasBellows = false

    override fun write(): TagStructure {
        val tag = super.write()
        tag.setBoolean("Bellows", hasBellows)
        return tag
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        hasBellows = tagStructure.getBoolean("Bellows") ?: false
        maximumTemperature = if (hasBellows) Float.POSITIVE_INFINITY else 600.0f
    }

    override fun update(delta: Double) {
        super.update(delta)
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        temperature /= 1.01f
        temperature = max(10f, temperature)
        val xx = pos.intX()
        val yy = pos.intY()
        val zz = pos.intZ()
        val blockOff = materials.bloomery.block(0)
        val blockOn = materials.bloomery.block(1)
        if (temperature > 10) {
            if (world.terrain.block(xx, yy, zz) == blockOff) {
                world.terrain.queue { handle ->
                    if (handle.block(xx, yy, zz) == blockOff) {
                        handle.block(xx, yy, zz, blockOn)
                    }
                }
            }
        } else if (world.terrain.block(xx, yy, zz) == blockOn) {
            world.terrain.queue { handle ->
                if (handle.block(xx, yy, zz) == blockOn) {
                    handle.block(xx, yy, zz, blockOff)
                }
            }
        }
    }

    override fun isValidOn(terrain: TerrainServer,
                           x: Int,
                           y: Int,
                           z: Int): Boolean {
        val plugin = terrain.world.plugins.plugin(
                "VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        return terrain.type(x, y, z) === materials.bloomery
    }

    fun updateBellows(terrain: TerrainServer) {
        val world = terrain.world
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        var hasBellows = false
        val xx = pos.intX()
        val yy = pos.intY()
        val zz = pos.intZ()
        if (terrain.block(xx, yy - 1, zz) == materials.bellows.block(
                Face.NORTH.value)) {
            hasBellows = true
        }
        if (terrain.block(xx + 1, yy, zz) == materials.bellows.block(
                Face.EAST.value)) {
            hasBellows = true
        }
        if (terrain.block(xx, yy + 1, zz) == materials.bellows.block(
                Face.SOUTH.value)) {
            hasBellows = true
        }
        if (terrain.block(xx - 1, yy, zz) == materials.bellows.block(
                Face.WEST.value)) {
            hasBellows = true
        }
        this.hasBellows = hasBellows
        maximumTemperature = if (hasBellows) Float.POSITIVE_INFINITY else 600.0f
        world.send(PacketEntityChange(this))
    }
}
