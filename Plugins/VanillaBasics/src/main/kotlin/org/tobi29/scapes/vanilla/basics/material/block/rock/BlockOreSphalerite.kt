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

package org.tobi29.scapes.vanilla.basics.material.block.rock

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.material.StoneType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import java.util.*

class BlockOreSphalerite(materials: VanillaMaterial,
                         stoneRegistry: GameRegistry.Registry<StoneType>) : BlockOre(
        materials, "vanilla.basics.block.OreSphalerite", stoneRegistry) {

    override fun destroy(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: ItemStack): Boolean {
        if ("Pickaxe" == item.material().toolType(item) && !canBeBroken(
                item.material().toolLevel(item), data)) {
            terrain.world.dropItem(
                    ItemStack(materials.oreChunk, 3.toShort().toInt()),
                    x + face.x, y + face.y, z + face.z)
            terrain.type(x, y, z, materials.stoneRaw)
            return false
        }
        return true
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        if ("Pickaxe" == item.material().toolType(item) && canBeBroken(
                item.material().toolLevel(item), data)) {
            return Arrays.asList(
                    ItemStack(materials.oreChunk, 3.toShort().toInt()),
                    ItemStack(materials.stoneRock, data,
                            Random().nextInt(4) + 8))
        }
        return emptyList()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 4
    }

    override fun name(item: ItemStack): String {
        return stoneName(item) + " Sphalerite Ore"
    }

    override fun oreTexture(): String {
        return "Sphalerite"
    }
}
