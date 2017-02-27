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

package org.tobi29.scapes.vanilla.basics.material.item

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

class ItemCoal(materials: VanillaMaterial) : VanillaItem(materials,
        "vanilla.basics.item.Coal"), ItemFuel {
    private var texture: TerrainTexture? = null
    private var model: ItemModel? = null

    override fun registerTextures(registry: TerrainTextureRegistry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/ore/coal/Coal.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        model = ItemModelSimple(texture, 1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        model?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        model?.renderInventory(gl, shader)
    }

    override fun name(item: ItemStack): String {
        return "Coal"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 16
    }

    override fun fuelTemperature(item: ItemStack): Float {
        return 0.8f
    }

    override fun fuelTime(item: ItemStack): Float {
        return 200.0f
    }

    override fun fuelTier(item: ItemStack): Int {
        return 50
    }
}
