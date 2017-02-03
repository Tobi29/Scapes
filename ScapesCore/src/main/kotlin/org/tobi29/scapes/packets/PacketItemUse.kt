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
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.engine.server.InvalidPacketDataException
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import org.tobi29.scapes.engine.utils.io.WritableByteStream
import org.tobi29.scapes.engine.utils.math.abs
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.entity.server.EntityBlockBreakServer
import org.tobi29.scapes.server.connection.PlayerConnection
import java.io.IOException

class PacketItemUse : PacketAbstract, PacketServer {
    private var strength = 0.0
    private var side = false
    private lateinit var direction: Vector2d

    constructor()

    constructor(strength: Double,
                side: Boolean,
                direction: Vector2d) {
        this.strength = strength
        this.side = side
        this.direction = direction
    }

    @Throws(IOException::class)
    override fun sendServer(client: ClientConnection,
                            stream: WritableByteStream) {
        stream.putDouble(strength)
        stream.putBoolean(side)
        stream.putDouble(direction.x)
        stream.putDouble(direction.y)
    }

    @Throws(IOException::class)
    override fun parseServer(player: PlayerConnection,
                             stream: ReadableByteStream) {
        strength = stream.double
        side = stream.boolean
        direction = Vector2d(stream.double, stream.double)
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
            mob.inventories().modify("Container") { inventory ->
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
                    val blockPos = Vector3i(pane.x, pane.y, pane.z)
                    val face = pane.face
                    val br = item.material().click(mob, item, terrain,
                            blockPos.x,
                            blockPos.y,
                            blockPos.z, face)
                    var flag = false
                    if (strength < 0.6) {
                        flag = terrain.type(blockPos.x, blockPos.y,
                                blockPos.z).click(terrain, blockPos.x,
                                blockPos.y,
                                blockPos.z, face, mob)
                    }
                    if (!flag && br > 0.0 && strength > 0.0) {
                        world.taskExecutor.addTaskOnce({
                            terrain.queue { handler ->
                                val block = handler.block(blockPos.x,
                                        blockPos.y,
                                        blockPos.z)
                                val type = handler.type(block)
                                val data = handler.data(block)
                                val punch = br / type.resistance(item, data) *
                                        strength * strength
                                if (punch > 0) {
                                    type.breakSound(item, data)?.let {
                                        world.playSound(it,
                                                Vector3d(blockPos),
                                                Vector3d.ZERO)
                                    }
                                    val entityBreak = terrain.getEntities(
                                            blockPos.x,
                                            blockPos.y,
                                            blockPos.z)
                                            .filterMap<EntityBlockBreakServer>()
                                            .firstOrNull() ?: run {
                                        val entityBreak = EntityBlockBreakServer(
                                                world, Vector3d(blockPos))
                                        world.addEntityNew(entityBreak)
                                        entityBreak
                                    }
                                    if (entityBreak.punch(world, punch)) {
                                        if (type.destroy(handler, blockPos.x,
                                                blockPos.y, blockPos.z, data,
                                                face, mob, item)) {
                                            val drops = type.drops(item, data)
                                            world.dropItems(drops, blockPos.x,
                                                    blockPos.y, blockPos.z)
                                            handler.typeData(blockPos.x,
                                                    blockPos.y, blockPos.z,
                                                    handler.air, 0)
                                        }
                                    }
                                }
                            }
                        }, "Block-Break",
                                (item.material().hitWait(item) * 0.05).toLong())
                    }
                }
            }
        }
    }
}
