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

import org.tobi29.math.*
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.plus
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.chunk.terrain.block
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.server.MobLivingEquippedServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.toRad

class MobSkeletonServer(type: EntityType<*, *>,
                        world: WorldServer) : MobLivingEquippedServer(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 20.0, 30.0,
        Frustum(90.0, 1.0, 0.1, 24.0), Frustum(20.0, 0.5, 0.1, 0.2)) {
    private var lookWait = 0.0
    private var walkWait = 0.0
    private var hitWait = 0.0

    init {
        val random = threadLocalRandom()
        registerComponent(CreatureType.COMPONENT, CreatureType.MONSTER)
        onNotice[SKELETON_LISTENER_TOKEN] = { mob ->
            if (mob is MobPlayerServer && !ai.hasMobTarget()) {
                ai.setMobTarget(mob, 10.0)
            }
        }
        onDamage[SKELETON_LISTENER_TOKEN] = { damage ->
            world.playSound(
                    "VanillaBasics:sound/entity/mob/skeleton/Hurt${random.nextInt(
                            3) + 1}.ogg", this)
        }
    }

    override fun canMoveHere(terrain: TerrainServer,
                             x: Int,
                             y: Int,
                             z: Int): Boolean {
        if (terrain.light(x, y, z) < 7 &&
                terrain.block(x, y, z) {
                    !isSolid(it) && isTransparent(it)
                } &&
                terrain.block(x, y, z + 1) {
                    !isSolid(it) && isTransparent(it)
                } &&
                terrain.block(x, y, z - 1) {
                    isSolid(it) && !isTransparent(it)
                }) {
            return true
        }
        return false
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.7)
    }

    override fun update(delta: Double) {
        if (isSwimming) {
            speed.addZ(1.2)
            physicsState.isOnGround = false
        }
        ai.update(delta)
        var walkSpeed = 0.0
        hitWait -= delta
        if (hitWait <= 0.0) {
            hitWait = 0.5
            attack(20.0)
        }
        if (ai.hasTarget()) {
            walkSpeed = 80.0
            rot.setZ(ai.targetYaw())
        } else {
            walkWait -= delta
            if (walkWait <= 0.0) {
                walkWait = 0.2
                findWalkPosition()?.let { ai.setPositionTarget(it, 160.0) }
            }
        }
        lookWait -= delta
        if (lookWait <= 0.0) {
            val random = threadLocalRandom()
            lookWait = random.nextDouble() * 8.0 + 1.0
            rot.setX(random.nextDouble() * 40.0 - 20.0)
        }
        if (!isOnGround && !physicsState.slidingWall && !isInWater) {
            walkSpeed *= 0.0006
        } else if (!isOnGround && !isInWater) {
            walkSpeed *= 0.05
        } else if (isInWater) {
            walkSpeed *= 0.2
        }
        walkSpeed *= delta
        speed.addX(cosTable(rot.z.toRad()) * walkSpeed)
        speed.addY(sinTable(rot.z.toRad()) * walkSpeed)
        if (world.terrain.light(pos.x.floorToInt(), pos.y.floorToInt(),
                (pos.z + 0.7).floorToInt()) > 8) {
            damage(1.0)
        }
    }

    private fun findWalkPosition(): Vector3d? {
        val random = threadLocalRandom()
        val vector3d = pos.now().plus(
                Vector3d((random.nextInt(17) - 8).toDouble(),
                        (random.nextInt(17) - 8).toDouble(),
                        (random.nextInt(7) - 3).toDouble()))
        if (canMoveHere(world.terrain, vector3d.x.floorToInt(), vector3d.y.floorToInt(),
                vector3d.z.floorToInt())) {
            return vector3d
        }
        return null
    }
}

private val SKELETON_LISTENER_TOKEN = ListenerToken("VanillaBasics:Skeleton")
