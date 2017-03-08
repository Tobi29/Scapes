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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.server.MobLivingEquippedServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import java.util.concurrent.ThreadLocalRandom

class MobSkeletonServer(type: EntityType<*, *>,
                        world: WorldServer) : MobLivingEquippedServer(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 20.0, 30.0,
        Frustum(90.0, 1.0, 0.1, 24.0), Frustum(20.0, 0.5, 0.1, 0.2)) {
    private var lookWait = 0.0
    private var walkWait = 0.0
    private var hitWait = 0.0

    init {
        val random = ThreadLocalRandom.current()
        onNotice("Local") { mob ->
            if (mob is MobPlayerServer && !ai.hasMobTarget()) {
                ai.setMobTarget(mob, 10.0)
            }
        }
        onDamage("Local") { damage ->
            world.playSound("VanillaBasics:sound/entity/mob/skeleton/Hurt" +
                    (random.nextInt(3) + 1) + ".ogg", this)
        }
    }

    override fun canMoveHere(terrain: TerrainServer,
                             x: Int,
                             y: Int,
                             z: Int): Boolean {
        if (terrain.light(x, y, z) < 7) {
            if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                    terrain.type(x, y, z).isTransparent(terrain, x, y, z) &&
                    !terrain.type(x, y, z + 1).isSolid(terrain, x, y, z + 1) &&
                    terrain.type(x, y, z + 1).isTransparent(terrain, x, y,
                            z + 1) &&
                    terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1) &&
                    !terrain.type(x, y, z - 1).isTransparent(terrain, x, y,
                            z - 1)) {
                return true
            }
        }
        return false
    }

    override fun creatureType(): CreatureType {
        return CreatureType.MONSTER
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.7)
    }

    override fun update(delta: Double) {
        if (isSwimming) {
            speed.plusZ(1.2)
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
            val random = ThreadLocalRandom.current()
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
        speed.plusX(cosTable(rot.doubleZ().toRad()) * walkSpeed)
        speed.plusY(sinTable(rot.doubleZ().toRad()) * walkSpeed)
        if (world.terrain.light(pos.intX(), pos.intY(),
                floor(pos.doubleZ() + 0.7)) > 8) {
            damage(1.0)
        }
    }

    override fun leftWeapon(): ItemStack {
        return ItemStack(registry)
    }

    override fun rightWeapon(): ItemStack {
        return ItemStack(registry)
    }

    private fun findWalkPosition(): Vector3d? {
        val random = ThreadLocalRandom.current()
        val vector3d = pos.now().plus(
                Vector3d((random.nextInt(17) - 8).toDouble(),
                        (random.nextInt(17) - 8).toDouble(),
                        (random.nextInt(7) - 3).toDouble()))
        if (canMoveHere(world.terrain, vector3d.intX(), vector3d.intY(),
                vector3d.intZ())) {
            return vector3d
        }
        return null
    }
}
