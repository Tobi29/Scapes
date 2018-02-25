/*
 * Copyright 2012-2018 Tobi29
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

import org.tobi29.scapes.block.models.ItemModel
import org.tobi29.scapes.block.models.ItemModelSimple
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.scapes.engine.resource.map
import org.tobi29.utils.ComponentTypeRegistered
import org.tobi29.scapes.inventory.ItemType
import org.tobi29.scapes.inventory.ItemTypeI
import org.tobi29.scapes.inventory.ItemTypeKindsI
import org.tobi29.scapes.inventory.TypedItem

typealias ItemTypeIcon = ItemTypeIconI<*>

interface ItemTypeIconI<in I : ItemType> : ItemTypeI<I>,
        ItemTypeTexturedI<I>,
        ItemTypeModelI<I> {
    val model: Resource<ItemModel> get() = this[ITEM_MODEL_COMPONENT]

    val textureAsset: String

    override fun createModels(registry: TerrainTextureRegistry) {
        val texture = registry.engine.graphics.textures[textureAsset]
        val model = texture.map {
            ItemModelSimple(it.gos, it, it.width(), it.height(), 0.0, 1.0, 0.0,
                    1.0, 1.0, 1.0, 1.0, 1.0)
        }
        registerComponent(ITEM_MODEL_COMPONENT, model)
    }

    override fun render(item: TypedItem<I>,
                        gl: GL,
                        shader: Shader) {
        model.tryGet()?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<I>,
                                 gl: GL,
                                 shader: Shader) {
        model.tryGet()?.renderInventory(gl, shader)
    }
}

private val ITEM_MODEL_COMPONENT = ComponentTypeRegistered<ItemTypeIcon, Resource<ItemModel>, Any>()

typealias ItemTypeIconKinds<K> = ItemTypeIconKindsI<*, K>

interface ItemTypeIconKindsI<I : ItemType, K : Any> : ItemTypeKindsI<I, K>,
        ItemTypeTexturedI<I>,
        ItemTypeModelI<I> {
    val models: Map<K, Resource<ItemModel>> get() = this[ITEM_MODELS_COMPONENT<K>()]

    fun textureAsset(kind: K): String

    override fun createModels(registry: TerrainTextureRegistry) {
        val models = HashMap<K, Resource<ItemModel>>()
        for (kind in kinds) {
            val texture = registry.engine.graphics.textures[textureAsset(kind)]
            val model = texture.map {
                ItemModelSimple(it.gos, it, it.width(), it.height(), 0.0, 1.0,
                        0.0, 1.0, 1.0, 1.0, 1.0, 1.0)
            }
            models[kind] = model
        }
        registerComponent(ITEM_MODELS_COMPONENT<K>(), models)
    }

    override fun render(item: TypedItem<I>,
                        gl: GL,
                        shader: Shader) {
        models[kind(item)]?.tryGet()?.render(gl, shader)
    }

    override fun renderInventory(item: TypedItem<I>,
                                 gl: GL,
                                 shader: Shader) {
        models[kind(item)]?.tryGet()?.renderInventory(gl, shader)
    }
}

private val itemModelsComponent = ComponentTypeRegistered<ItemTypeIconKinds<Any>, Map<Any, Resource<ItemModel>>, Any>()

@Suppress("UNCHECKED_CAST")
fun <K : Any> ITEM_MODELS_COMPONENT() =
        itemModelsComponent as ComponentTypeRegistered<ItemTypeIconKinds<K>, Map<K, Resource<ItemModel>>, Any>
