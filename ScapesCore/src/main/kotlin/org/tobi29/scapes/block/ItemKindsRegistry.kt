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

import org.tobi29.scapes.inventory.ItemTypeKinds
import org.tobi29.scapes.inventory.ItemTypeKindsI
import org.tobi29.scapes.inventory.TypedItem
import org.tobi29.io.tag.toInt
import org.tobi29.io.tag.toTag

interface ItemTypeKindsRegistryI<I : ItemTypeKinds<K>, K : Any> : ItemTypeKindsI<I, K> {
    val registry: Registries.Registry<K>
    val kindTag: String

    override val kinds: Set<K>
        get() = registry.values()
                .asSequence().filterNotNull().toSet()

    override fun kind(item: TypedItem<I>): K =
            item.metaData[kindTag]?.toInt()?.let { registry[it] }
                    ?: registry.values.first { it != null }!!

    override fun kind(item: TypedItem<I>,
                      value: K): TypedItem<I> =
            item.copy((item.metaData +
                    (kindTag to registry[value].toTag())).toTag())
}
