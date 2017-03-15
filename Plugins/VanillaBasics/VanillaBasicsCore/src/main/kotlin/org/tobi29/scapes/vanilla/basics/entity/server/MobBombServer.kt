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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.vanilla.basics.material.BlockExplosive

class MobBombServer(type: EntityType<*, *>,
                    world: WorldServer) : MobServer(type, world, Vector3d.ZERO,
        Vector3d.ZERO, AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)) {
    private val item = ItemStack(world.plugins)
    private var time = 0.0

    init {
        stepHeight = 0.0
    }

    fun setType(item: ItemStack) {
        this.item.set(item)
    }

    fun setTime(time: Double) {
        this.time = time
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Block"] = TagMap { item.write(this) }
        map["Time"] = time
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Block"]?.toMap()?.let { item.read(it) }
        map["Time"]?.toDouble()?.let { time = it }
    }

    override fun update(delta: Double) {
        time -= delta
        if (time <= 0.0) {
            world.playSound("Scapes:sound/entity/mob/Explosion1.ogg", this,
                    1.0f, 64.0f)
            world.removeEntity(this)
            val x = pos.intX()
            val y = pos.intY()
            val z = pos.intZ()
            (item.material() as BlockExplosive).explode(world.terrain, x, y, z)
        }
    }
}
