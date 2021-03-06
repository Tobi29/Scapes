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

package org.tobi29.scapes.vanilla.basics.material.block.rock

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimple

class BlockBedrock(type: VanillaMaterialType) : BlockSimple(type) {
    override fun resistance(item: Item?,
                            data: Int) = -1.0

    override fun footStepSound(data: Int) = "VanillaBasics:sound/footsteps/Stone.ogg"

    override fun breakSound(item: Item?,
                            data: Int) = "VanillaBasics:sound/blocks/Stone.ogg"

    override fun registerTextures(registry: TerrainTextureRegistry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/Bedrock.png")
    }

    override fun name(item: TypedItem<BlockType>) = "Bedrock"

    override fun maxStackSize(item: TypedItem<BlockType>) = 16
}
