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
package org.tobi29.scapes.entity.server

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import java.util.concurrent.ThreadLocalRandom

class MobFlyingBlockServer(world: WorldServer,
                           pos: Vector3d = Vector3d.ZERO,
                           speed: Vector3d = Vector3d.ZERO,
                           type: BlockType = world.air,
                           data: Int = 0) : MobServer(
        world, pos, speed, AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)) {
    private val item: ItemStack
    private var time = 0.0

    init {
        item = ItemStack(type, data)
        stepHeight = 0.0
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
                    val random = ThreadLocalRandom.current()
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
                world.terrain.queue { handler ->
                    handler.typeData(x, y, z, item.material() as BlockType,
                            item.data())
                }
                world.removeEntity(this)
            }
        }
    }
}
