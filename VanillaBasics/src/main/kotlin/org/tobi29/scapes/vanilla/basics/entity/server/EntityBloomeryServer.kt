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
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.math.Face
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.packets.PacketEntityChange
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.io.tag.ReadWriteTagMap
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toBoolean
import org.tobi29.io.tag.toTag
import org.tobi29.stdex.math.floorToInt
import kotlin.collections.set

class EntityBloomeryServer(
        type: EntityType<*, *>,
        world: WorldServer
) : EntityAbstractFurnaceServer(type, world, Vector3d.ZERO, 4, 9, 600.0, 1.004,
        4.0, 50) {
    private var hasBellows = false

    init {
        inventories.add("Container", 14)
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Bellows"] = hasBellows.toTag()
    }

    override fun read(map: TagMap) {
        super.read(map)
        hasBellows = map["Bellows"]?.toBoolean() ?: false
        maximumTemperature = if (hasBellows) Double.POSITIVE_INFINITY else 600.0
    }

    override fun update(delta: Double) {
        super.update(delta)
        val plugin = world.plugins.plugin<VanillaBasics>()
        val materials = plugin.materials
        val xx = pos.x.floorToInt()
        val yy = pos.y.floorToInt()
        val zz = pos.z.floorToInt()
        val blockOff = materials.bloomery.block(0)
        val blockOn = materials.bloomery.block(1)
        if (temperature > 80.0) {
            if (world.terrain.block(xx, yy, zz) == blockOff) {
                world.terrain.modify(xx, yy, zz) { handle ->
                    if (handle.block(xx, yy, zz) == blockOff) {
                        handle.block(xx, yy, zz, blockOn)
                    }
                }
            }
        } else if (world.terrain.block(xx, yy, zz) == blockOn) {
            world.terrain.modify(xx, yy, zz) { handle ->
                if (handle.block(xx, yy, zz) == blockOn) {
                    handle.block(xx, yy, zz, blockOff)
                }
            }
        }
    }

    override fun isValidOn(terrain: Terrain,
                           x: Int,
                           y: Int,
                           z: Int): Boolean {
        val plugin = world.plugins.plugin<VanillaBasics>()
        val materials = plugin.materials
        return terrain.type(x, y, z) == materials.bloomery
    }

    fun updateBellows(terrain: TerrainServer) {
        val world = terrain.world
        val plugin = world.plugins.plugin<VanillaBasics>()
        val materials = plugin.materials
        var hasBellows = false
        val xx = pos.x.floorToInt()
        val yy = pos.y.floorToInt()
        val zz = pos.z.floorToInt()
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
        maximumTemperature = if (hasBellows) Double.POSITIVE_INFINITY else 600.0
        world.send(PacketEntityChange(registry, this))
    }
}
