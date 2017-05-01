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
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.engine.utils.threadLocalRandom
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.server.MobServer

class MobFlyingBlockServer(type: EntityType<*, *>,
                           world: WorldServer) : MobServer(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)) {
    private val item = ItemStack(world.plugins)
    private var time = 0.0

    fun setType(item: ItemStack) {
        this.item.set(item)
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
        if (isOnGround) {
            val x = pos.intX()
            val y = pos.intY()
            val z = pos.intZ()
            if (!world.terrain.type(x, y, z).isReplaceable(world.terrain, x, y,
                    z)) {
                var xx = pos.intX() + 0.5
                var yy = pos.intY() + 0.5
                if (pos.doubleX() == xx && pos.doubleY() == yy) {
                    val random = threadLocalRandom()
                    xx += random.nextDouble() * delta - delta
                    yy += random.nextDouble() * delta - delta
                }
                push(pos.doubleX() - xx, pos.doubleY() - yy, 0.0)
                time += delta
                if (time >= 5.0) {
                    pos.plusZ(1.0)
                    speed.setZ(1.0)
                }
            } else if (abs(speed.doubleX()) < 0.1 && abs(
                    speed.doubleY()) < 0.1) {
                world.terrain.modify(x, y, z) { handler ->
                    handler.typeData(x, y, z, item.material() as BlockType,
                            item.data())
                }
                world.removeEntity(this)
            }
        }
    }
}
