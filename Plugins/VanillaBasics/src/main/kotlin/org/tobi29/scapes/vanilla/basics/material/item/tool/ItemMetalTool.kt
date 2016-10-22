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
package org.tobi29.scapes.vanilla.basics.material.item.tool

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.engine.utils.math.tanh
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.server.MobItemServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.vanilla.basics.material.AlloyType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.item.ItemMetal
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem
import org.tobi29.scapes.vanilla.basics.util.Alloy
import org.tobi29.scapes.vanilla.basics.util.createTool
import org.tobi29.scapes.vanilla.basics.util.read
import org.tobi29.scapes.vanilla.basics.util.write
import java.util.concurrent.ConcurrentHashMap

abstract class ItemMetalTool protected constructor(materials: VanillaMaterial, nameID: String) : VanillaItem(
        materials, nameID), ItemMetal {
    private val modelsHead = ConcurrentHashMap<AlloyType, ItemModel>()
    private val modelsBuilt = ConcurrentHashMap<AlloyType, ItemModel>()
    private var textureHead: TerrainTexture? = null
    private var textureBuilt: TerrainTexture? = null
    private var textureHandle: TerrainTexture? = null
    private var modelHandle: ItemModel? = null

    override fun example(data: Int): ItemStack {
        val item = materials.ingot.example(data)
        createTool(plugin, item, type())
        return item
    }

    override fun click(entity: MobPlayerServer,
                       item: ItemStack) {
        if (item.data() == 0) {
            val itemHandle = ItemStack(materials.stick, 0)
            entity.inventories().modify("Container") { inventory ->
                if (inventory.canTake(itemHandle)) {
                    inventory.take(itemHandle)
                    item.setData(1)
                }
            }
        }
    }

    override fun click(entity: MobPlayerServer,
                       item: ItemStack,
                       terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face): Double {
        if (item.data() > 0) {
            val damage = item.metaData("Vanilla").getDouble("ToolDamage") ?: 0.0
            val modifier = if (entity.wieldMode() == WieldMode.DUAL) 1.0 else 2.1
            item.metaData("Vanilla").setDouble("ToolDamage",
                    damage + (item.metaData("Vanilla").getDouble(
                            "ToolDamageAdd") ?: 0.0))
            return (item.metaData("Vanilla").getDouble(
                    "ToolEfficiency") ?: 0.0) *
                    (1.0 - tanh(damage)) * modifier
        } else {
            return 0.0
        }
    }

    override fun click(entity: MobPlayerServer,
                       item: ItemStack,
                       hit: MobServer): Double {
        if (item.data() > 0) {
            val damage = item.metaData("Vanilla").getDouble("ToolDamage") ?: 0.0
            val modifier = if (entity.wieldMode() == WieldMode.DUAL) 1.0 else 2.1
            item.metaData("Vanilla").setDouble("ToolDamage", damage)
            return (item.metaData("Vanilla").getDouble("ToolStrength") ?: 0.0) *
                    (1.0 - tanh(damage)) * modifier
        } else {
            return 0.0
        }
    }

    override fun toolLevel(item: ItemStack): Int {
        return item.metaData("Vanilla").getInt("ToolLevel") ?: 0
    }

    override fun toolType(item: ItemStack): String {
        if (item.data() > 0) {
            return type()
        } else {
            return type() + "Head"
        }
    }

    override fun isTool(item: ItemStack): Boolean {
        return true
    }

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textureHead = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/head/metal/" + type() +
                        ".png")
        textureBuilt = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/built/metal/" + type() +
                        ".png")
        textureHandle = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/handle/metal/" + type() +
                        ".png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        modelHandle = ItemModelSimple(textureHandle, 1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        val alloyType = alloy(item).type(plugin)
        if (item.data() > 0) {
            modelHandle?.render(gl, shader)
            modelBuilt(alloyType).render(gl, shader)
        } else {
            modelHead(alloyType).render(gl, shader)
        }
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        val alloyType = alloy(item).type(plugin)
        if (item.data() > 0) {
            modelHandle?.renderInventory(gl, shader)
            modelBuilt(alloyType).renderInventory(gl, shader)
        } else {
            modelHead(alloyType).renderInventory(gl, shader)
        }
    }

    override fun name(item: ItemStack): String {
        val name = StringBuilder(100)
        val alloy = alloy(item)
        val alloyType = alloy.type(plugin)
        name.append(alloyType.name()).append(' ').append(type())
        if (item.data() == 0) {
            name.append(" Head")
        }
        val temperature = temperature(item)
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(floor(temperature)).append(
                    "°C")
            if (temperature > meltingPoint(item)) {
                name.append("\n - Liquid")
            }
        }
        val damage = (1.0 - tanh(
                item.metaData("Vanilla").getDouble(
                        "ToolDamage") ?: 0.0)) * 100.0
        if (damage > 0.1) {
            name.append("\nDamage: ").append(floor(damage))
        }
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }

    abstract fun type(): String

    override fun heat(item: ItemStack,
                      temperature: Float) {
        var currentTemperature = temperature(item)
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f)
        } else {
            currentTemperature += (temperature - currentTemperature) / 400.0f
            item.metaData("Vanilla").setFloat("Temperature", currentTemperature)
            if (currentTemperature >= meltingPoint(item)) {
                val tag = item.metaData("Vanilla")
                tag.remove("ToolEfficiency")
                tag.remove("ToolStrength")
                tag.remove("ToolDamage")
                tag.remove("ToolDamageAdd")
                tag.remove("ToolLevel")
                item.setMaterial(materials.ingot)
                item.setData(0)
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
                        currentTemperature / 4.0f)
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

    private fun modelHead(alloyType: AlloyType): ItemModel {
        var model: ItemModel? = modelsHead[alloyType]
        if (model == null) {
            val r = alloyType.r().toDouble()
            val g = alloyType.g().toDouble()
            val b = alloyType.b().toDouble()
            model = ItemModelSimple(textureHead, r, g, b, 1.0)
            modelsHead.put(alloyType, model)
        }
        return model
    }

    private fun modelBuilt(alloyType: AlloyType): ItemModel {
        var model: ItemModel? = modelsBuilt[alloyType]
        if (model == null) {
            val r = alloyType.r().toDouble()
            val g = alloyType.g().toDouble()
            val b = alloyType.b().toDouble()
            model = ItemModelSimple(textureBuilt, r, g, b, 1.0)
            modelsBuilt.put(alloyType, model)
        }
        return model
    }
}
