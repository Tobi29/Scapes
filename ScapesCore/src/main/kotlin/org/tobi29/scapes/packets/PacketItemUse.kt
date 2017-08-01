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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.server.InvalidPacketDataException
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.server.connection.PlayerConnection

class PacketItemUse : PacketAbstract, PacketServer {
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
            mob.inventories().modify("Container") {
                val world = mob.world
                val terrain = world.terrain
                val item: ItemStack
                if (side) {
                    mob.attackLeft(strength * strength, direction)
                    item = mob.leftWeapon()
                } else {
                    mob.attackRight(strength * strength, direction)
                    item = mob.rightWeapon()
                }
                item.material().click(mob, item)
                mob.onPunch(strength)
                val pane = mob.selectedBlock(direction)
                if (pane != null) {
                    val x = pane.x
                    val y = pane.y
                    val z = pane.z
                    val face = pane.face
                    var flag = false
                    if (strength < 0.6) {
                        flag = terrain.type(x, y, z).click(terrain, x, y, z,
                                face, mob)
                    }
                    if (!flag && strength > 0.0) {
                        world.loop.addTaskOnce({
                            mob.inventories().modify("Container") {
                                val br = item.material().click(mob, item,
                                        terrain, x, y, z, face)
                                if (br > 0.0) {
                                    val block = terrain.block(x, y, z)
                                    val type = terrain.type(block)
                                    val data = terrain.data(block)
                                    type.punch(terrain, x, y, z, data, face,
                                            mob, item, br, strength)
                                }
                            }
                        }, "Block-Break",
                                (item.material().hitWait(item) * 0.05).toLong(),
                                false)
                    }
                }
            }
        }
    }
}
