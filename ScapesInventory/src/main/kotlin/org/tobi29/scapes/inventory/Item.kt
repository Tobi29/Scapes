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

package org.tobi29.scapes.inventory

import org.tobi29.io.tag.*
import kotlin.collections.set

typealias Item = TypedItem<ItemType>

class TypedItem<out T : ItemType>(
        val type: T,
        val metaData: TagMap = TagMap()
) : TagMapWrite {
    fun orNull(): TypedItem<T>? = if (isEmpty()) null else this

    override fun write(map: ReadWriteTagMap) {
        map["Type"] = type.id.toTag()
        map["MetaData"] = metaData
    }

    operator fun component1(): T = type
    operator fun component2(): TagMap = metaData

    fun <T : ItemType> copy(
            type: T,
            metaData: TagMap = this.metaData
    ): TypedItem<T> = TypedItem(type, metaData)

    fun copy(
            metaData: TagMap = this.metaData
    ): TypedItem<T> = TypedItem(type, metaData)


    override fun toString(): String {
        return "TypedItem(type=$type, metaData=$metaData)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypedItem<*>) return false

        if (type != other.type) return false
        if (metaData != other.metaData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + metaData.hashCode()
        return result
    }
}

fun MutableTag.toItem(registry: (Int) -> ItemType?): Item? {
    val map = toMap() ?: return null
    val material = map["Type"]?.toInt()?.let {
        registry(it)
    } ?: return null
    val metaData = map["MetaData"]?.toMap() ?: TagMap()
    return Item(material, metaData)
}

inline fun <reified T : ItemType> TypedItem<*>?.kind(): TypedItem<T>? =
        if (this != null && type is T) this as TypedItem<T> else null

inline fun Item?.isEmpty(): Boolean = this == null
        || (kind<ItemTypeStackable>()?.let { it.amount <= 0 } == true)

inline fun Item?.orNull(): Item? = this?.let { if (it.isEmpty()) null else it }

inline fun Item?.toTag(): TagMap = if (this == null) TagMap() else toTag()
