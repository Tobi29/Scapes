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

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.chunk.terrain.selectBlock
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.engine.utils.math.vector.xz
import org.tobi29.scapes.engine.utils.tag.ReadWriteTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ai.AI
import org.tobi29.scapes.entity.ai.SimpleAI
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.packets.PacketMobDamage

abstract class MobLivingServer(type: EntityType<*, *>,
                               world: WorldServer,
                               pos: Vector3d,
                               speed: Vector3d,
                               aabb: AABB,
                               protected var health: Double,
                               protected var maxHealth: Double,
                               protected val viewField: Frustum,
                               protected val hitField: Frustum) : MobServer(
        type, world, pos, speed, aabb) {
    protected val noticeListeners: MutableMap<String, (MobServer) -> Unit> = ConcurrentHashMap()
    protected val jumpListeners: MutableMap<String, () -> Unit> = ConcurrentHashMap()
    protected val healListeners: MutableMap<String, (Double) -> Unit> = ConcurrentHashMap()
    protected val damageListeners: MutableMap<String, (Double) -> Unit> = ConcurrentHashMap()
    protected val deathListeners: MutableMap<String, () -> Unit> = ConcurrentHashMap()
    protected val ai: AI
    protected var lastDamage = 0.0
    protected var invincibleTicks = 0.0

    init {
        ai = createAI()
    }

    protected fun createAI(): AI {
        return SimpleAI(this)
    }

    @Synchronized fun attack(damage: Double): List<MobLivingServer> {
        val rotX = rot.doubleX().toRad()
        val rotZ = rot.doubleZ().toRad()
        val factor = cos(rotX) * 6.0
        val lookX = cos(rotZ) * factor
        val lookY = sin(rotZ) * factor
        val lookZ = sin(rotX) * 6.0
        val viewOffset = viewOffset()
        hitField.setView(pos.doubleX() + viewOffset.x,
                pos.doubleY() + viewOffset.y,
                pos.doubleZ() + viewOffset.z, pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0.0, 0.0, 1.0)
        val mobs = ArrayList<MobLivingServer>()
        world.getEntities(
                hitField).filterMap<MobLivingServer>().filter { it != this }.forEach { mob ->
            mobs.add(mob)
            mob.damage(damage)
            mob.onNotice(this)
            val rad = rot.doubleZ().toRad()
            mob.push(cos(rad) * 10.0,
                    sin(rad) * 10.0, 2.0)
        }
        return mobs
    }

    fun block(distance: Double,
              direction: Vector2d): PointerPane? {
        return world.terrain.selectBlock(pos.now() + viewOffset(), distance,
                rot.now().xz + direction)
    }

    abstract fun canMoveHere(terrain: TerrainServer,
                             x: Int,
                             y: Int,
                             z: Int): Boolean

    abstract fun creatureType(): CreatureType

    fun damage(damage: Double,
               ignoreInvincible: Boolean = false) {
        var d = damage
        if (invincibleTicks > 0.0 && !ignoreInvincible) {
            if (damage > lastDamage) {
                d = damage - lastDamage
                lastDamage = damage
            } else {
                d = 0.0
            }
        } else {
            lastDamage = damage
            invincibleTicks = 0.8
        }
        health -= d
        if (health > maxHealth) {
            health = maxHealth
        }
        if (health < 0) {
            health = 0.0
        }
        if (d != 0.0) {
            onDamage(damage)
            world.send(PacketMobDamage(registry, this))
        }
    }

    fun health(): Double {
        return health
    }

    fun maxHealth(): Double {
        return maxHealth
    }

    fun heal(heal: Double) {
        health += heal
        if (health > maxHealth) {
            health = maxHealth
        }
        if (health < 0) {
            health = 0.0
        }
        onHeal(heal)
        world.send(PacketMobDamage(registry, this))
    }

    val isDead: Boolean
        get() = health <= 0

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Health"] = health
        map["MaxHealth"] = maxHealth
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Health"]?.toDouble()?.let { health = it }
        map["MaxHealth"]?.toDouble()?.let { maxHealth = it }
    }

    override fun move(delta: Double) {
        super.move(delta)
        val lookX = cosTable(rot.doubleZ().toRad()) *
                cosTable(rot.doubleX().toRad()) * 6.0
        val lookY = sinTable(rot.doubleZ().toRad()) *
                cosTable(rot.doubleX().toRad()) * 6.0
        val lookZ = sinTable(rot.doubleX().toRad()) * 6
        val viewOffset = viewOffset()
        viewField.setView(pos.doubleX() + viewOffset.x,
                pos.doubleY() + viewOffset.y,
                pos.doubleZ() + viewOffset.z, pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0.0, 0.0, 1.0)
        world.getEntities(
                viewField).filterMap<MobServer>().filter { it != this }.forEach { mob ->
            val otherPos = mob.getCurrentPos()
            if (!world.checkBlocked(pos.intX(), pos.intY(), pos.intZ(),
                    otherPos.intX(), otherPos.intY(), otherPos.intZ())) {
                onNotice(mob)
            }
        }
        if (invincibleTicks >= 0.0) {
            invincibleTicks = max(invincibleTicks - delta, 0.0)
        }
    }

    abstract fun viewOffset(): Vector3d

    fun onNotice(id: String,
                 listener: (MobServer) -> Unit) {
        noticeListeners[id] = listener
    }

    fun onNotice(mob: MobServer) {
        noticeListeners.values.forEach { it(mob) }
    }

    fun onJump(id: String,
               listener: () -> Unit) {
        jumpListeners[id] = listener
    }

    fun onJump() {
        jumpListeners.values.forEach { it() }
    }

    fun onHeal(id: String,
               listener: (Double) -> Unit) {
        healListeners[id] = listener
    }

    fun onHeal(amount: Double) {
        healListeners.values.forEach { it(amount) }
    }

    fun onDamage(id: String,
                 listener: (Double) -> Unit) {
        damageListeners[id] = listener
    }

    fun onDamage(amount: Double) {
        damageListeners.values.forEach { it(amount) }
    }

    fun onDeath(id: String,
                listener: () -> Unit) {
        deathListeners[id] = listener
    }

    fun onDeath() {
        deathListeners.values.forEach { it() }
    }
}
