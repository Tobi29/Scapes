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

import org.tobi29.checksums.Checksum
import org.tobi29.io.tag.*
import org.tobi29.math.AABB
import org.tobi29.math.Frustum
import org.tobi29.math.PointerPane
import org.tobi29.math.vector.Vector2d
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.block.*
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.entity.*
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.packets.PacketEntityChange
import org.tobi29.scapes.packets.PacketOpenGui
import org.tobi29.scapes.packets.PacketUpdateInventory
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.ConcurrentMap
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.toRad
import kotlin.collections.set
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

abstract class MobPlayerServer(
        type: EntityType<*, *>,
        world: WorldServer,
        pos: Vector3d,
        speed: Vector3d,
        aabb: AABB,
        lives: Double,
        maxLives: Double,
        viewField: Frustum,
        hitField: Frustum,
        protected val nickname: String,
        private val skin: Checksum,
        protected val connection: PlayerConnection
) : MobLivingEquippedServer(type, world, pos, speed, aabb, lives, maxLives,
        viewField, hitField) {
    protected val positionSenderOther: MobPositionSenderServer
    val onPunch: ConcurrentMap<ListenerToken, (Double) -> Unit> = ConcurrentHashMap()
    val positionReceiver: MobPositionReceiver
    var inventorySelectLeft = 0
    var inventorySelectRight = 9
    protected var healWait = 0
    protected var currentContainer: EntityServer? = null

    init {
        registerComponent(CreatureType.COMPONENT, CreatureType.CREATURE)
        this[InventoryContainer.ON_UPDATE][CONTAINER_UPDATE_LISTENER_TOKEN] = { id ->
            world.send(PacketUpdateInventory(registry, this, id))
        }
        inventories.add("Container", 40)
        inventories.add("Hold", 1)
        val exceptions = listOf(connection)
        positionSenderOther = MobPositionSenderServer(registry, pos,
                { world.send(it, exceptions) })
        positionReceiver = MobPositionReceiver(pos,
                { this.pos.set(it) },
                { this.speed.set(it) },
                { this.rot.set(it) },
                { ground, slidingWall, inWater, swimming ->
                    if (ground != this.isOnGround) {
                        physicsState.isOnGround = ground
                        if (speed.z > 0.0 && !inWater) {
                            jump()
                        }
                    }
                    physicsState.slidingWall = slidingWall
                    physicsState.isInWater = inWater
                    physicsState.isSwimming = swimming
                })
    }

    abstract fun isActive(): Boolean

    override fun leftWeapon(): Item? {
        return inventories.access("Container") { it[inventorySelectLeft] }
    }

    override fun rightWeapon(): Item? {
        return inventories.access("Container") { it[inventorySelectRight] }
    }

    fun nickname(): String {
        return nickname
    }

    fun selectedBlock(direction: Vector2d): PointerPane? {
        return block(6.0, direction)
    }

    override fun createPositionHandler(): MobPositionSenderServer {
        return MobPositionSenderServer(registry, pos.now(),
                { connection.send(it) })
    }

    fun connection(): PlayerConnection {
        return connection
    }

    fun attackLeft(strength: Double,
                   direction: Vector2d): List<MobLivingServer> {
        return attack(true, strength, direction)
    }

    fun attackRight(strength: Double,
                    direction: Vector2d): List<MobLivingServer> {
        return attack(false, strength, direction)
    }

    @Synchronized
    protected fun attack(side: Boolean,
                         strength: Double,
                         direction: Vector2d): List<MobLivingServer> {
        val rotX = rot.x + direction.y
        val rotZ = rot.z + direction.x
        val factor = cos(rotX.toRad()) * 6
        val lookX = cos(rotZ.toRad()) * factor
        val lookY = sin(rotZ.toRad()) * factor
        val lookZ = sin(rotX.toRad()) * 6
        val viewOffset = viewOffset()
        hitField.setView(pos.x + viewOffset.x,
                pos.y + viewOffset.y,
                pos.z + viewOffset.z, pos.x + lookX,
                pos.y + lookY, pos.z + lookZ, 0.0, 0.0, 1.0)
        val mobs = ArrayList<MobLivingServer>()
        val weaponSlot = if (side) inventorySelectLeft else inventorySelectRight
        inventories.modify("Container") { inventory ->
            var weapon = inventory[weaponSlot]?.kind<ItemTypeWeapon>()
                    ?: return@modify
            val range = weapon.hitRange()
            hitField.setPerspective(100 / range, 1.0, 0.1, range)
            world.getEntities(hitField).filterIsInstance<MobLivingServer>()
                    .filter { it != this }.forEach { mob ->
                mobs.add(mob)
                val (stack, damage) = weapon.click(this, mob)
                inventory[weaponSlot] = stack
                weapon = stack?.kind() ?: return@modify
                mob.damage(damage ?: 2.0)
                mob.notice(this)
                val rad = rot.z.toRad()
                mob.push(cos(rad) * 10.0,
                        sin(rad) * 10.0, 2.0)
            }
        }

        return mobs
    }

    @Synchronized
    fun hasGui(): Boolean {
        return currentContainer != null
    }

    @Synchronized
    fun openGui(gui: EntityServer) {
        if (hasGui()) {
            closeGui()
        }
        currentContainer = gui
        world.send(PacketEntityChange(registry, gui))
        gui.viewers.add(this)
        connection.send(PacketOpenGui(registry, gui))
    }

    @Synchronized
    fun closeGui() {
        currentContainer?.let {
            it.viewers.remove(this)
            currentContainer = null
        }
    }

    open fun onOpenInventory() = true

    open fun onCloseInventory() = true

    override fun canMoveHere(terrain: TerrainServer,
                             x: Int,
                             y: Int,
                             z: Int): Boolean {
        return false
    }

    override fun write(map: ReadWriteTagMap) {
        write(map, true)
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["HealWait"]?.toInt()?.let { healWait = it }
        map["Inventory"]?.toMap()?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag[id]?.toMap()?.let {
                    inventory.read(world.plugins, it)
                }
            }
        }
    }

    override fun move(delta: Double) {
        val aabb = getAABB()
        val aabbs = AABBS.get()
        // stepHeight is 0.0 as we will not actually move around here, just
        // blocks that we are inside of
        EntityPhysics.collisions(delta, speed, world.terrain, aabb, 0.0, aabbs)
        EntityPhysics.collide(delta, aabb, aabbs, physicsState) {
            it.collision.inside(this, delta)
        }
        positionSenderOther.submitUpdate(uuid, pos.now(), speed.now(),
                rot.now(), physicsState.isOnGround, physicsState.slidingWall,
                physicsState.isInWater, physicsState.isSwimming)
        isHeadInWater = world.terrain.type(pos.x.floorToInt(),
                pos.y.floorToInt(),
                (pos.z + 0.7).floorToInt()).isLiquid
        if (invincibleTicks > 0.0) {
            invincibleTicks = max(invincibleTicks - delta, 0.0)
        }
        aabbs.reset()
    }

    fun write(map: ReadWriteTagMap,
              packet: Boolean) {
        super.write(map)
        map["HealWait"] = healWait.toTag()
        map["Inventory"] = TagMap {
            inventories.forEach { id, inventory ->
                this[id] = TagMap { inventory.write(this) }
            }
        }
        if (packet) {
            map["Nickname"] = nickname.toTag()
            map["SkinChecksum"] = skin.array().toTag()
        }
    }

    fun punch(strength: Double) {
        onPunch.values.forEach { it(strength) }
    }
}

private val CONTAINER_UPDATE_LISTENER_TOKEN = ListenerToken(
        "Core:ContainerUpdate")
