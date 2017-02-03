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
import org.tobi29.scapes.engine.utils.Checksum
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.server.connection.PlayerConnection
import java.util.concurrent.ThreadLocalRandom

class MobPlayerServerVB(world: WorldServer,
                        pos: Vector3d,
                        speed: Vector3d,
                        xRot: Double,
                        zRot: Double,
                        nickname: String,
                        skin: Checksum,
                        connection: PlayerConnection) : MobPlayerServer(world,
        pos, speed, AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 100.0, 100.0,
        Frustum(90.0, 1.0, 0.1, 24.0), Frustum(50.0, 1.0, 0.1, 2.0), nickname,
        skin, connection) {
    init {
        rot.setX(xRot)
        rot.setZ(zRot)
    }

    override fun wieldMode(): WieldMode {
        return if (inventorySelectLeft == inventorySelectRight)
            WieldMode.RIGHT
        else
            WieldMode.DUAL
    }

    override fun update(delta: Double) {
        val lookX = cosTable(rot.doubleZ().toRad()) *
                cosTable(rot.doubleX().toRad()) * 6.0
        val lookY = sinTable(rot.doubleZ().toRad()) *
                cosTable(rot.doubleX().toRad()) * 6.0
        val lookZ = sinTable(rot.doubleX().toRad()) * 6.0
        val viewOffset = viewOffset()
        viewField.setView(pos.doubleX() + viewOffset.x,
                pos.doubleY() + viewOffset.y,
                pos.doubleZ() + viewOffset.z, pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0.0, 0.0, 1.0)
        world.getEntities(
                viewField).filterMap<MobServer>().filter { it != this }.forEach { mob ->
            val mobPos = mob.getCurrentPos()
            if (!world.checkBlocked(pos.intX(), pos.intY(),
                    pos.intZ(), mobPos.intX(), mobPos.intY(),
                    mobPos.intZ())) {
                onNotice(mob)
            }
        }
        if (pos.doubleZ() < -100.0) {
            damage(-pos.doubleZ() - 100.0)
        }
        if (health < 10.0) {
            val random = ThreadLocalRandom.current()
            if (random.nextInt(40) == 0) {
                push(random.nextDouble() * 2.0 - 1.0, 0.0, 0.0)
            }
            if (random.nextInt(40) == 0) {
                push(0.0, random.nextDouble() * 2.0 - 1.0, 0.0)
            }
            if (random.nextInt(20) == 0) {
                setRot(Vector3d(
                        rot.doubleX() + random.nextDouble() * 60.0 - 30.0,
                        rot.doubleY(), rot.doubleZ()))
            }
            if (random.nextInt(20) == 0) {
                setRot(Vector3d(rot.doubleX(), rot.doubleY(),
                        rot.doubleZ() + random.nextDouble() * 60.0 - 30.0))
            }
        }
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.63)
    }

    override fun isActive(): Boolean {
        val conditionTag = metaData("Vanilla").structure("Condition")
        return !(conditionTag.getBoolean("Sleeping") ?: false)
    }
}
