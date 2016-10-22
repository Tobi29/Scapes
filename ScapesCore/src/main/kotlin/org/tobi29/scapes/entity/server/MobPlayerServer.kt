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
package org.tobi29.scapes.entity.server

import java8.util.stream.Stream
import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.Checksum
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getInt
import org.tobi29.scapes.engine.utils.io.tag.setInt
import org.tobi29.scapes.engine.utils.io.tag.setStructure
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.math.vector.plus
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.MobPositionReceiver
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.packets.PacketEntityChange
import org.tobi29.scapes.packets.PacketOpenGui
import org.tobi29.scapes.packets.PacketUpdateInventory
import org.tobi29.scapes.server.connection.PlayerConnection
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class MobPlayerServer(world: WorldServer, pos: Vector3d, speed: Vector3d,
                               aabb: AABB, lives: Double, maxLives: Double, viewField: Frustum,
                               hitField: Frustum, protected val nickname: String, private val skin: Checksum,
                               protected val connection: PlayerConnection) : MobLivingEquippedServer(
        world, pos, speed, aabb, lives, maxLives, viewField,
        hitField), EntityContainerServer {
    protected val positionSenderOther: MobPositionSenderServer
    protected val viewers: MutableList<MobPlayerServer> = ArrayList()
    protected val inventories: InventoryContainer
    private val punchListeners = ConcurrentHashMap<String, (Double) -> Unit>()
    val positionReceiver: MobPositionReceiver
    var inventorySelectLeft = 0
    var inventorySelectRight = 9
    protected var healWait = 0
    protected var currentContainer: EntityContainerServer? = null

    init {
        inventories = InventoryContainer { id ->
            world.send(PacketUpdateInventory(this, id))
        }
        inventories.add("Container", Inventory(registry, 40))
        inventories.add("Hold", Inventory(registry, 1))
        viewers.add(this)
        val exceptions = listOf(connection)
        positionSenderOther = MobPositionSenderServer(pos,
                { world.send(it, exceptions) })
        positionReceiver = MobPositionReceiver(pos,
                { this.pos.set(it) },
                { this.speed.set(it) },
                { this.rot.set(it) },
                { ground, slidingWall, inWater, swimming ->
                    if (ground != this.isOnGround) {
                        this.isOnGround = ground
                        if (speed.z > 0.0 && !inWater) {
                            onJump()
                        }
                    }
                    this.slidingWall = slidingWall
                    this.isInWater = inWater
                    this.isSwimming = swimming
                })
        onDeath("Local", {
            inventories.modify<List<ItemStack>>(
                    "Container") { inventory ->
                val items = ArrayList<ItemStack>()
                for (i in 0..inventory.size() - 1) {
                    inventory.item(i).take()?.let { items.add(it) }
                }
                items
            }.forEach { item -> world.dropItem(item, this.pos.now()) }
            inventories.modify("Hold") {
                it.item(0).take()
            }?.let { world.dropItem(it, this.pos.now()) }
            setSpeed(Vector3d.ZERO)
            setPos(Vector3d(world.spawn + Vector3i(0, 0, 1)))
            health = maxHealth
            world.send(PacketEntityChange(this))
            onSpawn()
        })
    }

    abstract fun isActive(): Boolean

    override fun leftWeapon(): ItemStack {
        return inventories.access("Container"
        ) { inventory -> inventory.item(inventorySelectLeft) }
    }

    override fun rightWeapon(): ItemStack {
        return inventories.access("Container"
        ) { inventory -> inventory.item(inventorySelectRight) }
    }

    fun nickname(): String {
        return nickname
    }

    fun selectedBlock(direction: Vector2d): PointerPane? {
        return block(6.0, direction)
    }

    override fun inventories(): InventoryContainer {
        return inventories
    }

    override fun addViewer(player: MobPlayerServer) {
        if (!viewers.contains(player)) {
            viewers.add(player)
        }
    }

    override fun viewers(): Stream<MobPlayerServer> {
        return viewers.stream()
    }

    override fun removeViewer(player: MobPlayerServer) {
        viewers.remove(player)
    }

    override fun createPositionHandler(): MobPositionSenderServer {
        return MobPositionSenderServer(pos.now(),
                { packet -> connection.send(packet) })
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

    @Synchronized protected fun attack(side: Boolean,
                                       strength: Double,
                                       direction: Vector2d): List<MobLivingServer> {
        val rotX = rot.doubleX() + direction.y
        val rotZ = rot.doubleZ() + direction.x
        val factor = cos(rotX.toRad()) * 6
        val lookX = cos(rotZ.toRad()) * factor
        val lookY = sin(rotZ.toRad()) * factor
        val lookZ = sin(rotX.toRad()) * 6
        val viewOffset = viewOffset()
        hitField.setView(pos.doubleX() + viewOffset.x,
                pos.doubleY() + viewOffset.y,
                pos.doubleZ() + viewOffset.z, pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0.0, 0.0, 1.0)
        val range: Double
        if (side) {
            range = leftWeapon().material().hitRange(leftWeapon())
        } else {
            range = rightWeapon().material().hitRange(rightWeapon())
        }
        hitField.setPerspective(100 / range, 1.0, 0.1, range)
        val mobs = ArrayList<MobLivingServer>()
        world.getEntities(hitField) {
            it.filterMap<MobLivingServer>().filter { it != this }.forEach { mob ->
                mobs.add(mob)
                if (side) {
                    mob.damage(leftWeapon().material().click(this, leftWeapon(),
                            mob) * strength)
                } else {
                    mob.damage(
                            rightWeapon().material().click(this, rightWeapon(),
                                    mob) * strength)
                }
                mob.onNotice(this)
                val rad = rot.doubleZ().toRad()
                mob.push(cos(rad) * 10.0,
                        sin(rad) * 10.0, 2.0)
            }
        }
        return mobs
    }

    fun hasGui(): Boolean {
        return currentContainer != null
    }

    fun openGui(gui: EntityContainerServer) {
        if (hasGui()) {
            closeGui()
        }
        currentContainer = gui
        world.send(PacketEntityChange(gui as EntityServer))
        gui.addViewer(this)
        connection.send(PacketOpenGui(gui))
    }

    fun closeGui() {
        if (currentContainer != null) {
            currentContainer!!.removeViewer(this)
            currentContainer = null
        }
    }

    override fun canMoveHere(terrain: TerrainServer,
                             x: Int,
                             y: Int,
                             z: Int): Boolean {
        return false
    }

    override fun creatureType(): CreatureType {
        return CreatureType.CREATURE
    }

    override fun write(): TagStructure {
        return write(true)
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getInt("HealWait")?.let { healWait = it }
        tagStructure.getStructure("Inventory")?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag.getStructure(id)?.let { inventory.load(it) }
            }
        }
    }

    override fun move(delta: Double) {
        val aabb = getAABB()
        val aabbs = world.terrain.collisions(floor(aabb.minX),
                floor(aabb.minY),
                floor(aabb.minZ),
                floor(aabb.maxX),
                floor(aabb.maxY),
                floor(aabb.maxZ))
        collide(aabb, aabbs, delta)
        positionSenderOther.submitUpdate(uuid, pos.now(), speed.now(),
                rot.now(), isOnGround,
                slidingWall, isInWater, isSwimming)
        isHeadInWater = world.terrain.type(pos.intX(), pos.intY(),
                floor(pos.doubleZ() + 0.7)).isLiquid
        if (invincibleTicks > 0.0) {
            invincibleTicks = max(invincibleTicks - delta, 0.0)
        }
    }

    fun write(packet: Boolean): TagStructure {
        val tagStructure = super.write()
        tagStructure.setInt("HealWait", healWait)
        tagStructure.setStructure("Inventory") {
            inventories.forEach { id, inventory ->
                setStructure(id, inventory.save())
            }
        }
        if (packet) {
            tagStructure.setString("Nickname", nickname)
            tagStructure.setByteArray("SkinChecksum", *skin.array())
        }
        return tagStructure
    }

    fun onPunch(id: String,
                listener: (Double) -> Unit) {
        punchListeners[id] = listener
    }

    fun onPunch(strength: Double) {
        punchListeners.values.forEach { it(strength) }
    }
}
