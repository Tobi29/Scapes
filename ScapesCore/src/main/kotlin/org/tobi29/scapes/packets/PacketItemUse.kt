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
package org.tobi29.scapes.packets

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.tobi29.io.ReadableByteStream
import org.tobi29.io.WritableByteStream
import org.tobi29.math.vector.Vector2d
import org.tobi29.scapes.block.*
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.server.InvalidPacketDataException
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class PacketItemUse : PacketAbstract,
        PacketServer {
    private var strength = 0.0
    private var side = false
    private lateinit var direction: Vector2d

    constructor(type: PacketType) : super(type)

    constructor(type: PacketType,
                strength: Double,
                side: Boolean,
                direction: Vector2d) : super(type) {
        this.strength = strength
        this.side = side
        this.direction = direction
    }

    constructor(registry: Registries,
                strength: Double,
                side: Boolean,
                direction: Vector2d) : this(
            Packet.make(registry, "core.packet.ItemUse"),
            strength, side, direction)

    // TODO: @Throws(IOException::class)
    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putDouble(strength)
        stream.putBoolean(side)
        stream.putDouble(direction.x)
        stream.putDouble(direction.y)
    }

    // TODO: @Throws(IOException::class)
    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        strength = stream.getDouble()
        side = stream.getBoolean()
        direction = Vector2d(stream.getDouble(), stream.getDouble())
    }

    override fun runServer(player: PlayerConnection) {
        if (strength > 1.0 || strength < 0.0) {
            throw InvalidPacketDataException("Invalid item use strength!")
        }
        if (abs(direction.x) > 90.0 || abs(
                direction.y) > 180.0) {
            throw InvalidPacketDataException("Invalid direction!")
        }
        player.mob { mob ->
            val world = mob.world
            val terrain = world.terrain
            if (side) {
                mob.attackLeft(strength * strength, direction)
            } else {
                mob.attackRight(strength * strength, direction)
            }
            val heldSlot = if (side) mob.inventorySelectLeft else mob.inventorySelectRight
            mob.inventories.modify("Container") { inventory ->
                inventory[heldSlot] = inventory[heldSlot].click(mob)
            }
            mob.punch(strength)
            val pane = mob.selectedBlock(direction)
            if (pane != null) {
                val x = pane.x
                val y = pane.y
                val z = pane.z
                val face = pane.face
                var flag = false
                if (strength < 0.9) {
                    flag = terrain.type(x, y, z).click(terrain, x, y, z, face,
                            mob)
                }
                if (!flag && strength > 0.0) {
                    launch(world + CoroutineName("Block-Break")) {
                        val weapon = mob.inventories.access(
                                "Container") { inventory ->
                            inventory[heldSlot]
                        }.kind<ItemTypeWeapon>()
                        delay(((weapon?.hitWait() ?: 500) * 0.05).toLong(),
                                TimeUnit.MILLISECONDS)
                        mob.inventories.modify("Container") { inventory ->
                            val item = inventory[heldSlot]
                            if (weapon == null || weapon.type == item?.type) {
                                val (stack, br) = item.click(mob, terrain,
                                        x, y, z, face)
                                inventory[heldSlot] = stack
                                if (br == null || br > 0.0) {
                                    val block = terrain.block(x, y, z)
                                    val type = terrain.type(block)
                                    val data = terrain.data(block)
                                    type.punch(terrain, x, y, z, data, face,
                                            mob, stack, br ?: 0.1, strength)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
