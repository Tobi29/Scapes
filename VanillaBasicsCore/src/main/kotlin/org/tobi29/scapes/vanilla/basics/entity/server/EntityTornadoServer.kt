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

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.engine.utils.tag.ReadWriteTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.tag.toTag
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.EntityAbstractServer
import org.tobi29.scapes.entity.server.MobPositionSenderServer
import org.tobi29.scapes.entity.server.MobServer
import kotlin.collections.set

class EntityTornadoServer(type: EntityType<*, *>,
                          world: WorldServer) : EntityAbstractServer(
        type, world, Vector3d.ZERO) {
    private val positionHandler: MobPositionSenderServer
    private var time = 0.0
    private var dir = 0.0

    init {
        val random = threadLocalRandom()
        dir = random.nextDouble() * 360
        time = (random.nextInt(100) + 20).toDouble()
        positionHandler = MobPositionSenderServer(registry, { world.send(it) })
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Dir"] = dir.toTag()
        map["Time"] = time.toTag()
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Dir"]?.toDouble()?.let { dir = it }
        map["Time"]?.toDouble()?.let { time = it }
        updatePosition()
    }

    override fun update(delta: Double) {
        val random = threadLocalRandom()
        dir += (random.nextDouble() * 80.0 - 40.0) * delta
        val d = dir.toRad()
        val speed = 2.0 * delta
        pos.plusX(cosTable(d) * speed)
        pos.plusY(sinTable(d) * speed)
        pos.setZ(world.terrain.highestTerrainBlockZAt(pos.intX(),
                pos.intY()) + 0.5)
        updatePosition()
        val currentPos = pos.now()
        world.getEntities(currentPos,
                16.0).filterMap<MobServer>().forEach { mob ->
            val push = mob.getCurrentPos() - currentPos
            val s = max(0.0,
                    320.0 - push.length() * 8.0) * delta
            val force = push.normalizeSafe() * -s
            mob.push(force.x, force.y, s)
        }
        time -= delta
        if (time <= 0.0) {
            world.removeEntity(this)
        }
    }

    private fun updatePosition() {
        positionHandler.submitUpdate(uuid, pos.now(), Vector3d.ZERO,
                Vector3d.ZERO,
                false, false, false, false)
    }
}
