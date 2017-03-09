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
package org.tobi29.scapes.vanilla.basics.material.item.tool

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.toDouble
import org.tobi29.scapes.engine.utils.io.tag.toInt
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.engine.utils.math.tanh
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.vanilla.basics.material.AlloyType
import org.tobi29.scapes.vanilla.basics.material.ItemMetal
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem
import org.tobi29.scapes.vanilla.basics.util.createTool
import java.util.concurrent.ConcurrentHashMap

abstract class ItemMetalTool protected constructor(materials: VanillaMaterial,
                                                   nameID: String) : VanillaItem(
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
                       terrain: TerrainServer.TerrainMutable,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face): Double {
        if (item.data() > 0) {
            val damage = item.metaData("Vanilla")[
                    "ToolDamage"]?.toDouble() ?: 0.0
            val modifier = if (entity.wieldMode() == WieldMode.DUAL) 1.0 else 2.1
            item.metaData("Vanilla")["ToolDamage"] = damage + (item.metaData(
                    "Vanilla")["ToolDamageAdd"]?.toDouble() ?: 0.0)
            return (item.metaData("Vanilla")[
                    "ToolEfficiency"]?.toDouble() ?: 0.0) *
                    (1.0 - tanh(damage)) * modifier
        } else {
            return 0.0
        }
    }

    override fun click(entity: MobPlayerServer,
                       item: ItemStack,
                       hit: MobServer): Double {
        if (item.data() > 0) {
            val damage = item.metaData(
                    "Vanilla")["ToolDamage"]?.toDouble() ?: 0.0
            val modifier = if (entity.wieldMode() == WieldMode.DUAL) 1.0 else 2.1
            item.metaData("Vanilla")["ToolDamage"] = damage
            return (item.metaData(
                    "Vanilla")["ToolStrength"]?.toDouble() ?: 0.0) *
                    (1.0 - tanh(damage)) * modifier
        } else {
            return 0.0
        }
    }

    override fun toolLevel(item: ItemStack): Int {
        return item.metaData("Vanilla")["ToolLevel"]?.toInt() ?: 0
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
        name.append(alloyType.name).append(' ').append(type())
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
                item.metaData("Vanilla")[
                        "ToolDamage"]?.toDouble() ?: 0.0)) * 100.0
        if (damage > 0.1) {
            name.append("\nDamage: ").append(floor(damage))
        }
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }

    abstract fun type(): String

    override fun temperatureUpdated(item: ItemStack) {
        if (temperature(item) >= meltingPoint(item)) {
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

    private fun modelHead(alloyType: AlloyType): ItemModel {
        var model: ItemModel? = modelsHead[alloyType]
        if (model == null) {
            val r = alloyType.r
            val g = alloyType.g
            val b = alloyType.b
            model = ItemModelSimple(textureHead, r, g, b, 1.0)
            modelsHead.put(alloyType, model)
        }
        return model
    }

    private fun modelBuilt(alloyType: AlloyType): ItemModel {
        var model: ItemModel? = modelsBuilt[alloyType]
        if (model == null) {
            val r = alloyType.r
            val g = alloyType.g
            val b = alloyType.b
            model = ItemModelSimple(textureBuilt, r, g, b, 1.0)
            modelsBuilt.put(alloyType, model)
        }
        return model
    }
}
