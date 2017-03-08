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
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.util.dropItem
import java.util.concurrent.ThreadLocalRandom

class MobPigServer(type: EntityType<*, *>,
                   world: WorldServer) : MobLivingServer(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.45, -0.45, -0.6875, 0.45, 0.45, 0.375), 20.0, 30.0,
        Frustum(90.0, 1.0, 0.1, 24.0), Frustum(20.0, 0.5, 0.1, 0.2)) {
    private var soundWait = 0.0
    private var lookWait = 0.0
    private var walkWait = 0.0

    init {
        val random = ThreadLocalRandom.current()
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        val materials = plugin.materials
        onDamage("Local") { damage ->
            world.playSound("VanillaBasics:sound/entity/mob/pig/Hurt" +
                    (random.nextInt(2) + 1) + ".ogg", this)
        }
        onDeath("Local") {
            world.dropItem(ItemStack(materials.meat, 0, random.nextInt(7) + 10),
                    this.pos.now())
        }
    }

    override fun canMoveHere(terrain: TerrainServer,
                             x: Int,
                             y: Int,
                             z: Int): Boolean {
        if (terrain.light(x, y, z) >= 7) {
            if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                    terrain.type(x, y, z).isTransparent(terrain, x, y, z) &&
                    terrain.type(x, y, z - 1).isSolid(terrain, x, y, z - 1) &&
                    !terrain.type(x, y, z - 1).isTransparent(terrain, x, y,
                            z - 1)) {
                return true
            }
        }
        return false
    }

    override fun creatureType(): CreatureType {
        return CreatureType.CREATURE
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.2)
    }

    override fun update(delta: Double) {
        // Movement
        if (isSwimming) {
            speed.plusZ(1.2)
            physicsState.isOnGround = false
        }
        ai.update(delta)
        var walkSpeed = 0.0
        if (ai.hasTarget()) {
            walkSpeed = 40.0
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
        soundWait -= delta
        if (soundWait <= 0.0) {
            val random = ThreadLocalRandom.current()
            soundWait = random.nextDouble() * 12.0 + 3.0
            world.playSound("VanillaBasics:sound/entity/mob/pig/Calm" +
                    (random.nextInt(2) + 1) + ".ogg", this)
        }
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
