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

typealias ItemTypeTool = ItemTypeToolI<*>

interface ItemTypeToolI<in I : ItemType> : ItemTypeI<I> {
    fun isTool(
            item: TypedItem<I>
    ): Boolean = false

    fun toolLevel(
            item: TypedItem<I>
    ): Int = 0

    fun toolType(
            item: TypedItem<I>
    ): String = "None"
}

inline fun Item?.isTool(): Boolean =
        kind<ItemTypeTool>()?.run {
            @Suppress("UNCHECKED_CAST")
            (type as ItemTypeToolI<ItemType>).isTool(this)
        } ?: false

inline fun TypedItem<ItemTypeTool>.toolLevel(): Int =
        @Suppress("UNCHECKED_CAST")
        (type as ItemTypeToolI<ItemType>).toolLevel(this)

inline fun TypedItem<ItemTypeTool>.toolType(): String =
        @Suppress("UNCHECKED_CAST")
        (type as ItemTypeToolI<ItemType>).toolType(this)
