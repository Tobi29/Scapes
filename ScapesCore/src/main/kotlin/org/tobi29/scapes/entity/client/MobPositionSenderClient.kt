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

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.engine.utils.UUID
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.angleDiff
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.minus
import org.tobi29.scapes.engine.utils.threadLocalRandom
import org.tobi29.scapes.packets.*

class MobPositionSenderClient(private val registry: Registries,
                              pos: Vector3d,
                              private val packetHandler: (PacketBoth) -> Unit) {
    private val sentPos: MutableVector3d
    private val sentSpeed = MutableVector3d()
    private val sentRot = MutableVector3d()
    private var nextForce: Long = 0
    private var ground = false
    private var slidingWall = false
    private var inWater = false
    private var swimming = false
    private var init = false

    init {
        sentPos = MutableVector3d(pos)
        nextForce = System.nanoTime() + FORCE_TIME + (threadLocalRandom().nextInt(
                FORCE_TIME_RANDOM).toLong() shl 3)
    }

    fun submitUpdate(uuid: UUID,
                     pos: Vector3d,
                     speed: Vector3d,
                     rot: Vector3d,
                     ground: Boolean,
                     slidingWall: Boolean,
                     inWater: Boolean,
                     swimming: Boolean,
                     forced: Boolean = false) {
        submitUpdate(uuid, pos, speed, rot, ground, slidingWall, inWater,
                swimming, forced, packetHandler)
    }

    @Synchronized fun submitUpdate(uuid: UUID,
                                   pos: Vector3d,
                                   speed: Vector3d,
                                   rot: Vector3d,
                                   ground: Boolean,
                                   slidingWall: Boolean,
                                   inWater: Boolean,
                                   swimming: Boolean,
                                   forced: Boolean,
                                   packetHandler: (PacketBoth) -> Unit) {
        var force = forced
        if (!init) {
            init = true
            force = true
        }
        val time = System.nanoTime()
        if (!force && time >= nextForce) {
            force = true
            nextForce = time + FORCE_TIME + (threadLocalRandom().nextInt(
                    FORCE_TIME_RANDOM).toLong() shl 3)
        }
        val oldPos: Vector3d?
        if (force) {
            oldPos = null
        } else {
            oldPos = sentPos.now()
        }
        if (force || max(abs(sentPos.now().minus(
                pos))) > POSITION_SEND_OFFSET) {
            sendPos(uuid, pos, force)
            sendSpeed(uuid, speed, force)
        } else {
            if (abs(sentSpeed.doubleX()) > SPEED_SEND_OFFSET && abs(
                    speed.x) <= SPEED_SEND_OFFSET ||
                    abs(sentSpeed.doubleY()) > SPEED_SEND_OFFSET && abs(
                            speed.y) <= SPEED_SEND_OFFSET ||
                    abs(sentSpeed.doubleZ()) > SPEED_SEND_OFFSET && abs(
                            speed.z) <= SPEED_SEND_OFFSET) {
                sendSpeed(uuid, Vector3d.ZERO, false)
            } else if (max(
                    abs(sentSpeed.now().minus(speed))) > SPEED_SEND_OFFSET) {
                sendSpeed(uuid, speed, false)
            }
        }
        if (force || abs(angleDiff(sentRot.doubleX(),
                rot.x)) > DIRECTION_SEND_OFFSET || abs(
                angleDiff(sentRot.doubleY(),
                        rot.y)) > DIRECTION_SEND_OFFSET || abs(
                angleDiff(sentRot.doubleZ(),
                        rot.z)) > DIRECTION_SEND_OFFSET) {
            sendRotation(uuid, rot, force)
        }
        if (force || this.ground != ground ||
                this.slidingWall != slidingWall || this.inWater != inWater ||
                this.swimming != swimming) {
            this.ground = ground
            this.slidingWall = slidingWall
            this.inWater = inWater
            this.swimming = swimming
            packetHandler(
                    PacketMobChangeState(registry, uuid, oldPos, ground,
                            slidingWall, inWater, swimming))
        }
    }

    fun sendPos(uuid: UUID,
                pos: Vector3d,
                forced: Boolean,
                packetHandler: (PacketBoth) -> Unit = this.packetHandler) {
        val oldPos: Vector3d?
        if (forced) {
            oldPos = null
        } else {
            oldPos = sentPos.now()
        }
        sentPos.set(pos)
        packetHandler(
                PacketMobMoveAbsolute(registry, uuid, oldPos, pos.x, pos.y,
                        pos.z))
    }

    fun sendRotation(uuid: UUID,
                     rot: Vector3d,
                     forced: Boolean,
                     packetHandler: (PacketBoth) -> Unit = this.packetHandler) {
        sentRot.set(rot)
        packetHandler(PacketMobChangeRot(registry, uuid,
                if (forced) null else sentPos.now(), rot.floatX(), rot.floatY(),
                rot.floatZ()))
    }

    fun sendSpeed(uuid: UUID,
                  speed: Vector3d,
                  forced: Boolean,
                  packetHandler: (PacketBoth) -> Unit = this.packetHandler) {
        sentSpeed.set(speed)
        packetHandler(PacketMobChangeSpeed(registry, uuid,
                if (forced) null else sentPos.now(), speed.x, speed.y, speed.z))
    }

    companion object {
        private val POSITION_SEND_OFFSET = 0.01
        private val SPEED_SEND_OFFSET = 0.05
        private val DIRECTION_SEND_OFFSET = 12.25
        private val FORCE_TIME = 10000000000
        private val FORCE_TIME_RANDOM = 1250000000
    }
}
