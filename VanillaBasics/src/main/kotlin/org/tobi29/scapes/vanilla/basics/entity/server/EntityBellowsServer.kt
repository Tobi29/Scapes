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

package org.tobi29.scapes.vanilla.basics.entity.server

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.math.Face
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.server.EntityAbstractServer
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.io.tag.ReadWriteTagMap
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toDouble
import org.tobi29.io.tag.toTag
import org.tobi29.stdex.math.floorToInt
import kotlin.collections.set

class EntityBellowsServer(type: EntityType<*, *>,
                          world: WorldServer) : EntityAbstractServer(
        type, world, Vector3d.ZERO) {
    private val plugin = world.plugins.plugin<VanillaBasics>()
    val face
        get() = parseFace(world.terrain, pos.x.floorToInt(), pos.y.floorToInt(),
                pos.z.floorToInt(), plugin.materials.bellows)
    var scale = 0.0
        private set

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Scale"] = scale.toTag()
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Scale"]?.toDouble()?.let { scale = it }
    }

    override fun update(delta: Double) {
        scale = (scale + 0.4 * delta) % 2.0
    }

    override fun updateTile(terrain: TerrainServer,
                            x: Int,
                            y: Int,
                            z: Int,
                            data: Int) {
        val world = terrain.world
        val materials = plugin.materials
        if (terrain.type(pos.x.floorToInt(), pos.y.floorToInt(),
                pos.z.floorToInt()) !== materials.bellows) {
            world.removeEntity(this)
        }
    }

    companion object {
        fun parseFace(terrain: Terrain,
                      x: Int,
                      y: Int,
                      z: Int,
                      type: BlockType): Face {
            val block = terrain.block(x, y, z)
            if (terrain.type(block) != type) {
                return Face.NONE
            }
            val data = terrain.data(block)
            return Face[data]
        }
    }
}
