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

package org.tobi29.scapes.block

import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.plugins.Plugins

abstract class Material(type: MaterialType) {
    val plugins = type.plugins
    val nameID = type.name
    val id = type.id

    open fun example(data: Int): ItemStack {
        return ItemStack(this, data)
    }

    open fun click(entity: MobPlayerServer,
                   item: ItemStack) {
    }

    open fun click(entity: MobPlayerServer,
                   item: ItemStack,
                   terrain: TerrainServer.TerrainMutable,
                   x: Int,
                   y: Int,
                   z: Int,
                   face: Face): Double {
        return 0.0
    }

    open fun click(entity: MobPlayerServer,
                   item: ItemStack,
                   hit: MobServer): Double {
        return 0.0
    }

    open fun toolLevel(item: ItemStack): Int {
        return 0
    }

    open fun toolType(item: ItemStack): String {
        return "None"
    }

    open fun isTool(item: ItemStack): Boolean {
        return false
    }

    open fun isWeapon(item: ItemStack): Boolean {
        return false
    }

    open fun hitWait(item: ItemStack): Int {
        return 500
    }

    open fun hitRange(item: ItemStack): Double {
        return 2.0
    }

    abstract fun registerTextures(registry: TerrainTextureRegistry)

    abstract fun createModels(registry: TerrainTextureRegistry)

    abstract fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader)

    abstract fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader)

    open fun playerLight(item: ItemStack): Float {
        return 0f
    }

    abstract fun name(item: ItemStack): String

    abstract fun maxStackSize(item: ItemStack): Int
}

data class MaterialType(val plugins: Plugins,
                        val id: Int,
                        val name: String)
