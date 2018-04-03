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

import org.tobi29.io.tag.toDouble
import org.tobi29.io.tag.toInt
import org.tobi29.io.tag.toTag
import org.tobi29.math.Face
import org.tobi29.scapes.block.*
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.ItemStack
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem
import org.tobi29.stdex.math.floorToInt
import kotlin.math.tanh

abstract class ItemFlintTool(type: VanillaMaterialType) : VanillaItem(type) {
    private var textureHead: TerrainTexture? = null
    private var textureBuilt: TerrainTexture? = null
    private var modelHead: ItemModel? = null
    private var modelBuilt: ItemModel? = null

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<VanillaItem>): Item? =
            if (item.data == 0) {
                val itemHandle = ItemStack(type = materials.stick, amount = 1)
                val assemble = entity.inventories.modify(
                        "Container") { inventory ->
                    if (inventory.canTakeAll(itemHandle)) {
                        inventory.take(itemHandle)
                        true
                    } else false
                }
                if (assemble) item.copy(data = 1) else item
            } else item

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<VanillaItem>,
                       terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face): Pair<Item?, Double?> =
            if (item.data > 0) {
                val damage = item.metaData["ToolDamage"]?.toDouble() ?: 0.0
                val modifier = if (entity.wieldMode() == WieldMode.DUAL) 1.0 else 2.1
                item.copy(metaData = (item.metaData + mapOf(
                        "ToolDamage" to (damage + (item.metaData["ToolDamageAdd"]?.toDouble() ?: 0.0)).toTag()
                )).toTag()) to
                        (item.metaData["ToolEfficiency"]?.toDouble() ?: 0.0) * (1.0 - tanh(
                                damage)) * modifier
            } else item to 0.0

    override fun click(entity: MobPlayerServer,
                       item: TypedItem<VanillaItem>,
                       hit: MobServer): Pair<Item?, Double?> =
            if (item.data > 0) {
                val damage = item.metaData["ToolDamage"]?.toDouble() ?: 0.0
                val modifier = if (entity.wieldMode() == WieldMode.DUAL) 1.0 else 2.1
                item.copy(metaData = (item.metaData + mapOf(
                        "ToolDamage" to (damage + (item.metaData["ToolDamageAdd"]?.toDouble() ?: 0.0)).toTag()
                )).toTag()) to
                        (item.metaData["ToolStrength"]?.toDouble() ?: 0.0) * (1.0 - tanh(
                                damage)) * modifier
            } else item to 0.0

    override fun toolLevel(item: TypedItem<VanillaItem>): Int {
        return item.metaData["ToolLevel"]?.toInt() ?: 0
    }

    override fun toolType(item: TypedItem<VanillaItem>): String {
        if (item.metaData["Data"]?.toInt() ?: 0 > 0) {
            return type()
        } else {
            return type() + "Head"
        }
    }

    override fun isTool(item: TypedItem<VanillaItem>): Boolean {
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
        modelHead = ItemModelSimple(textureHead!!, 1.0, 1.0, 1.0, 1.0)
        modelBuilt = ItemModelSimple(textureBuilt!!, 1.0, 1.0, 1.0, 1.0)
    }

    override fun render(item: TypedItem<VanillaItem>,
                        gl: GL,
                        shader: Shader) {
        if (item.data > 0) {
            modelBuilt?.render(gl, shader)
        } else {
            modelHead?.render(gl, shader)
        }
    }

    override fun renderInventory(item: TypedItem<VanillaItem>,
                                 gl: GL,
                                 shader: Shader) {
        if (item.data > 0) {
            modelBuilt?.renderInventory(gl, shader)
        } else {
            modelHead?.renderInventory(gl, shader)
        }
    }

    override fun name(item: TypedItem<VanillaItem>): String {
        val name = StringBuilder(100)
        name.append("Flint ").append(type())
        if (item.data == 0) {
            name.append(" Head")
        }
        val damage = (1.0 - tanh(
                item.metaData["ToolDamage"]?.toDouble() ?: 0.0)) * 100.0
        if (damage > 0.1) {
            name.append("\nDamage: ").append(damage.floorToInt())
        }
        return name.toString()
    }

    override fun maxStackSize(item: TypedItem<VanillaItem>): Int {
        return 1
    }

    abstract fun type(): String
}
