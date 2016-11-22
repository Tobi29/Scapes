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
package org.tobi29.scapes.entity.server

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d

abstract class MobServer protected constructor(world: WorldServer, pos: Vector3d, speed: Vector3d, protected val collision: AABB) : EntityServer(
        world, pos) {
    protected val speed: MutableVector3d
    protected val rot = MutableVector3d()
    protected val positionSender: MobPositionSenderServer
    var isOnGround = false
        protected set
    var isInWater = false
        protected set
    var isSwimming = false
        protected set
    var isHeadInWater = false
        protected set
    protected var slidingWall = false
    protected var swim = 0
    protected var gravitationMultiplier = 1.0
    protected var airFriction = 0.2
    protected var groundFriction = 1.6
    protected var wallFriction = 2.0
    protected var waterFriction = 8.0
    protected var stepHeight = 1.0

    init {
        this.speed = MutableVector3d(speed)
        positionSender = createPositionHandler()
    }

    protected fun updateVelocity(gravitation: Double,
                                 delta: Double) {
        speed.div(1.0 + airFriction * delta)
        if (isInWater) {
            speed.div(1.0 + waterFriction * delta)
        } else {
            if (isOnGround) {
                speed.div(1.0 + groundFriction * delta * gravitation)
            }
            if (slidingWall) {
                speed.div(1.0 + wallFriction * delta)
            }
        }
        speed.plusZ(-gravitation * gravitationMultiplier * delta)
    }

    protected fun move(aabb: AABB,
                       aabbs: Pool<AABBElement>,
                       goX: Double,
                       goY: Double,
                       goZ: Double) {
        var goX = goX
        var goY = goY
        var ground = false
        var slidingWall = false
        val lastGoZ = aabb.moveOutZ(collisions(aabbs), goZ)
        pos.plusZ(lastGoZ)
        aabb.add(0.0, 0.0, lastGoZ)
        if (lastGoZ - goZ > 0) {
            ground = true
        }
        // Walk
        var walking = true
        while (walking) {
            walking = false
            if (goX != 0.0) {
                val lastGoX = aabb.moveOutX(collisions(aabbs), goX)
                if (lastGoX != 0.0) {
                    pos.plusX(lastGoX)
                    aabb.add(lastGoX, 0.0, 0.0)
                    goX -= lastGoX
                    walking = true
                }
            }
            if (goY != 0.0) {
                val lastGoY = aabb.moveOutY(collisions(aabbs), goY)
                if (lastGoY != 0.0) {
                    pos.plusY(lastGoY)
                    aabb.add(0.0, lastGoY, 0.0)
                    goY -= lastGoY
                    walking = true
                }
            }
        }
        // Check collision
        val slidingX = goX != 0.0
        val slidingY = goY != 0.0
        if (slidingX || slidingY) {
            if (stepHeight > 0.0 && (this.isOnGround || isInWater)) {
                // Step
                // Calculate step height
                var aabbStep = AABB(aabb).add(goX, 0.0, 0.0)
                val stepX = aabbStep.moveOutZ(collisions(aabbs), stepHeight)
                aabbStep = AABB(aabb).add(0.0, goY, 0.0)
                val stepY = aabbStep.moveOutZ(collisions(aabbs), stepHeight)
                var step = max(stepX, stepY)
                aabbStep = AABB(aabb).add(goX, goY, step)
                step += aabbStep.moveOutZ(collisions(aabbs), -step)
                // Check step height
                aabbStep.copy(aabb).add(0.0, 0.0, step)
                step = aabb.moveOutZ(collisions(aabbs), step)
                // Attempt walk at new height
                val lastGoX = aabbStep.moveOutX(collisions(aabbs), goX)
                aabbStep.add(lastGoX, 0.0, 0.0)
                val lastGoY = aabbStep.moveOutY(collisions(aabbs), goY)
                // Check if walk was successful
                if (lastGoX != 0.0 || lastGoY != 0.0) {
                    pos.plusX(lastGoX)
                    pos.plusY(lastGoY)
                    aabb.copy(aabbStep).add(0.0, lastGoY, 0.0)
                    pos.plusZ(step)
                } else {
                    // Collide
                    slidingWall = true
                    if (slidingX) {
                        speed.setX(0.0)
                    }
                    if (slidingY) {
                        speed.setY(0.0)
                    }
                }
            } else {
                // Collide
                slidingWall = true
                if (slidingX) {
                    speed.setX(0.0)
                }
                if (slidingY) {
                    speed.setY(0.0)
                }
            }
        }
        this.isOnGround = ground
        this.slidingWall = slidingWall
    }

    protected fun collide(aabb: AABB,
                          aabbs: Pool<AABBElement>,
                          delta: Double) {
        var inWater = false
        val swimming: Boolean
        for (element in aabbs) {
            if (aabb.overlay(element.aabb)) {
                element.collision.inside(this, delta)
                if (element.collision.isLiquid) {
                    inWater = true
                }
            }
        }
        aabb.minZ = mix(aabb.minZ, aabb.maxZ, 0.6)
        val water = aabbs.any { aabb.overlay(it.aabb) && it.collision.isLiquid }
        if (water) {
            swim++
            swimming = swim > 1
        } else {
            swimming = false
            swim = 0
        }
        this.isInWater = inWater
        this.isSwimming = swimming
    }

    fun dropItem(item: ItemStack) {
        val entity = MobItemServer(world, pos.now(), Vector3d(
                cos(rot.doubleZ().toRad()) * 10.0 *
                        cos(rot.doubleX().toRad()),
                sin(rot.doubleZ().toRad()) * 10.0 *
                        cos(rot.doubleX().toRad()),
                sin(rot.doubleX().toRad()) * 0.3 + 0.3),
                item, Double.NaN)
        world.addEntityNew(entity)
    }

    override fun getAABB(): AABB {
        val aabb = AABB(collision)
        aabb.add(pos.doubleX(), pos.doubleY(), pos.doubleZ())
        return aabb
    }

    override fun write(): TagStructure {
        val tagStructure = super.write()
        tagStructure.setMultiTag("Speed", speed)
        tagStructure.setMultiTag("Rot", rot)
        return tagStructure
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getMultiTag("Speed", speed)
        tagStructure.getMultiTag("Rot", rot)
        updatePosition()
    }

    protected open fun createPositionHandler(): MobPositionSenderServer {
        return MobPositionSenderServer(pos.now(), { world.send(it) })
    }

    fun speed(): Vector3d {
        return speed.now()
    }

    open fun setSpeed(speed: Vector3d) {
        assert(world.checkThread())
        this.speed.set(speed)
        positionSender.sendSpeed(uuid, this.speed.now(), true)
    }

    fun rot(): Vector3d {
        return rot.now()
    }

    open fun setRot(rot: Vector3d) {
        this.rot.set(rot)
        positionSender.sendRotation(uuid, this.rot.now(), true)
    }

    open fun push(x: Double,
                  y: Double,
                  z: Double) {
        assert(world.checkThread())
        speed.plusX(x).plusY(y).plusZ(z)
        positionSender.sendSpeed(uuid, speed.now(), true)
    }

    open fun setPos(pos: Vector3d) {
        assert(world.checkThread())
        synchronized(this.pos) {
            this.pos.set(pos)
            positionSender.sendPos(uuid, this.pos.now(), true)
        }
    }

    fun updatePosition() {
        positionSender.submitUpdate(uuid, pos.now(), speed.now(), rot.now(),
                isOnGround,
                slidingWall, isInWater, isSwimming, true)
    }

    open fun move(delta: Double) {
        if (!world.terrain.isBlockTicking(pos.intX(), pos.intY(), pos.intZ())) {
            return
        }
        updateVelocity(world.gravity, delta)
        val goX = clamp(speed.doubleX() * delta, -1.0, 1.0)
        val goY = clamp(speed.doubleY() * delta, -1.0, 1.0)
        val goZ = clamp(speed.doubleZ() * delta, -1.0, 1.0)
        val aabb = getAABB()
        val aabbs = world.terrain.collisions(
                floor(aabb.minX + min(goX, 0.0)),
                floor(aabb.minY + min(goY, 0.0)),
                floor(aabb.minZ + min(goZ, 0.0)),
                floor(aabb.maxX + max(goX, 0.0)),
                floor(aabb.maxY + max(goY, 0.0)),
                floor(aabb.maxZ + max(goZ, stepHeight)))
        move(aabb, aabbs, goX, goY, goZ)
        if (isOnGround) {
            speed.setZ(speed.doubleZ() / (1.0 + 4.0 * delta))
        }
        isHeadInWater = world.terrain.type(pos.intX(), pos.intY(),
                floor(pos.doubleZ() + 0.7)).isLiquid
        collide(aabb, aabbs, delta)
        positionSender.submitUpdate(uuid, pos.now(), speed.now(), rot.now(),
                isOnGround,
                slidingWall, isInWater, isSwimming)
    }

    companion object {

        protected fun collisions(aabbs: Pool<AABBElement>): Iterator<AABB> {
            return aabbs.stream().filter { it.isSolid }.map { it.aabb() }.iterator()
        }
    }
}
