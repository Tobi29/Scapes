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
package org.tobi29.scapes.entity.particle

import org.tobi29.math.AABB
import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.chunk.terrain.collisions
import org.tobi29.scapes.entity.EntityPhysics
import org.tobi29.stdex.ThreadLocal
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.floorToInt
import org.tobi29.utils.Pool
import org.tobi29.utils.forAllObjects
import kotlin.math.max
import kotlin.math.min

object ParticlePhysics {
    fun update(delta: Double,
               instance: ParticleInstance,
               terrain: TerrainClient,
               aabb: AABB,
               gravitation: Float,
               gravitationMultiplier: Float,
               airFriction: Float,
               groundFriction: Float,
               waterFriction: Float): Boolean {
        val goX = clamp(instance.speed.x * delta, -10.0, 10.0)
        val goY = clamp(instance.speed.y * delta, -10.0, 10.0)
        val goZ = clamp(instance.speed.z * delta, -10.0, 10.0)
        val collisions = AABBS.get()
        terrain.collisions(
                (aabb.minX + min(goX, 0.0)).floorToInt(),
                (aabb.minY + min(goY, 0.0)).floorToInt(),
                (aabb.minZ + min(goZ, 0.0)).floorToInt(),
                (aabb.maxX + max(goX, 0.0)).floorToInt(),
                (aabb.maxY + max(goY, 0.0)).floorToInt(),
                (aabb.maxZ + max(goZ, 0.0)).floorToInt(), collisions)
        val aabbs = EntityPhysics.collisions(collisions)
        val lastGoZ = aabb.moveOutZ(aabbs, goZ)
        instance.pos.addZ(lastGoZ)
        aabb.add(0.0, 0.0, lastGoZ)
        val ground = lastGoZ - goZ > 0.0
        val lastGoX = aabb.moveOutX(aabbs, goX)
        instance.pos.addX(lastGoX)
        aabb.add(lastGoX, 0.0, 0.0)
        if (lastGoX != goX) {
            instance.speed.setX(0.0)
        }
        val lastGoY = aabb.moveOutY(aabbs, goY)
        instance.pos.addY(lastGoY)
        aabb.add(0.0, lastGoY, 0.0)
        if (lastGoY != goY) {
            instance.speed.setY(0.0)
        }
        var inWater = false
        for (element in collisions) {
            if (aabb.overlay(element.aabb)) {
                element.collision.inside(instance, delta)
                if (element.collision.isLiquid) {
                    inWater = true
                }
            }
        }
        instance.speed.addZ(
                (-gravitationMultiplier).toDouble() * delta * gravitation.toDouble())
        instance.speed.divide(1.0 + airFriction * delta)
        if (inWater) {
            instance.speed.divide(1.0 + waterFriction * delta)
        } else if (ground) {
            instance.speed.setZ(0.0)
            instance.speed.divide(
                    1.0 + groundFriction.toDouble() * delta * gravitation.toDouble())
        }
        collisions.reset()
        collisions.forAllObjects { it.collision = BlockType.STANDARD_COLLISION }
        return ground
    }

    private val AABBS = ThreadLocal { Pool { AABBElement() } }
}
