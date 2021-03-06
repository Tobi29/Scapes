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

import org.tobi29.uuid.Uuid

interface EntityContainer<E : Entity> {
    fun addEntity(entity: E,
                  spawn: Boolean = false): Boolean

    fun removeEntity(entity: E): Boolean

    fun hasEntity(uuid: Uuid): Boolean {
        return getEntity(uuid) != null
    }

    fun hasEntity(entity: E): Boolean

    fun getEntity(uuid: Uuid): E?

    fun getEntities(): Sequence<E>

    fun getEntities(x: Int,
                    y: Int,
                    z: Int): Sequence<E>

    fun getEntitiesAtLeast(minX: Int,
                           minY: Int,
                           minZ: Int,
                           maxX: Int,
                           maxY: Int,
                           maxZ: Int): Sequence<E>

    fun entityAdded(entity: E,
                    spawn: Boolean = false)

    fun entityRemoved(entity: E)

    companion object {
        val MAX_ENTITY_SIZE = 16
    }
}
