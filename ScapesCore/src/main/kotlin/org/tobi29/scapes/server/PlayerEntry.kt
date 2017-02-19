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

package org.tobi29.scapes.server

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.toMutTag
import org.tobi29.scapes.engine.utils.io.tag.toTag
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.server.connection.PlayerConnection

class PlayerEntry(private val permissions: Int,
                  private val worldName: String? = null,
                  private val entityTag: TagMap? = null) {
    fun createEntity(
            player: PlayerConnection,
            world: WorldServer?,
            pos: Vector3d?): MobPlayerServer? {
        if (entityTag == null) {
            return null
        }
        var spawnWorld = world
        if (spawnWorld == null && worldName != null) {
            spawnWorld = player.server.server.world(worldName)
        }
        if (spawnWorld == null) {
            return null
        }
        val spawnPos = Vector3d(spawnWorld.spawn + Vector3i(0, 0, 1))
        val entity = spawnWorld.plugins.worldType.newPlayer(
                spawnWorld, spawnPos, Vector3d.ZERO, 0.0, 0.0,
                player.name(), player.skin().checksum, player)
        val entityTag = pos?.let {
            entityTag.toMutTag().apply { this["Pos"] = it }.toTag()
        } ?: entityTag
        entity.read(entityTag)
        return entity
    }

    fun permissions(): Int {
        return permissions
    }
}