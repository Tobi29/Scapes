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

package org.tobi29.scapes.vanilla.basics.material.block

import org.tobi29.scapes.chunk.terrain.TerrainMutableServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.math.Face
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.client.EntityContainerClient
import org.tobi29.scapes.entity.server.EntityContainerServer
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterialType

abstract class VanillaBlockEntity<out C : EntityClient, out S : EntityServer>(
        type: VanillaMaterialType,
        val entity: EntityType<C, S>
) : VanillaBlock(type) {
    override fun causesTileUpdate(): Boolean {
        return true
    }

    override fun place(terrain: TerrainMutableServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        if (!super.place(terrain, x, y, z, face, player)) {
            return false
        }
        getEntity(player.world.terrain, x, y, z)
        return true
    }

    fun getEntity(terrain: TerrainServer,
                  x: Int,
                  y: Int,
                  z: Int): S {
        return terrain.getEntities(x, y, z).filter { it.type == entity }.map {
            @Suppress("UNCHECKED_CAST")
            it as S
        }.firstOrNull() ?: run {
            placeEntity(terrain, x, y, z)
        }
    }

    protected fun placeEntity(terrain: TerrainServer,
                              x: Int,
                              y: Int,
                              z: Int): S {
        val entity = entity.createServer(
                terrain.world).apply {
            setPos(Vector3d(x + 0.5, y + 0.5, z + 0.5))
        }
        terrain.world.addEntityNew(entity)
        return entity
    }
}

abstract class VanillaBlockContainer<out C : EntityContainerClient, out S : EntityContainerServer>(
        type: VanillaMaterialType,
        entity: EntityType<C, S>
) : VanillaBlockEntity<C, S>(type, entity) {
    override fun click(terrain: TerrainServer,
                       x: Int,
                       y: Int,
                       z: Int,
                       face: Face,
                       player: MobPlayerServer): Boolean {
        player.openGui(getEntity(terrain, x, y, z))
        return true
    }
}
