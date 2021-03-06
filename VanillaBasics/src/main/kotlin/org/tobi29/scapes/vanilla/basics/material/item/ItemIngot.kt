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

import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.data
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.math.floorToInt
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.*
import org.tobi29.scapes.vanilla.basics.util.type

class ItemIngot(type: VanillaMaterialType) : VanillaItem(type),
        ItemMetalI<VanillaItem> {
    private val modelsShaped = ConcurrentHashMap<AlloyType, ItemModel>()
    private val modelsRaw = ConcurrentHashMap<AlloyType, ItemModel>()
    private var textureShaped: TerrainTexture? = null
    private var textureRaw: TerrainTexture? = null
    private var textureMold: TerrainTexture? = null
    private var modelMold: ItemModel? = null

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureShaped = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Shaped.png")
        textureRaw = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Raw.png")
        textureMold = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/MoldFilled.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        modelMold = ItemModelSimple(textureMold!!, 1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: TypedItem<VanillaItem>,
                        gl: GL,
                        shader: Shader) {
        val alloyType = alloy(item).type(plugin)
        when (item.data) {
            1 -> modelShaped(alloyType).render(gl, shader)
            else -> {
                modelRaw(alloyType).render(gl, shader)
                modelMold?.render(gl, shader)
            }
        }
    }

    override fun renderInventory(item: TypedItem<VanillaItem>,
                                 gl: GL,
                                 shader: Shader) {
        val alloyType = alloy(item).type(plugin)
        when (item.data) {
            1 -> modelShaped(alloyType).renderInventory(gl, shader)
            else -> {
                modelRaw(alloyType).renderInventory(gl, shader)
                modelMold?.renderInventory(gl, shader)
            }
        }
    }

    override fun name(item: TypedItem<VanillaItem>): String {
        val name = StringBuilder(50)
        if (item.data == 0) {
            name.append("Unshaped ")
        }
        val alloy = alloy(item)
        val alloyType = alloy.type(plugin)
        name.append(alloyType.ingotName)
        val temperature = temperature(item)
        name.append("\nTemp.:").append(temperature.floorToInt()).append("°C")
        if (temperature > (item as TypedItem<ItemIngot>).meltingPoint) {
            name.append("\n - Liquid")
        }
        return name.toString()
    }

    override fun maxStackSize(item: TypedItem<VanillaItem>): Int {
        return 1
    }

    override fun temperatureUpdated(item: TypedItem<VanillaItem>): Item? {
        if ((item as TypedItem<ItemIngot>).temperature >= item.meltingPoint && item.data == 1) {
            return null
        }
        return item
    }

    private fun modelRaw(alloy: AlloyType): ItemModel {
        var model: ItemModel? = modelsRaw[alloy]
        if (model == null) {
            val r = alloy.r
            val g = alloy.g
            val b = alloy.b
            model = ItemModelSimple(textureRaw!!, r, g, b, 1.0)
            modelsRaw.put(alloy, model)
        }
        return model
    }

    private fun modelShaped(alloy: AlloyType): ItemModel {
        var model: ItemModel? = modelsShaped[alloy]
        if (model == null) {
            val r = alloy.r
            val g = alloy.g
            val b = alloy.b
            model = ItemModelSimple(textureShaped!!, r, g, b, 1.0)
            modelsShaped.put(alloy, model)
        }
        return model
    }
}
