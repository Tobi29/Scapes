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

package org.tobi29.scapes.vanilla.basics.entity.client

import org.tobi29.scapes.Debug
import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.block.InventoryContainer
import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.Playlist
import org.tobi29.scapes.client.gui.GuiChatWrite
import org.tobi29.scapes.client.gui.GuiPause
import org.tobi29.scapes.client.input.InputMode
import org.tobi29.scapes.client.input.keyboard.InputModeKeyboard
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.engine.utils.ListenerOwner
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.direction
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.map
import org.tobi29.scapes.engine.utils.tag.toBoolean
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.client.EntityContainerClient
import org.tobi29.scapes.entity.client.MobClient
import org.tobi29.scapes.entity.client.MobLivingClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.model.MobLivingModelHuman
import org.tobi29.scapes.entity.model.RotationSmoothing
import org.tobi29.scapes.packets.PacketInteraction
import org.tobi29.scapes.packets.PacketItemUse
import org.tobi29.scapes.packets.PacketPlayerJump
import org.tobi29.scapes.vanilla.basics.gui.GuiPlayerInventory

class MobPlayerClientMainVB(type: EntityType<*, *>,
                            world: WorldClient) : MobPlayerClientMain(
        type, world, Vector3d.ZERO, Vector3d.ZERO,
        AABB(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 100.0, 100.0,
        Frustum(90.0, 1.0, 0.1, 24.0), ""), EntityContainerClient {
    private val inventories = InventoryContainer().apply {
        add("Container", Inventory(world.plugins, 40))
        add("Hold", Inventory(world.plugins, 1))
    }
    private var punchLeft: Long = -1
    private var punchRight: Long = -1
    private var chargeLeft = 0.0f
    private var chargeRight = 0.0f

    internal fun setHotbarSelectLeft(value: Int) {
        inventorySelectLeft = value
        world.send(PacketInteraction(registry,
                PacketInteraction.INVENTORY_SLOT_LEFT, value.toByte()))
    }

    internal fun setHotbarSelectRight(value: Int) {
        inventorySelectRight = value
        world.send(PacketInteraction(registry,
                PacketInteraction.INVENTORY_SLOT_RIGHT, value.toByte()))
    }

    override fun update(delta: Double) {
        super<MobPlayerClientMain>.update(delta)
        val conditionTag = metaData("Vanilla").map("Condition")
        if (conditionTag?.get("Sleeping")?.toBoolean() ?: false) {
            return
        }
        if (!hasGui()) {
            // Debug
            if (Debug.enabled()) {
                (input as? InputModeKeyboard)?.controller?.let { controller ->
                    if (controller.isPressed(ControllerKey.KEY_F5)) {
                        flying = !flying
                    }
                    if (flying) {
                        if (controller.isDown(ControllerKey.KEY_Q)) {
                            speed.plusZ(60.0 * delta)
                        }
                        if (controller.isDown(ControllerKey.KEY_C)) {
                            speed.plusZ(-60.0 * delta)
                        }
                    }
                }
            }
            // Movement
            if (input.jump()) {
                if (isSwimming) {
                    speed.plusZ(50.0 * delta)
                } else if (isOnGround) {
                    speed.setZ(5.1)
                    physicsState.isOnGround = false
                    connection().send(PacketPlayerJump(registry))
                }
            }
            val walk = input.walk()
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
            if (input.left() && wieldMode() != WieldMode.RIGHT) {
                if (chargeLeft < 0.01f) {
                    if (punchLeft == -1L) {
                        punchLeft = System.currentTimeMillis()
                    }
                }
            } else if (punchLeft != -1L) {
                updatePosition()
                val direction = input.hitDirection()
                breakParticles(world.terrain, 16, direction)
                world.send(PacketItemUse(registry, min(
                        (System.currentTimeMillis() - punchLeft).toDouble() / leftWeapon().material().hitWait(
                                leftWeapon()),
                        0.5) * 2.0, true, direction))
                punchLeft = -1
            }
            if (input.right() && wieldMode() != WieldMode.LEFT) {
                if (chargeRight < 0.01f) {
                    if (punchRight == -1L) {
                        punchRight = System.currentTimeMillis()
                    }
                }
            } else if (punchRight != -1L) {
                updatePosition()
                val direction = input.hitDirection()
                breakParticles(world.terrain, 16, direction)
                world.send(PacketItemUse(registry, min(
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
                game.playlist.setMusic(Playlist.Music.BATTLE)
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
                false, true, {
            if (input.requiresCameraSmoothing) RotationSmoothing.TIGHT
            else RotationSmoothing.DISABLE
        })
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toMap()?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag[id]?.toMap()?.let {
                    inventory.read(it)
                }
            }
        }
    }

    override fun inputMode(input: InputMode,
                           listenerOwner: ListenerOwner) {
        input.events.listener<InputDirectionEvent>(listenerOwner) { event ->
            if (!hasGui()) {
                rot.setZ((rot.doubleZ() - event.direction.x) % 360)
                rot.setX(min(89.0,
                        max(-89.0, rot.doubleX() - event.direction.y)))
            }
        }
        input.events.listener<HotbarChangeLeftEvent>(listenerOwner) { event ->
            setHotbarSelectLeft((inventorySelectLeft + event.delta) remP 10)
        }
        input.events.listener<HotbarChangeRightEvent>(listenerOwner) { event ->
            setHotbarSelectRight((inventorySelectRight + event.delta) remP 10)
        }
        input.events.listener<HotbarSetLeftEvent>(listenerOwner) { event ->
            setHotbarSelectLeft(event.value remP 10)
        }
        input.events.listener<HotbarSetRightEvent>(listenerOwner) { event ->
            setHotbarSelectRight(event.value remP 10)
        }
        input.events.listener<MenuOpenEvent>(listenerOwner) { event ->
            if (currentGui() !is GuiChatWrite) {
                event.success = true
                if (!closeGui()) {
                    openGui(GuiPause(game, this, game.engine.guiStyle))
                }
            }
        }
        input.events.listener<MenuInventoryEvent>(listenerOwner) { event ->
            if (currentGui() !is GuiChatWrite) {
                event.success = true
                if (!closeGui()) {
                    world.send(
                            PacketInteraction(registry,
                                    PacketInteraction.OPEN_INVENTORY, 0))
                }
            }
        }
        input.events.listener<MenuChatEvent>(listenerOwner) { event ->
            if (currentGui() !is GuiChatWrite) {
                event.success = true
                if (!hasGui()) {
                    openGui(GuiChatWrite(game, this, game.engine.guiStyle))
                }
            }
        }
    }
}
