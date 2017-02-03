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
package scapes.plugin.tobi29.vanilla.basics.entity.server

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.math.cosTable
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.sinTable
import org.tobi29.scapes.engine.utils.math.toRad
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.entity.server.MobPositionSenderServer
import org.tobi29.scapes.entity.server.MobServer
import java.util.concurrent.ThreadLocalRandom

class EntityTornadoServer(world: WorldServer,
                          pos: Vector3d = Vector3d.ZERO) : EntityServer(
        world, pos) {
    private val positionHandler: MobPositionSenderServer
    private var time = 0.0
    private var dir = 0.0

    init {
        val random = ThreadLocalRandom.current()
        dir = random.nextDouble() * 360
        time = (random.nextInt(100) + 20).toDouble()
        positionHandler = MobPositionSenderServer(pos, { world.send(it) })
    }

    override fun write(): TagStructure {
        val tag = super.write()
        tag.setDouble("Dir", dir)
        tag.setDouble("Time", time)
        return tag
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getDouble("Dir")?.let { dir = it }
        tagStructure.getDouble("Time")?.let { time = it }
        updatePosition()
    }

    override fun update(delta: Double) {
        val random = ThreadLocalRandom.current()
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
