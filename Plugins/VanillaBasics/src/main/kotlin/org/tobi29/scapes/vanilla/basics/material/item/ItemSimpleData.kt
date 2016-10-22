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

package org.tobi29.scapes.vanilla.basics.material.item

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTexture
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.engine.utils.streamRange
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

abstract class ItemSimpleData protected constructor(materials: VanillaMaterial, nameID: String) : VanillaItem(
        materials, nameID) {
    protected var textures: Array<TerrainTexture?>? = null
    protected var models: Array<ItemModel?>? = null

    protected abstract fun types(): Int

    protected abstract fun texture(data: Int): String?

    override fun registerTextures(registry: TerrainTextureRegistry) {
        textures = streamRange(0, types()).mapToObj {
            val texture = texture(it) ?: return@mapToObj null
            registry.registerTexture(texture)
        }.toArray { arrayOfNulls<TerrainTexture?>(it) }
    }

    override fun createModels(registry: TerrainTextureRegistry) {
        textures?.let {
            models = stream(*it).map {
                if (it == null) {
                    return@map null
                }
                ItemModelSimple(it, 1.0, 1.0, 1.0, 1.0)
            }.toArray { arrayOfNulls<ItemModel?>(it) }
        }
    }

    override fun render(item: ItemStack,
                        gl: GL,
                        shader: Shader) {
        models?.get(item.data())?.render(gl, shader)
    }

    override fun renderInventory(item: ItemStack,
                                 gl: GL,
                                 shader: Shader) {
        models?.get(item.data())?.renderInventory(gl, shader)
    }
}
