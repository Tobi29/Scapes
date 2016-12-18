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
package scapes.plugin.tobi29.vanilla.basics.material.item

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.io.tag.getFloat
import org.tobi29.scapes.engine.utils.io.tag.setFloat
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.entity.server.MobItemServer
import scapes.plugin.tobi29.vanilla.basics.material.AlloyType
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.util.Alloy
import scapes.plugin.tobi29.vanilla.basics.util.read
import scapes.plugin.tobi29.vanilla.basics.util.write
import java.util.concurrent.ConcurrentHashMap

class ItemIngot(materials: VanillaMaterial) : VanillaItem(materials,
        "vanilla.basics.item.Ingot"), ItemMetal {
    private val modelsShaped = ConcurrentHashMap<AlloyType, ItemModel>()
    private val modelsRaw = ConcurrentHashMap<AlloyType, ItemModel>()
    private var textureShaped: TerrainTexture? = null
    private var textureRaw: TerrainTexture? = null
    private var textureMold: TerrainTexture? = null
    private var modelMold: ItemModel? = null

    override fun example(data: Int): ItemStack {
        val item = super.example(data)
        val alloy = Alloy()
        alloy.add(plugin.metalType("Iron"), 1.0)
        setAlloy(item, alloy)
        return item
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureShaped = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Shaped.png")
        textureRaw = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Raw.png")
        textureMold = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/MoldFilled.png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        modelMold = ItemModelSimple(textureMold, 1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        val alloyType = alloy(item).type(plugin)
        when (item.data()) {
            1 -> modelShaped(alloyType).render(gl, shader)
            else -> {
                modelRaw(alloyType).render(gl, shader)
                modelMold?.render(gl, shader)
            }
        }
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        val alloyType = alloy(item).type(plugin)
        when (item.data()) {
            1 -> modelShaped(alloyType).renderInventory(gl, shader)
            else -> {
                modelRaw(alloyType).renderInventory(gl, shader)
                modelMold?.renderInventory(gl, shader)
            }
        }
    }

    override fun name(item: ItemStack): String {
        val name = StringBuilder(50)
        if (item.data() == 0) {
            name.append("Unshaped ")
        }
        val alloy = alloy(item)
        val alloyType = alloy.type(plugin)
        name.append(alloyType.ingotName())
        val temperature = temperature(item)
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(floor(temperature)).append("°C")
            if (temperature > meltingPoint(item)) {
                name.append("\n - Liquid")
            }
        }
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }

    override fun heat(item: ItemStack,
                      temperature: Float) {
        var currentTemperature = temperature(item)
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            currentTemperature += (temperature - currentTemperature) / 400.0f
            item.metaData("Vanilla").setFloat("Temperature", currentTemperature)
            if (currentTemperature >= meltingPoint(item) && item.data() == 1) {
                item.setAmount(0)
            }
        }
    }

    override fun cool(item: ItemStack) {
        val currentTemperature = temperature(item)
        if (currentTemperature < 1) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            item.metaData("Vanilla").setFloat("Temperature",
                    currentTemperature / 1.002f)
        }
    }

    override fun cool(item: MobItemServer) {
        val currentTemperature = temperature(item.item())
        if (currentTemperature < 1) {
            item.item().metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            if (item.isInWater) {
                item.item().metaData("Vanilla").setFloat("Temperature",
                        currentTemperature / 1.1f)
            } else {
                item.item().metaData("Vanilla").setFloat("Temperature",
                        currentTemperature / 1.002f)
            }
        }
    }

    override fun temperature(item: ItemStack): Float {
        return item.metaData("Vanilla").getFloat("Temperature") ?: 0.0f
    }

    override fun alloy(item: ItemStack): Alloy {
        return read(plugin, item.metaData("Vanilla").structure("Alloy"))
    }

    override fun setAlloy(item: ItemStack,
                          alloy: Alloy) {
        item.metaData("Vanilla").setStructure("Alloy", write(alloy))
    }

    private fun modelRaw(alloy: AlloyType): ItemModel {
        var model: ItemModel? = modelsRaw[alloy]
        if (model == null) {
            val r = alloy.r().toDouble()
            val g = alloy.g().toDouble()
            val b = alloy.b().toDouble()
            model = ItemModelSimple(textureRaw, r, g, b, 1.0)
            modelsRaw.put(alloy, model)
        }
        return model
    }

    private fun modelShaped(alloy: AlloyType): ItemModel {
        var model: ItemModel? = modelsShaped[alloy]
        if (model == null) {
            val r = alloy.r().toDouble()
            val g = alloy.g().toDouble()
            val b = alloy.b().toDouble()
            model = ItemModelSimple(textureShaped, r, g, b, 1.0)
            modelsShaped.put(alloy, model)
        }
        return model
    }
}
