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

import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toInt
import org.tobi29.io.tag.toTag
import org.tobi29.stdex.math.ceilToInt
import kotlin.math.min

typealias ItemStackable = TypedItem<ItemTypeStackable>
typealias ItemTypeStackable = ItemTypeStackableI<*>

interface ItemTypeStackableI<in I : ItemType> : ItemTypeI<I> {
    /**
     * Stacks one item onto another
     *
     * The implementation shall take all items of bottom and as many as possible
     * from top and return them as the left stack
     *
     * Leftovers of [top] (if any) shall be returned on the right stack
     *
     * As per convention, the `"Amount"` tag has to be updated accordingly
     */
    fun stack(bottom: TypedItem<I>,
              top: ItemStackable,
              max: Int = Int.MAX_VALUE): Pair<Item?, Item?>

    /**
     * Materializes a stack by taking it from another
     *
     * The implementation shall take as many items of top to recreate bottom
     * into the middle stack
     *
     * Leftovers of [top] (if any) shall be returned on the left stack
     *
     * Leftovers of [bottom] (if any) shall be returned on the right stack
     *
     * As per convention, the `"Amount"` tag has to be updated accordingly
     */
    fun take(top: TypedItem<I>,
             bottom: ItemStackable): Triple<Item?, Item?, Item?>
}

interface ItemTypeStackableDefaultI<in I : ItemTypeStackableDefaultI<I>> : ItemTypeStackableI<I> {
    fun maxStackSize(item: TypedItem<I>): Int

    override fun stack(bottom: TypedItem<I>,
                       top: ItemStackable,
                       max: Int): Pair<Item?, Item?> {
        if (top.type != this
                || (bottom.metaData - "Amount") != (top.metaData - "Amount"))
            return bottom to top

        @Suppress("UNCHECKED_CAST")
        val topM = top as TypedItem<I>

        val amount = bottom.amount + topM.amount
        val maxBottom = bottom.type.maxStackSize(bottom)
        val maxTop = bottom.type.maxStackSize(topM)
        val leftAmount = amount.coerceAtMost(max)
                .coerceAtMost(min(maxBottom, maxTop))

        return bottom.copy(amount = leftAmount).orNull() to
                topM.copy(amount = amount - leftAmount).orNull()
    }

    override fun take(top: TypedItem<I>,
                      bottom: ItemStackable): Triple<Item?, Item?, Item?> {
        if (bottom.type != this) return Triple(top, null, bottom)

        val amount = min(top.amount, bottom.amount)
        val (left, right) = top.split(amount)
        return Triple(right, left,
                bottom.copy(amount = bottom.amount - amount).orNull())
    }
}

inline val ItemStackable?.amount: Int
    get() = if (this == null) 0 else metaData["Amount"]?.toInt() ?: 1

fun <T : ItemTypeStackable> TypedItem<T>.copy(
        type: T = this.type,
        amount: Int = this.amount,
        metaData: TagMap = this.metaData
): TypedItem<T> = TypedItem(type,
        (metaData + ("Amount" to amount.toTag())).toTag())

fun <T : ItemTypeStackable> ItemStack(
        type: T,
        amount: Int,
        metaData: TagMap = TagMap()
): TypedItem<T> = TypedItem(type,
        (metaData + ("Amount" to amount.toTag())).toTag())

fun <T, K : Any> ItemStack(
        type: T,
        kind: K,
        amount: Int,
        metaData: TagMap = TagMap()
): TypedItem<T> where T : ItemTypeStackable, T : ItemTypeKinds<K> =
        ItemStack(type, amount, metaData).copy(kind)

fun Item?.split(ratio: Double): Pair<Item?, Item?> =
        kind<ItemTypeStackable>()?.run {
            split((amount * ratio).ceilToInt())
        } ?: this to null

fun Item?.split(left: Int): Pair<Item?, Item?> =
        kind<ItemTypeStackable>()?.run {
            left.coerceAtMost(amount).let {
                copy(amount = it).orNull() to copy(
                        amount = amount - it).orNull()
            }
        } ?: this to null

fun Item?.stack(
        add: Item?,
        max: Int = Int.MAX_VALUE
): Pair<Item?, Item?> = add.kind<ItemTypeStackable>()?.let { other ->
    kind<ItemTypeStackable>()?.run {
        @Suppress("UNCHECKED_CAST")
        (type as ItemTypeStackableI<ItemType>).stack(this, other, max)
    } ?: add to null
} ?: this to add

fun Item?.stackAll(
        add: Item?
): Pair<Item?, Item?> {
    val result = stack(add)
    return if (result.second.isEmpty()) result
    else this to add
}

fun Item?.take(
        take: Item?
): Triple<Item?, Item?, Item?> = take.kind<ItemTypeStackable>()?.let { other ->
    kind<ItemTypeStackable>()?.run {
        @Suppress("UNCHECKED_CAST")
        (type as ItemTypeStackableI<ItemType>).take(this, other)
    }
} ?: Triple(null, this, take)

fun Item?.takeAll(
        take: Item?
): Pair<Item?, Item?> {
    val result = take(take)
    return if (result.third.isEmpty()) result.first to result.second
    else result.first to null
}
