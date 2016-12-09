/*
 * Copyright 2012-2016 Tobi29
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

import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.Frustum
import org.tobi29.scapes.engine.utils.math.ceil
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.distanceSqr

fun <E : Entity> EntityContainer<E>.getEntities(minX: Int,
                                                minY: Int,
                                                minZ: Int,
                                                maxX: Int,
                                                maxY: Int,
                                                maxZ: Int): Sequence<E> {
    return getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
            maxZ).filter { entity ->
        val pos = entity.getCurrentPos()
        val x = pos.intX()
        if (x >= minX && x <= maxX) {
            return@filter false
        }
        val y = pos.intY()
        if (y >= minY && y <= maxY) {
            return@filter false
        }
        val z = pos.intZ()
        if (z >= minZ && z <= maxZ) {
            return@filter false
        }
        true
    }
}

fun <E : Entity> EntityContainer<E>.getEntities(pos: Vector3d,
                                                range: Double): Sequence<E> {
    val minX = floor(pos.x - range)
    val minY = floor(pos.y - range)
    val minZ = floor(pos.z - range)
    val maxX = ceil(pos.x + range)
    val maxY = ceil(pos.y + range)
    val maxZ = ceil(pos.z + range)
    val rangeSqr = range * range
    return getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
            maxZ).filter { entity ->
        pos.distanceSqr(entity.getCurrentPos()) <= rangeSqr
    }
}

fun <E : Entity> EntityContainer<E>.getEntities(aabb: AABB): Sequence<E> {
    val minX = floor(aabb.minX) - EntityContainer.MAX_ENTITY_SIZE
    val minY = floor(aabb.minY) - EntityContainer.MAX_ENTITY_SIZE
    val minZ = floor(aabb.minZ) - EntityContainer.MAX_ENTITY_SIZE
    val maxX = ceil(aabb.maxX) + EntityContainer.MAX_ENTITY_SIZE
    val maxY = ceil(aabb.maxY) + EntityContainer.MAX_ENTITY_SIZE
    val maxZ = ceil(aabb.maxZ) + EntityContainer.MAX_ENTITY_SIZE
    return getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
            maxZ).filter { entity -> aabb.overlay(entity.getAABB()) }
}

fun <E : Entity> EntityContainer<E>.getEntities(frustum: Frustum): Sequence<E> {
    val x = frustum.x()
    val y = frustum.y()
    val z = frustum.z()
    val range = frustum.range()
    val minX = floor(x - range)
    val minY = floor(y - range)
    val minZ = floor(z - range)
    val maxX = ceil(x + range)
    val maxY = ceil(y + range)
    val maxZ = ceil(z + range)
    return getEntitiesAtLeast(minX, minY, minZ, maxX, maxY,
            maxZ).filter { entity -> frustum.inView(entity.getAABB()) > 0 }
}
