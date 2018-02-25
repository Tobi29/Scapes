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

import org.tobi29.scapes.inventory.*
import org.tobi29.io.tag.Tag
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toInt
import org.tobi29.io.tag.toTag

typealias ItemTypeData = ItemTypeDataI<*>

interface ItemTypeDataI<in I : ItemType> : ItemTypeI<I> {
    fun data(item: TypedItem<I>): Int = (item as Item).metaData["Data"]?.toInt() ?: 0
}

fun <T> ItemStackData(
        type: T,
        data: Int = 0,
        amount: Int = 1,
        metaData: TagMap = TagMap()
): TypedItem<T> where T : ItemTypeStackable, T : ItemTypeData = TypedItem(type,
        (metaData + mapOf("Data" to data.toTag(),
                "Amount" to amount.toTag())).toTag())

inline val Item.data: Int
    get() = @Suppress("UNCHECKED_CAST")
    (type as ItemTypeDataI<ItemType>).data(this)

fun <T : ItemTypeData> TypedItem<T>.copy(
        data: Int = this.data,
        metaData: Map<String, Tag> = this.metaData
): TypedItem<T> = copy(
        type = this.type,
        data = data,
        metaData = metaData)

fun <T : ItemTypeData, T2 : ItemType> TypedItem<T>.copy(
        type: T2,
        data: Int = this.data,
        metaData: Map<String, Tag> = this.metaData
): TypedItem<T2> = copy(
        type = type,
        metaData = (metaData + ("Data" to data.toTag())).toTag())

fun <T> TypedItem<T>.copy(
        data: Int = this.data,
        amount: Int = this.amount,
        metaData: Map<String, Tag> = this.metaData
): TypedItem<T> where T : ItemTypeStackable, T : ItemTypeData = copy(
        type = this.type,
        data = data,
        amount = amount,
        metaData = metaData)

fun <T, T2 : ItemType> TypedItem<T>.copy(
        type: T2,
        data: Int = this.data,
        amount: Int = this.amount,
        metaData: Map<String, Tag> = this.metaData
): TypedItem<T2> where T : ItemTypeStackable, T : ItemTypeData = copy(
        type = type,
        metaData = (metaData + mapOf("Data" to data.toTag(),
                "Amount" to amount.toTag())).toTag())
