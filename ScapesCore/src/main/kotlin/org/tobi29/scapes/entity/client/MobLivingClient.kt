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
import org.tobi29.scapes.chunk.terrain.selectBlock
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.ConcurrentMap
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.*
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.MobLiving
import org.tobi29.scapes.packets.PacketMobDamage

abstract class MobLivingClient(type: EntityType<*, *>,
                               world: WorldClient,
                               pos: Vector3d,
                               speed: Vector3d,
                               aabb: AABB,
                               protected var health: Double,
                               protected var maxHealth: Double) : MobClient(
        type, world, pos, speed, aabb), MobLiving {
    override final val onNotice: ConcurrentMap<ListenerToken, (MobClient) -> Unit> = ConcurrentHashMap()
    override final val onJump: ConcurrentMap<ListenerToken, () -> Unit> = ConcurrentHashMap()
    override final val onHeal: ConcurrentMap<ListenerToken, (Double) -> Unit> = ConcurrentHashMap()
    override final val onDamage: ConcurrentMap<ListenerToken, (Double) -> Unit> = ConcurrentHashMap()
    override final val onDeath: ConcurrentMap<ListenerToken, () -> Unit> = ConcurrentHashMap()
    protected var footStep = 0.0
    protected var invincibleTicks = 0.0

    fun block(distance: Double,
              direction: Vector2d): PointerPane? {
        return world.terrain.selectBlock(pos.now() + viewOffset(), distance,
                rot.now().xz + direction)
    }

    fun notice(notice: MobClient) {
        onNotice.values.forEach { it(notice) }
    }

    fun heal(heal: Double) {
        onHeal.values.forEach { it(heal) }
    }

    fun damage(damage: Double) {
        onDamage.values.forEach { it(damage) }
    }

    fun death() {
        onDeath.values.forEach { it() }
    }

    fun health(): Double {
        return health
    }

    fun maxHealth(): Double {
        return maxHealth
    }

    val isDead: Boolean
        get() = health <= 0

    override fun read(map: TagMap) {
        super.read(map)
        map["Health"]?.toDouble()?.let { health = it }
        map["MaxHealth"]?.toDouble()?.let { maxHealth = it }
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
                    val random = threadLocalRandom()
                    world.playSound(footStepSound, this,
                            0.9 + random.nextDouble() * 0.2, 1.0)
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
            damage(oldLives - newLives)
        } else {
            health = newLives
            heal(newLives - oldLives)
        }
    }
}
