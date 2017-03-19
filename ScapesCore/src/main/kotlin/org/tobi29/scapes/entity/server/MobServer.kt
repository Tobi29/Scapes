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

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.tag.ReadWriteTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.floor
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityPhysics
import org.tobi29.scapes.entity.EntityType

abstract class MobServer(type: EntityType<*, *>,
                         world: WorldServer,
                         pos: Vector3d,
                         speed: Vector3d,
                         protected val collision: AABB) : EntityAbstractServer(
        type, world, pos) {
    protected val speed: MutableVector3d
    protected val rot = MutableVector3d()
    protected val positionSender: MobPositionSenderServer
    val isOnGround: Boolean
        get() = physicsState.isOnGround
    val isInWater: Boolean
        get() = physicsState.isInWater
    val isSwimming: Boolean
        get() = physicsState.isSwimming
    var isHeadInWater = false
        protected set
    protected var gravitationMultiplier = 1.0
    protected var airFriction = 0.2
    protected var groundFriction = 1.6
    protected var wallFriction = 2.0
    protected var waterFriction = 8.0
    protected var stepHeight = 1.0
    protected var physicsState = EntityPhysics.PhysicsState()

    init {
        this.speed = MutableVector3d(speed)
        positionSender = createPositionHandler()
    }

    override fun getAABB(): AABB {
        val aabb = AABB(collision)
        aabb.add(pos.doubleX(), pos.doubleY(), pos.doubleZ())
        return aabb
    }

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Speed"] = speed.now().toTag()
        map["Rot"] = rot.now().toTag()
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Speed"]?.toMap()?.let { speed.set(it) }
        map["Rot"]?.toMap()?.let { rot.set(it) }
        updatePosition()
    }

    protected open fun createPositionHandler(): MobPositionSenderServer {
        return MobPositionSenderServer(registry, pos.now(), { world.send(it) })
    }

    fun speed(): Vector3d {
        return speed.now()
    }

    open fun setSpeed(speed: Vector3d) {
        assert(world.checkThread() || !world.hasEntity(this))
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
        assert(world.checkThread() || !world.hasEntity(this))
        speed.plusX(x).plusY(y).plusZ(z)
        positionSender.sendSpeed(uuid, speed.now(), true)
    }

    override fun setPos(pos: Vector3d) {
        assert(world.checkThread() || !world.hasEntity(this))
        synchronized(this.pos) {
            this.pos.set(pos)
            positionSender.sendPos(uuid, this.pos.now(), true)
        }
    }

    fun updatePosition() {
        positionSender.submitUpdate(uuid, pos.now(), speed.now(), rot.now(),
                physicsState.isOnGround, physicsState.slidingWall,
                physicsState.isInWater, isSwimming, true)
    }

    open fun move(delta: Double) {
        if (!world.terrain.isBlockTicking(pos.intX(), pos.intY(), pos.intZ())) {
            return
        }
        EntityPhysics.updateVelocity(delta, speed, world.gravity,
                gravitationMultiplier, airFriction, groundFriction,
                waterFriction, wallFriction, physicsState)
        val aabb = getAABB()
        val aabbs = AABBS.get()
        EntityPhysics.collisions(delta, speed, world.terrain, aabb,
                stepHeight, aabbs)
        EntityPhysics.move(delta, pos, speed, aabb, stepHeight,
                physicsState, aabbs)
        if (isOnGround) {
            speed.setZ(speed.doubleZ() / (1.0 + 4.0 * delta))
        }
        EntityPhysics.collide(delta, aabb, aabbs, physicsState) {
            it.collision.inside(this, delta)
        }
        isHeadInWater = world.terrain.type(pos.intX(), pos.intY(),
                floor(pos.doubleZ() + 0.7)).isLiquid
        positionSender.submitUpdate(uuid, pos.now(), speed.now(), rot.now(),
                physicsState.isOnGround, physicsState.slidingWall,
                physicsState.isInWater, physicsState.isSwimming)
        aabbs.reset()
    }

    companion object {
        // Kotlin limitation, has to be JvmStatic
        @JvmStatic
        protected val AABBS = ThreadLocal { Pool { AABBElement() } }
    }
}
