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

import org.tobi29.math.Face
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.*

typealias ItemTypeUseable = ItemTypeUseableI<*>

interface ItemTypeUseableI<in I : ItemType> : ItemTypeI<I> {
    fun click(
            entity: MobPlayerServer,
            item: TypedItem<I>
    ): Item? = item

    fun click(
            entity: MobPlayerServer,
            item: TypedItem<I>,
            terrain: TerrainServer,
            x: Int,
            y: Int,
            z: Int,
            face: Face
    ): Pair<Item?, Double?> = item to null

    fun click(
            entity: MobPlayerServer,
            item: TypedItem<I>,
            hit: MobServer
    ): Pair<Item?, Double?> = item to null
}

inline fun Item?.click(
        entity: MobPlayerServer
) = kind<ItemTypeUseable>()?.run {
    @Suppress("UNCHECKED_CAST")
    (type as ItemTypeUseableI<ItemType>).click(entity, this)
} ?: this

inline fun Item?.click(
        entity: MobPlayerServer,
        terrain: TerrainServer,
        x: Int,
        y: Int,
        z: Int,
        face: Face
): Pair<Item?, Double?> = kind<ItemTypeUseable>()?.run {
    @Suppress("UNCHECKED_CAST")
    (type as ItemTypeUseableI<ItemType>).click(entity, this, terrain, x, y, z, face)
} ?: this to null

inline fun Item?.click(
        entity: MobPlayerServer,
        hit: MobServer
): Pair<Item?, Double?> = kind<ItemTypeUseable>()?.run {
    @Suppress("UNCHECKED_CAST")
    (type as ItemTypeUseableI<ItemType>).click(entity, this, hit)
} ?: this to null
