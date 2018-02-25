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

package org.tobi29.scapes.entity

import org.tobi29.math.AABB
import org.tobi29.math.Frustum
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.distanceSqr
import org.tobi29.stdex.math.ceilToInt
import org.tobi29.stdex.math.floorToInt

fun <E : Entity> EntityContainer<E>.getEntities(minX: Int,
                                                minY: Int,
                                                minZ: Int,
                                                maxX: Int,
                                                maxY: Int,
                                                maxZ: Int): Sequence<E> {
    return getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
            maxZ).filter { entity ->
        val pos = entity.getCurrentPos()
        val x = pos.x.floorToInt()
        if (x in minX..maxX) {
            return@filter false
        }
        val y = pos.y.floorToInt()
        if (y in minY..maxY) {
            return@filter false
        }
        val z = pos.z.floorToInt()
        if (z in minZ..maxZ) {
            return@filter false
        }
        true
    }
}

fun <E : Entity> EntityContainer<E>.getEntities(pos: Vector3d,
                                                range: Double): Sequence<E> {
    val minX = (pos.x - range).floorToInt()
    val minY = (pos.y - range).floorToInt()
    val minZ = (pos.z - range).floorToInt()
    val maxX = (pos.x + range).ceilToInt()
    val maxY = (pos.y + range).ceilToInt()
    val maxZ = (pos.z + range).ceilToInt()
    val rangeSqr = range * range
    return getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
            maxZ).filter { entity ->
        pos.distanceSqr(entity.getCurrentPos()) <= rangeSqr
    }
}

fun <E : Entity> EntityContainer<E>.getEntities(aabb: AABB): Sequence<E> {
    val minX = aabb.minX.floorToInt() - EntityContainer.MAX_ENTITY_SIZE
    val minY = aabb.minY.floorToInt() - EntityContainer.MAX_ENTITY_SIZE
    val minZ = aabb.minZ.floorToInt() - EntityContainer.MAX_ENTITY_SIZE
    val maxX = aabb.maxX.ceilToInt() + EntityContainer.MAX_ENTITY_SIZE
    val maxY = aabb.maxY.ceilToInt() + EntityContainer.MAX_ENTITY_SIZE
    val maxZ = aabb.maxZ.ceilToInt() + EntityContainer.MAX_ENTITY_SIZE
    return getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
            maxZ).filter { entity -> aabb.overlay(entity.getAABB()) }
}

fun <E : Entity> EntityContainer<E>.getEntities(frustum: Frustum): Sequence<E> {
    val x = frustum.x()
    val y = frustum.y()
    val z = frustum.z()
    val range = frustum.range()
    val minX = (x - range).floorToInt()
    val minY = (y - range).floorToInt()
    val minZ = (z - range).floorToInt()
    val maxX = (x + range).ceilToInt()
    val maxY = (y + range).ceilToInt()
    val maxZ = (z + range).ceilToInt()
    return getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
            maxZ).filter { entity -> frustum.inView(entity.getAABB()) > 0 }
}
