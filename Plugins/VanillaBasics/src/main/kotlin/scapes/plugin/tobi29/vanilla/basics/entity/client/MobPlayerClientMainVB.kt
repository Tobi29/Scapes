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

package scapes.plugin.tobi29.vanilla.basics.entity.client

import org.tobi29.scapes.Debug
import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.Playlist
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.direction
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.client.EntityContainerClient
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.entity.client.MobLivingClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.model.MobLivingModelHuman
import org.tobi29.scapes.packets.PacketInteraction
import org.tobi29.scapes.packets.PacketItemUse
import org.tobi29.scapes.packets.PacketPlayerJump
import scapes.plugin.tobi29.vanilla.basics.gui.GuiPlayerInventory

class MobPlayerClientMainVB(world: WorldClient, pos: Vector3d, speed: Vector3d,
                            xRot: Double, zRot: Double, nickname: String) : MobPlayerClientMain(
        world, pos, speed, AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 100.0, 100.0,
        Frustum(90.0, 1.0, 0.1, 24.0), nickname), EntityContainerClient {
    private val inventories: InventoryContainer
    private var punchLeft: Long = -1
    private var punchRight: Long = -1
    private var chargeLeft = 0.0f
    private var chargeRight = 0.0f

    init {
        inventories = InventoryContainer()
        inventories.add("Container", Inventory(registry, 40))
        inventories.add("Hold", Inventory(registry, 1))
        rot.setX(xRot)
        rot.setZ(zRot)
    }

    override fun update(delta: Double) {
        super.update(delta)
        val conditionTag = metaData("Vanilla").structure("Condition")
        if (conditionTag.getBoolean("Sleeping") ?: false) {
            return
        }
        if (!hasGui()) {
            // Inventory
            var previous = inventorySelectLeft
            var hotbar = controller.hotbarLeft(previous)
            if (hotbar != previous) {
                inventorySelectLeft = hotbar
                world.send(PacketInteraction(
                        PacketInteraction.INVENTORY_SLOT_LEFT,
                        inventorySelectLeft.toByte()))
            }
            previous = inventorySelectRight
            hotbar = controller.hotbarRight(previous)
            if (hotbar != previous) {
                inventorySelectRight = hotbar
                world.send(PacketInteraction(
                        PacketInteraction.INVENTORY_SLOT_RIGHT,
                        inventorySelectRight.toByte()))
            }
            // Debug
            if (Debug.enabled()) {
                game.engine.controller?.let { controllerDefault ->
                    if (controllerDefault.isPressed(ControllerKey.KEY_F5)) {
                        flying = !flying
                    }
                    if (flying) {
                        if (controllerDefault.isDown(ControllerKey.KEY_Q)) {
                            speed.plusZ(60.0 * delta)
                        }
                        if (controllerDefault.isDown(ControllerKey.KEY_C)) {
                            speed.plusZ(-60.0 * delta)
                        }
                    }
                }
            }
            // Movement
            if (controller.jump()) {
                if (isSwimming) {
                    speed.plusZ(50.0 * delta)
                } else if (isOnGround) {
                    speed.setZ(5.1)
                    physicsState.isOnGround = false
                    connection().send(PacketPlayerJump())
                }
            }
            val camera = controller.camera(delta)
            rot.setZ((rot.doubleZ() - camera.x) % 360)
            rot.setX(min(89.0,
                    max(-89.0, rot.doubleX() - camera.y)))
            val walk = controller.walk()
            var walkSpeed = clamp(
                    max(abs(walk.x),
                            abs(walk.y)), 0.0, 1.0) * 120.0
            if (!isOnGround && !physicsState.slidingWall && !isInWater && !flying) {
                walkSpeed *= 0.0006
            } else if (!isOnGround && !isInWater && !flying) {
                walkSpeed *= 0.05
            } else if (isInWater) {
                walkSpeed *= 0.2
            }
            if (walkSpeed > 0.0) {
                val dir = (walk.direction().toDeg() + rot.doubleZ() - 90.0).toRad()
                walkSpeed *= delta
                speed.plusX(cos(dir) * walkSpeed)
                speed.plusY(sin(dir) * walkSpeed)
            }
            // Placement
            if (controller.left() && wieldMode() != WieldMode.RIGHT) {
                if (chargeLeft < 0.01f) {
                    if (punchLeft == -1L) {
                        punchLeft = System.currentTimeMillis()
                    }
                }
            } else if (punchLeft != -1L) {
                updatePosition()
                val direction = controller.hitDirection()
                breakParticles(world.terrain, 16, direction)
                world.send(PacketItemUse(min(
                        (System.currentTimeMillis() - punchLeft).toDouble() / leftWeapon().material().hitWait(
                                leftWeapon()),
                        0.5) * 2.0, true, direction))
                punchLeft = -1
            }
            if (controller.right() && wieldMode() != WieldMode.LEFT) {
                if (chargeRight < 0.01f) {
                    if (punchRight == -1L) {
                        punchRight = System.currentTimeMillis()
                    }
                }
            } else if (punchRight != -1L) {
                updatePosition()
                val direction = controller.hitDirection()
                breakParticles(world.terrain, 16, direction)
                world.send(PacketItemUse(min(
                        (System.currentTimeMillis() - punchRight).toDouble() / rightWeapon().material().hitWait(
                                rightWeapon()),
                        0.5) * 2.0, false, direction))
                punchRight = -1
            }
            val time = System.currentTimeMillis()
            var swingTime = leftWeapon().material().hitWait(
                    leftWeapon()).toFloat()
            var punch: Long
            if (punchLeft == -1L) {
                punch = 0
            } else {
                punch = time - punchLeft
            }
            if (punch > 0) {
                chargeLeft = min(punch / swingTime, 0.5f)
            } else {
                if (chargeLeft > 0.0f) {
                    if (chargeLeft >= 0.45f) {
                        chargeLeft = 0.0f
                    } else {
                        chargeLeft = 0.0f
                    }
                }
            }
            swingTime = rightWeapon().material().hitWait(
                    rightWeapon()).toFloat()
            if (punchRight == -1L) {
                punch = 0
            } else {
                punch = time - punchRight
            }
            if (punch > 0) {
                chargeRight = min(punch / swingTime, 0.5f)
            } else {
                if (chargeRight > 0.0f) {
                    if (chargeRight >= 0.45f) {
                        chargeRight = 0.0f
                    } else {
                        chargeRight = 0.0f
                    }
                }
            }
        }
    }

    override fun onNotice(notice: MobClient) {
        if (notice is MobLivingClient) {
            if (notice.creatureType() == CreatureType.MONSTER) {
                game.playlist().setMusic(Playlist.Music.BATTLE)
            }
        }
    }

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.63)
    }

    override fun leftWeapon(): ItemStack {
        return inventories.access("Container"
        ) { inventory -> inventory.item(inventorySelectLeft) }
    }

    override fun rightWeapon(): ItemStack {
        return inventories.access("Container"
        ) { inventory -> inventory.item(inventorySelectRight) }
    }

    override fun wieldMode(): WieldMode {
        return if (inventorySelectLeft == inventorySelectRight)
            WieldMode.RIGHT
        else
            WieldMode.DUAL
    }

    override fun leftCharge(): Float {
        return chargeLeft
    }

    override fun rightCharge(): Float {
        return chargeRight
    }

    override fun gui(player: MobPlayerClientMain): Gui? {
        if (player is MobPlayerClientMainVB) {
            return GuiPlayerInventory(player, player.game.engine.guiStyle)
        }
        return null
    }

    override fun inventories(): InventoryContainer {
        return inventories
    }

    override fun createModel(): MobLivingModelHuman? {
        val texture = world.scene.skinStorage()[skin]
        return MobLivingModelHuman(world.game.modelHumanShared(), this, texture,
                false, true, true)
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getStructure("Inventory")?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag.getStructure(id)?.let { inventory.load(it) }
            }
        }
    }
}
