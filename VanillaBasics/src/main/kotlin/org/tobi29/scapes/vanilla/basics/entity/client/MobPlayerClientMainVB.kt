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

import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toMap
import org.tobi29.math.AABB3
import org.tobi29.math.Frustum
import org.tobi29.math.vector.*
import org.tobi29.scapes.Debug
import org.tobi29.scapes.block.ItemTypeWeapon
import org.tobi29.scapes.block.hitWait
import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.block.read
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.client.Playlist
import org.tobi29.scapes.client.gui.GuiChatWrite
import org.tobi29.scapes.client.gui.GuiPause
import org.tobi29.scapes.client.input.InputModeKeyboard
import org.tobi29.scapes.client.input.InputModeScapes
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.entity.CreatureType
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.WieldMode
import org.tobi29.scapes.entity.client.GUI_COMPONENT
import org.tobi29.scapes.entity.client.MobLivingClient
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.client.attachModel
import org.tobi29.scapes.entity.model.MobLivingModelHuman
import org.tobi29.scapes.entity.model.RotationSmoothing
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.packets.PacketInteraction
import org.tobi29.scapes.packets.PacketItemUse
import org.tobi29.scapes.packets.PacketPlayerJump
import org.tobi29.scapes.vanilla.basics.entity.server.ComponentMobLivingServerCondition
import org.tobi29.scapes.vanilla.basics.gui.GuiPlayerInventory
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.remP
import org.tobi29.stdex.math.toDeg
import org.tobi29.stdex.math.toRad
import org.tobi29.utils.ListenerRegistrar
import kotlin.collections.set
import kotlin.math.*

class MobPlayerClientMainVB(
    type: EntityType<*, *>,
    world: WorldClient
) : MobPlayerClientMain(
    type, world, Vector3d.ZERO, Vector3d.ZERO,
    AABB3(-0.4, -0.4, -1.0, 0.4, 0.4, 0.9), 100.0, 100.0,
    Frustum(90.0, 1.0, 0.1, 24.0), ""
) {
    private var punchLeft: Long = -1
    private var punchRight: Long = -1
    private var chargeLeft = 0.0f
    private var chargeRight = 0.0f
    private var flyingPressed = false

    init {
        inventories.add("Container", 40)
        inventories.add("Hold", 1)
        registerComponent(GUI_COMPONENT) { player ->
            if (player is MobPlayerClientMainVB) {
                GuiPlayerInventory(player, player.game.engine.guiStyle)
            } else null
        }
        val texture = world.scene.skinStorage()[skin]
        registerComponent(
            ComponentMobLivingServerCondition.COMPONENT,
            ComponentMobLivingServerCondition(this)
        )
        attachModel {
            MobLivingModelHuman(world.game.modelHumanShared(), this, texture,
                false, true, {
                    if (input.requiresCameraSmoothing) RotationSmoothing.TIGHT
                    else RotationSmoothing.DISABLE
                })
        }
        onNotice[PLAYER_LISTENER_TOKEN] = { notice ->
            if (notice is MobLivingClient) {
                if (notice.getOrNull(
                        CreatureType.COMPONENT
                    ) == CreatureType.MONSTER) {
                    game.playlist.setMusic(Playlist.Music.BATTLE)
                }
            }
        }
    }

    internal fun setHotbarSelectLeft(value: Int) {
        inventorySelectLeft = value
        world.send(
            PacketInteraction(
                registry,
                PacketInteraction.INVENTORY_SLOT_LEFT, value.toByte()
            )
        )
    }

    internal fun setHotbarSelectRight(value: Int) {
        inventorySelectRight = value
        world.send(
            PacketInteraction(
                registry,
                PacketInteraction.INVENTORY_SLOT_RIGHT, value.toByte()
            )
        )
    }

    override fun update(delta: Double) {
        super<MobPlayerClientMain>.update(delta)
        if (getOrNull(
                ComponentMobLivingServerCondition.COMPONENT
            )?.sleeping == true) {
            return
        }
        if (!hasGui()) {
            // Debug
            if (Debug.enabled()) {
                (input as? InputModeKeyboard)?.controller?.let { controller ->
                    if (controller.isDown(ControllerKey.KEY_F5)) {
                        if (!flyingPressed) {
                            flyingPressed = true
                            flying = !flying
                        }
                    } else flyingPressed = false
                    if (flying) {
                        if (controller.isDown(ControllerKey.KEY_Q)) {
                            speed.addZ(60.0 * delta)
                        }
                        if (controller.isDown(ControllerKey.KEY_C)) {
                            speed.addZ(-60.0 * delta)
                        }
                    }
                }
            }
            // Movement
            if (input.jump()) {
                if (isSwimming) {
                    speed.addZ(50.0 * delta)
                } else if (isOnGround) {
                    speed.setZ(5.1)
                    physicsState.isOnGround = false
                    connection().send(PacketPlayerJump(registry))
                }
            }
            val walk = input.walk()
            var walkSpeed = clamp(
                max(
                    abs(walk.x),
                    abs(walk.y)
                ), 0.0, 1.0
            ) * 120.0
            if (!isOnGround && !physicsState.slidingWall && !isInWater && !flying) {
                walkSpeed *= 0.0006
            } else if (!isOnGround && !isInWater && !flying) {
                walkSpeed *= 0.05
            } else if (isInWater) {
                walkSpeed *= 0.2
            }
            if (walkSpeed > 0.0) {
                val dir = (walk.direction().toDeg() + rot.z - 90.0).toRad()
                walkSpeed *= delta
                speed.addX(cos(dir) * walkSpeed)
                speed.addY(sin(dir) * walkSpeed)
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
                world.send(
                    PacketItemUse(
                        registry, min(
                            (System.currentTimeMillis() - punchLeft).toDouble() / (leftWeapon().kind<ItemTypeWeapon>()?.hitWait()
                                    ?: 500),
                            0.5
                        ) * 2.0, true, direction
                    )
                )
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
                world.send(
                    PacketItemUse(
                        registry, min(
                            (System.currentTimeMillis() - punchRight).toDouble() / (rightWeapon().kind<ItemTypeWeapon>()?.hitWait()
                                    ?: 500),
                            0.5
                        ) * 2.0, false, direction
                    )
                )
                punchRight = -1
            }
            val time = System.currentTimeMillis()
            var swingTime = (leftWeapon().kind<ItemTypeWeapon>()?.hitWait()
                    ?: 500).toFloat()
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
            swingTime = (rightWeapon().kind<ItemTypeWeapon>()?.hitWait()
                    ?: 500).toFloat()
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

    override fun viewOffset(): Vector3d {
        return Vector3d(0.0, 0.0, 0.63)
    }

    override fun leftWeapon() =
        inventories.access("Container") { it[inventorySelectLeft] }

    override fun rightWeapon() =
        inventories.access("Container") { it[inventorySelectRight] }

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

    override fun read(map: TagMap) {
        super.read(map)
        map["Inventory"]?.toMap()?.let { inventoryTag ->
            inventories.forEach { id, inventory ->
                inventoryTag[id]?.toMap()?.let {
                    inventory.read(world.plugins, it)
                }
            }
        }
    }

    override fun ListenerRegistrar.inputMode(input: InputModeScapes) {
        listen<InputDirectionEvent> { event ->
            if (!hasGui()) {
                rot.setZ((rot.z - event.direction.x) % 360)
                rot.setX(
                    min(
                        89.0,
                        max(-89.0, rot.x - event.direction.y)
                    )
                )
            }
        }
        listen<HotbarChangeLeftEvent> { event ->
            if (!hasGui()) {
                setHotbarSelectLeft((inventorySelectLeft + event.delta) remP 10)
                event.success = true
            }
        }
        listen<HotbarChangeRightEvent> { event ->
            if (!hasGui()) {
                setHotbarSelectRight(
                    (inventorySelectRight + event.delta) remP 10
                )
                event.success = true
            }
        }
        listen<HotbarSetLeftEvent> { event ->
            if (!hasGui()) {
                setHotbarSelectLeft(event.value remP 10)
                event.success = true
            }
        }
        listen<HotbarSetRightEvent> { event ->
            if (!hasGui()) {
                setHotbarSelectRight(event.value remP 10)
                event.success = true
            }
        }
        listen<MenuOpenEvent> { event ->
            if (currentGui() !is GuiChatWrite) {
                event.success = true
                if (!closeGui()) {
                    openGui(
                        GuiPause(
                            game, this@MobPlayerClientMainVB,
                            game.engine.guiStyle
                        )
                    )
                }
            }
        }
        listen<MenuInventoryEvent> { event ->
            if (currentGui() !is GuiChatWrite) {
                event.success = true
                if (!closeGui()) {
                    world.send(
                        PacketInteraction(
                            registry,
                            PacketInteraction.OPEN_INVENTORY, 0
                        )
                    )
                }
            }
        }
        listen<MenuChatEvent> { event ->
            if (currentGui() !is GuiChatWrite) {
                event.success = true
                if (!hasGui()) {
                    openGui(
                        GuiChatWrite(
                            game, this@MobPlayerClientMainVB,
                            game.engine.guiStyle
                        )
                    )
                }
            }
        }
    }
}

private val PLAYER_LISTENER_TOKEN = ListenerToken("VanillaBasics:Player")
