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

package org.tobi29.scapes.entity.client

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.Checksum
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.toMap
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.CreatureType

abstract class MobPlayerClient(world: WorldClient,
                               pos: Vector3d,
                               speed: Vector3d,
                               aabb: AABB,
                               lives: Double,
                               maxLives: Double,
                               protected var nickname: String) : MobLivingEquippedClient(
        world, pos, speed, aabb, lives, maxLives) {
    var inventorySelectLeft = 0
    var inventorySelectRight = 9
    protected var skin: Checksum? = null

    fun inventorySelectLeft(): Int {
        return inventorySelectLeft
    }

    fun inventorySelectRight(): Int {
        return inventorySelectRight
    }

    fun nickname(): String {
        return nickname
    }

    override fun creatureType(): CreatureType {
        return CreatureType.CREATURE
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Nickname"]?.toString()?.let { nickname = it }
        map["SkinChecksum"]?.toMap()?.let { skin = Checksum(it) }
    }
}
