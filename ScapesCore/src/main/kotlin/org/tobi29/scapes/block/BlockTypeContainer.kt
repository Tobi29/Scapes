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
package org.tobi29.scapes.block

import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.entity.server.EntityContainerServer
import org.tobi29.scapes.entity.server.MobPlayerServer

abstract class BlockTypeContainer protected constructor(registry: GameRegistry, nameID: String) : BlockType(
        registry, nameID) {

    override fun click(terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        val container = terrain.getEntities(x, y,
                z).filterMap<EntityContainerServer>().firstOrNull()
        if (container != null) {
            player.openGui(container)
        } else {
            player.openGui(placeEntity(terrain, x, y, z))
        }
        return true
    }

    override fun causesTileUpdate(): Boolean {
        return true
    }

    protected abstract fun placeEntity(terrain: TerrainServer,
                                       x: Int,
                                       y: Int,
                                       z: Int): EntityContainerServer
}
