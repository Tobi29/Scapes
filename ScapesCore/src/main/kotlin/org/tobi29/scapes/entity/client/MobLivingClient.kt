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

package org.tobi29.scapes.entity.client

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.model.MobLivingModel
import org.tobi29.scapes.packets.PacketMobDamage
import java.util.concurrent.ThreadLocalRandom

abstract class MobLivingClient(world: WorldClient, pos: Vector3d, speed: Vector3d,
                               aabb: AABB, protected var health: Double, protected var maxHealth: Double) : MobClient(
        world, pos, speed, aabb) {
    protected var footStep = 0.0
    protected var invincibleTicks = 0.0

    fun block(distance: Double,
              direction: Vector2d): PointerPane? {
        val pointerPanes = world.terrain.pointerPanes(pos.intX(), pos.intY(),
                pos.intZ(),
                ceil(distance))
        val rotX = rot.doubleX() + direction.y
        val rotZ = rot.doubleZ() + direction.x
        val factor = cos(rotX.toRad()) * 6
        val lookX = cos(rotZ.toRad()) * factor
        val lookY = sin(rotZ.toRad()) * factor
        val lookZ = sin(rotX.toRad()) * 6
        val viewOffset = viewOffset()
        val f = pos.now().plus(viewOffset)
        val t = f.plus(Vector3d(lookX, lookY, lookZ))
        var distanceSqr = distance * distance
        var closest: PointerPane? = null
        for (pane in pointerPanes) {
            val intersection = Intersection.intersectPointerPane(f, t, pane)
            if (intersection != null) {
                val check = f.distance(intersection)
                if (check < distanceSqr) {
                    closest = pane
                    distanceSqr = check
                }
            }
        }
        pointerPanes.reset()
        return closest
    }

    fun onHeal(heal: Double) {
    }

    open fun onDamage(damage: Double) {
    }

    open fun onDeath() {
    }

    abstract fun creatureType(): CreatureType

    fun health(): Double {
        return health
    }

    fun maxHealth(): Double {
        return maxHealth
    }

    val isDead: Boolean
        get() = health <= 0

    override fun createModel(): MobLivingModel? {
        return null
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getDouble("Health")?.let { health = it }
        tagStructure.getDouble("MaxHealth")?.let { maxHealth = it }
    }

    override fun move(delta: Double) {
        super.move(delta)
        footStep -= delta
        if (footStep <= 0.0) {
            footStep = 0.0
            val currentSpeed = speed()
            if (max(abs(currentSpeed.x),
                    abs(currentSpeed.y)) > 0.1) {
                val x = pos.intX()
                val y = pos.intY()
                val block = world.terrain.block(x, y,
                        floor(pos.doubleZ() - 0.1))
                var footStepSound = world.terrain.type(block).footStepSound(
                        world.terrain.data(block))
                if (footStepSound == null && isOnGround) {
                    val blockBottom = world.terrain.block(x, y,
                            floor(pos.doubleZ() - 1.4))
                    footStepSound = world.terrain.type(
                            blockBottom).footStepSound(
                            world.terrain.data(blockBottom))
                }
                if (footStepSound != null) {
                    val random = ThreadLocalRandom.current()
                    world.playSound(footStepSound, this,
                            0.9f + random.nextFloat() * 0.2f, 1.0f)
                    footStep = 1.0 / clamp(speed.now().length(), 1.0, 4.0)
                }
            }
        }
        if (invincibleTicks > 0.0) {
            invincibleTicks = max(invincibleTicks - delta, 0.0)
        }
    }

    abstract fun viewOffset(): Vector3d

    fun invincibleTicks(): Double {
        return invincibleTicks
    }

    fun processPacket(packet: PacketMobDamage) {
        maxHealth = packet.maxHealth()
        val newLives = packet.health()
        val oldLives = health
        if (newLives < oldLives) {
            invincibleTicks = 0.8
            health = newLives
            onDamage(oldLives - newLives)
        } else {
            health = newLives
            onHeal(newLives - oldLives)
        }
    }
}