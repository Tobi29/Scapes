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

package scapes.plugin.tobi29.vanilla.basics.material.block.rock

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTextureRegistry
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.block.BlockSimple

class BlockBedrock(materials: VanillaMaterial) : BlockSimple(materials,
        "vanilla.basics.block.Bedrock") {

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return -1.0
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Stone.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int): String {
        return "VanillaBasics:sound/blocks/Stone.ogg"
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/Bedrock.png")
    }

    override fun name(item: ItemStack): String {
        return "Bedrock"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }
}
