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

package org.tobi29.scapes.inventory

import org.tobi29.io.tag.TagMap

typealias ItemTypeKinds<K> = ItemTypeKindsI<*, K>

interface ItemTypeKindsI<I : ItemType, K : Any> : ItemTypeI<I> {
    val kinds: Set<K>

    fun kind(item: TypedItem<I>): K

    fun kind(item: TypedItem<I>,
             value: K): TypedItem<I>
}

inline val <K : Any> TypedItem<ItemTypeKinds<K>>.kind: K
    get() = @Suppress("UNCHECKED_CAST")
    (type as ItemTypeKindsI<ItemType, K>).kind(this)

fun <T : ItemTypeKinds<K>, K : Any> Item(
        type: T,
        kind: K,
        metaData: TagMap = TagMap()
): TypedItem<T> = TypedItem(type, metaData).copy(kind)

@Suppress("UNCHECKED_CAST")
fun <T : ItemTypeKinds<K>, K : Any> TypedItem<T>.copy(
        kind: K = this.kind
): TypedItem<T> =
        (type as ItemTypeKindsI<ItemType, K>).kind(this, kind) as TypedItem<T>

fun <T : ItemTypeKinds<K>, K : Any> TypedItem<T>.copy(
        kind: K = this.kind,
        metaData: TagMap = this.metaData
): TypedItem<T> = copy(metaData = metaData).copy(kind)

fun <T : ItemTypeKinds<K>, T2 : ItemTypeKinds<K>, K : Any> TypedItem<T>.copy(
        type: T2,
        kind: K = this.kind,
        metaData: TagMap = this.metaData
): TypedItem<T2> = copy(type, metaData).copy(kind)
