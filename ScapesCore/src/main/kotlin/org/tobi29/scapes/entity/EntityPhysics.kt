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

package org.tobi29.scapes.entity

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.chunk.terrain.Terrain
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d

object EntityPhysics {
    fun collisions(aabbs: Pool<AABBElement>): Sequence<AABB> {
        return aabbs.asSequence().filter { it.isSolid }.map { it.aabb() }
    }

    fun collisions(delta: Double,
                   speed: MutableVector3d,
                   terrain: Terrain,
                   aabb: AABB,
                   stepHeight: Double,
                   aabbs: Pool<AABBElement>) {
        val goX = clamp(speed.doubleX() * delta, -10.0, 10.0)
        val goY = clamp(speed.doubleY() * delta, -10.0, 10.0)
        val goZ = clamp(speed.doubleZ() * delta, -10.0, 10.0)
        terrain.collisions(
                floor(aabb.minX + min(goX, 0.0)),
                floor(aabb.minY + min(goY, 0.0)),
                floor(aabb.minZ + min(goZ, 0.0)),
                floor(aabb.maxX + max(goX, 0.0)),
                floor(aabb.maxY + max(goY, 0.0)),
                floor(aabb.maxZ + max(goZ, stepHeight)), aabbs)
    }

    fun move(delta: Double,
             pos: MutableVector3d,
             speed: MutableVector3d,
             aabb: AABB,
             stepHeight: Double,
             state: PhysicsState,
             collisions: Pool<AABBElement>) {
        val aabbs = collisions(collisions)
        var goX = clamp(speed.doubleX() * delta, -10.0, 10.0)
        var goY = clamp(speed.doubleY() * delta, -10.0, 10.0)
        val goZ = clamp(speed.doubleZ() * delta, -10.0, 10.0)
        var ground = false
        var slidingWall = false
        val lastGoZ = aabb.moveOutZ(aabbs, goZ)
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
                val lastGoX = aabb.moveOutX(aabbs, goX)
                if (lastGoX != 0.0) {
                    pos.plusX(lastGoX)
                    aabb.add(lastGoX, 0.0, 0.0)
                    goX -= lastGoX
                    walking = true
                }
            }
            if (goY != 0.0) {
                val lastGoY = aabb.moveOutY(aabbs, goY)
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
            if (stepHeight > 0.0 && (state.isOnGround || state.isInWater)) {
                // Step
                // Calculate step height
                var aabbStep = AABB(aabb).add(goX, 0.0, 0.0)
                val stepX = aabbStep.moveOutZ(aabbs, stepHeight)
                aabbStep = AABB(aabb).add(0.0, goY, 0.0)
                val stepY = aabbStep.moveOutZ(aabbs, stepHeight)
                var step = max(stepX, stepY)
                aabbStep = AABB(aabb).add(goX, goY, step)
                step += aabbStep.moveOutZ(aabbs, -step)
                // Check step height
                aabbStep.copy(aabb).add(0.0, 0.0, step)
                step = aabb.moveOutZ(aabbs, step)
                // Attempt walk at new height
                val lastGoX = aabbStep.moveOutX(aabbs, goX)
                aabbStep.add(lastGoX, 0.0, 0.0)
                val lastGoY = aabbStep.moveOutY(aabbs, goY)
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
        state.isOnGround = ground
        state.slidingWall = slidingWall
    }

    fun updateVelocity(delta: Double,
                       speed: MutableVector3d,
                       gravitation: Double,
                       gravitationMultiplier: Double,
                       airFriction: Double,
                       groundFriction: Double,
                       waterFriction: Double,
                       wallFriction: Double,
                       state: PhysicsState) {
        speed.div(1.0 + airFriction * delta)
        if (state.isInWater) {
            speed.div(1.0 + waterFriction * delta)
        } else {
            if (state.isOnGround) {
                speed.div(1.0 + groundFriction * delta * gravitation)
            }
            if (state.slidingWall) {
                val div = 1.0 + wallFriction * delta
                if (speed.z > 0.0) {
                    speed.div(Vector3d(div, div, 0.0))
                } else {
                    speed.div(div)
                }
            }
        }
        speed.plusZ(-gravitation * gravitationMultiplier * delta)
    }

    fun collide(delta: Double,
                aabb: AABB,
                collisions: Pool<AABBElement>,
                state: PhysicsState,
                inside: (AABBElement) -> Unit) {
        var inWater = false
        val swimming: Boolean
        for (element in collisions) {
            if (aabb.overlay(element.aabb)) {
                inside(element)
                if (element.collision.isLiquid) {
                    inWater = true
                }
            }
        }
        aabb.minZ = mix(aabb.minZ, aabb.maxZ, 0.6)
        val water = collisions.any {
            aabb.overlay(it.aabb) && it.collision.isLiquid
        }
        if (water) {
            state.swim += 20.0 * delta
            swimming = state.swim > 1
        } else {
            swimming = false
            state.swim = 0.0
        }
        state.isInWater = inWater
        state.isSwimming = swimming
    }

    class PhysicsState(var isOnGround: Boolean = false,
                       var isInWater: Boolean = false,
                       var slidingWall: Boolean = false,
                       var isSwimming: Boolean = false) {
        internal var swim = 0.0
    }
}
