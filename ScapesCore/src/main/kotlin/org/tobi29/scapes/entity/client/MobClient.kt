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

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.math.AABB
import org.tobi29.math.vector.MutableVector3d
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityPhysics
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.Mob
import org.tobi29.scapes.entity.MobPositionReceiver
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toMap
import org.tobi29.stdex.math.floorToInt

abstract class MobClient(type: EntityType<*, *>,
                         world: WorldClient,
                         pos: Vector3d,
                         speed: Vector3d,
                         protected val collision: AABB) : EntityAbstractClient(
        type, world, pos),
        Mob,
        MobileEntityClient {
    protected val speed: MutableVector3d
    protected val rot = MutableVector3d()
    override val positionReceiver: MobPositionReceiver
    val isOnGround: Boolean
        get() = physicsState.isOnGround
    val isInWater: Boolean
        get() = physicsState.isInWater
    val isSwimming: Boolean
        get() = physicsState.isSwimming
    var isHeadInWater = false
        protected set
    protected var physicsState = EntityPhysics.PhysicsState()

    init {
        this.speed = MutableVector3d(speed)
        positionReceiver = MobPositionReceiver(this.pos.now(),
                { this.pos.set(it) },
                { this.speed.set(it) },
                { this.rot.set(it) },
                { ground, slidingWall, inWater, swimming ->
                    physicsState.isOnGround = ground
                    physicsState.slidingWall = slidingWall
                    physicsState.isInWater = inWater
                    physicsState.isSwimming = swimming
                })
    }

    override fun getAABB(): AABB {
        val aabb = AABB(collision)
        aabb.add(pos.x, pos.y, pos.z)
        return aabb
    }

    override fun read(map: TagMap) {
        super.read(map)
        positionReceiver.receiveMoveAbsolute(pos.x, pos.y,
                pos.z)
        map["Speed"]?.toMap()?.let { speed.set(it) }
        map["Rot"]?.toMap()?.let { rot.set(it) }
    }

    fun speed(): Vector3d {
        return speed.now()
    }

    fun rot(): Vector3d {
        return rot.now()
    }

    fun speedX(): Double {
        return speed.x
    }

    fun pitch(): Double {
        return rot.x
    }

    fun speedY(): Double {
        return speed.y
    }

    fun speedZ(): Double {
        return speed.z
    }

    fun yaw(): Double {
        return rot.z
    }

    open fun move(delta: Double) {
        isHeadInWater = world.terrain.type(pos.x.floorToInt(),
                pos.y.floorToInt(),
                (pos.z + 0.7).floorToInt()).isLiquid
    }
}
