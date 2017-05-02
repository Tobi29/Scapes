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

package org.tobi29.scapes.vanilla.basics.material.block.structural

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.threadLocalRandom
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimpleDataTextured
import org.tobi29.scapes.vanilla.basics.material.update.UpdateStrawDry

class BlockStraw(type: VanillaMaterialType) : BlockSimpleDataTextured(type) {
    override fun place(terrain: TerrainMutableServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        val random = threadLocalRandom()
        terrain.addDelayedUpdate(
                UpdateStrawDry(player.world.registry).set(x, y, z,
                        random.nextDouble() * 800.0 + 800.0))
        return true
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return 0.1
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        return listOf(ItemStack(materials.grassBundle, data, 2))
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Grass.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Foliage.ogg"
    }

    override fun lightTrough(data: Int) = -5

    override fun name(item: ItemStack): String {
        when (item.data()) {
            1 -> return "Straw"
            else -> return "Wet Straw"
        }
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }

    override fun types(): Int {
        return TEXTURES.size
    }

    override fun texture(data: Int): String {
        return TEXTURES[data]
    }

    companion object {
        private val TEXTURES = arrayOf(
                "VanillaBasics:image/terrain/structure/WetStraw.png",
                "VanillaBasics:image/terrain/structure/Straw.png")
    }
}
