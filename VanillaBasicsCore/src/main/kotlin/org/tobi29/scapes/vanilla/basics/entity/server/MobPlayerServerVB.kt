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

import org.tobi29.checksums.Checksum
import org.tobi29.math.*
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.plus
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.packets.PacketEntityChange
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.scapes.vanilla.basics.util.dropItem
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.toRad

class MobPlayerServerVB(
        type: EntityType<*, *>,
        world: WorldServer,
        nickname: String,
        skin: Checksum,
        connection: PlayerConnection
) : MobPlayerServer(type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 100.0, 100.0,
        Frustum(90.0, 1.0, 0.1, 24.0), Frustum(50.0, 1.0, 0.1, 2.0), nickname,
        skin, connection) {
    init {
        registerComponent(
                ComponentMobLivingServerCondition.COMPONENT,
                ComponentMobLivingServerCondition(this))
        onDeath[PLAYER_LISTENER_TOKEN] = {
            inventories.modify("Container") { inventory ->
                val items = ArrayList<Item>(inventory.size())
                for (i in 0 until inventory.size()) {
                    inventory[i]?.let { items.add(it) }
                    inventory[i] = null
                }
                items
            }.forEach { world.dropItem(it, this.pos.now()) }
            inventories.modify("Hold") { inventory ->
                val items = ArrayList<Item>(inventory.size())
                for (i in 0 until inventory.size()) {
                    inventory[i]?.let { items.add(it) }
                    inventory[i] = null
                }
                items
            }.forEach { world.dropItem(it, this.pos.now()) }
            setSpeed(Vector3d.ZERO)
            setPos(Vector3d(world.spawn) + Vector3d(0.5, 0.5, 1.5))
            health = maxHealth
            world.send(PacketEntityChange(registry, this))
            spawn()
        }
    }

    override fun wieldMode(): WieldMode {
        return if (inventorySelectLeft == inventorySelectRight)
            WieldMode.RIGHT
        else
            WieldMode.DUAL
    }

    override fun update(delta: Double) {
        val lookX = cosTable(rot.z.toRad()) *
                cosTable(rot.x.toRad()) * 6.0
        val lookY = sinTable(rot.z.toRad()) *
                cosTable(rot.x.toRad()) * 6.0
        val lookZ = sinTable(rot.x.toRad()) * 6.0
        val viewOffset = viewOffset()
        viewField.setView(pos.x + viewOffset.x,
                pos.y + viewOffset.y,
                pos.z + viewOffset.z, pos.x + lookX,
                pos.y + lookY, pos.z + lookZ, 0.0, 0.0, 1.0)
        world.getEntities(
                viewField).filterIsInstance<MobServer>().filter { it != this }.forEach { mob ->
            val mobPos = mob.getCurrentPos()
            if (!world.checkBlocked(pos.x.floorToInt(), pos.y.floorToInt(),
                            pos.z.floorToInt(), mobPos.x.floorToInt(),
                            mobPos.y.floorToInt(), mobPos.z.floorToInt())) {
                notice(mob)
            }
        }
        if (pos.z < -100.0) {
            damage(-pos.z - 100.0)
        }
        if (health < 10.0) {
            val random = threadLocalRandom()
            if (random.nextInt(40) == 0) {
                push(random.nextDouble() * 2.0 - 1.0, 0.0, 0.0)
            }
            if (random.nextInt(40) == 0) {
                push(0.0, random.nextDouble() * 2.0 - 1.0, 0.0)
            }
            if (random.nextInt(20) == 0) {
                setRot(Vector3d(
                        rot.x + random.nextDouble() * 60.0 - 30.0,
                        rot.y, rot.z))
            }
            if (random.nextInt(20) == 0) {
                setRot(Vector3d(rot.x, rot.y,
                        rot.z + random.nextDouble() * 60.0 - 30.0))
            }
        }
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.63)
    }

    override fun isActive(): Boolean {
        return getOrNull(
                ComponentMobLivingServerCondition.COMPONENT)?.sleeping != true
    }

    override fun onCloseInventory(): Boolean {
        inventories.modify("Hold") { inventory ->
            val items = ArrayList<Item>(inventory.size())
            for (i in 0 until inventory.size()) {
                inventory[i]?.let { items.add(it) }
                inventory[i] = null
            }
            items
        }.forEach { world.dropItem(it, this.pos.now()) }
        return true
    }
}

private val PLAYER_LISTENER_TOKEN = ListenerToken("VanillaBasics:Player")
