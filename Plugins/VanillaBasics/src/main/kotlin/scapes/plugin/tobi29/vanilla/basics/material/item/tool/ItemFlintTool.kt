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
package scapes.plugin.tobi29.vanilla.basics.material.item.tool

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
import scapes.plugin.tobi29.vanilla.basics.material.VanillaMaterial
import scapes.plugin.tobi29.vanilla.basics.material.item.VanillaItem
import scapes.plugin.tobi29.vanilla.basics.util.createStoneTool

abstract class ItemFlintTool protected constructor(materials: VanillaMaterial,
                                                   nameID: String) : VanillaItem(
        materials, nameID) {
    private var textureHead: TerrainTexture? = null
    private var textureBuilt: TerrainTexture? = null
    private var modelHead: ItemModel? = null
    private var modelBuilt: ItemModel? = null

    override fun example(data: Int): ItemStack {
        val item = super.example(data)
        createStoneTool(plugin, item, type())
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
            val damage = item.metaData("Vanilla")[
                    "ToolDamage"]?.toDouble() ?: 0.0
            val modifier = if (entity.wieldMode() == WieldMode.DUAL) 1.0 else 2.1
            item.metaData("Vanilla")["ToolDamage"] =
                    damage + (item.metaData("Vanilla")[
                            "ToolDamageAdd"]?.toDouble() ?: 0.0)
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
                "VanillaBasics:image/terrain/tools/head/stone/" + type() +
                        ".png")
        textureBuilt = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/built/stone/" + type() +
                        ".png")
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        modelHead = ItemModelSimple(textureHead, 1.0, 1.0, 1.0, 1.0)
        modelBuilt = ItemModelSimple(textureBuilt, 1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        if (item.data() > 0) {
            modelBuilt?.render(gl, shader)
        } else {
            modelHead?.render(gl, shader)
        }
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        if (item.data() > 0) {
            modelBuilt?.renderInventory(gl, shader)
        } else {
            modelHead?.renderInventory(gl, shader)
        }
    }

    override fun name(item: ItemStack): String {
        val name = StringBuilder(100)
        name.append("Flint ").append(type())
        if (item.data() == 0) {
            name.append(" Head")
        }
        val damage = (1.0 - tanh(
                item.metaData(
                        "Vanilla")["ToolDamage"]?.toDouble() ?: 0.0)) * 100.0
        if (damage > 0.1) {
            name.append("\nDamage: ").append(floor(damage))
        }
        return name.toString()
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 1
    }

    abstract fun type(): String
}
