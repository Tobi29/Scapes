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

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.inventory.*

typealias ItemTypeTextured = ItemTypeTexturedI<*>

interface ItemTypeTexturedI<in I : ItemType> : ItemTypeI<I> {
    fun registerTextures(registry: TerrainTextureRegistry) {}

    fun createModels(registry: TerrainTextureRegistry) {}
}

typealias ItemTypeModel = ItemTypeModelI<*>

interface ItemTypeModelI<in I : ItemType> : ItemTypeI<I> {
    fun render(item: TypedItem<I>,
               gl: GL,
               shader: Shader) {
    }

    fun renderInventory(item: TypedItem<I>,
                        gl: GL,
                        shader: Shader) {
    }
}

typealias ItemTypeModel2 = ItemTypeModel2I<*>

interface ItemTypeModel2I<in I : ItemType> : ItemTypeI<I> {

}

inline fun Item?.render(
        gl: GL,
        shader: Shader
) = kind<ItemTypeModel>()?.run {
    @Suppress("UNCHECKED_CAST")
    (type as ItemTypeModelI<ItemType>).render(this, gl, shader)
}

inline fun Item?.renderInventory(
        gl: GL,
        shader: Shader
) = kind<ItemTypeModel>()?.run {
    @Suppress("UNCHECKED_CAST")
    (type as ItemTypeModelI<ItemType>).renderInventory(this, gl, shader)
}
