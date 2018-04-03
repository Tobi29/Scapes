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

package org.tobi29.scapes.entity.client

import org.tobi29.math.AABB3
import org.tobi29.math.Frustum
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.Vector2d
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.add
import org.tobi29.math.vector.length
import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.input.InputModeScapes
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.entity.EntityPhysics
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.ListenerToken
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.particle.ParticleEmitterBlock
import org.tobi29.scapes.packets.PacketInteraction
import org.tobi29.stdex.ThreadLocal
import org.tobi29.stdex.math.TWO_PI
import org.tobi29.stdex.math.clamp
import org.tobi29.stdex.math.floorToInt
import org.tobi29.stdex.math.toRad
import org.tobi29.utils.EventDispatcher
import org.tobi29.utils.ListenerRegistrar
import org.tobi29.utils.Pool
import org.tobi29.utils.forAllObjects
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

abstract class MobPlayerClientMain(
    type: EntityType<*, *>,
    world: WorldClient,
    pos: Vector3d,
    speed: Vector3d,
    aabb: AABB3,
    lives: Double,
    maxLives: Double,
    private val viewField: Frustum,
    nickname: String
) : MobPlayerClient(
    type,
    world, pos, speed, aabb, lives, maxLives, nickname
) {
    val game = world.game
    protected lateinit var input: InputModeScapes
    protected var flying = false
    protected var gravitationMultiplier = 1.0
    protected var airFriction = 0.2
    protected var groundFriction = 1.6
    protected var wallFriction = 2.0
    protected var waterFriction = 8.0
    protected var stepHeight = 1.0
    protected val sendPositionHandler = MobPositionSenderClient(
        registry,
        this.pos.now()
    ) { world.connection.send(it) }
    private var inputEventDispatcherwner: EventDispatcher? = null

    init {
        onDamage[PLAYER_LISTENER_TOKEN] = { damage ->
            world.scene.damageShake(damage)
        }
    }

    fun updatePosition() {
        sendPositionHandler.submitUpdate(
            uuid, pos.now(), speed.now(),
            rot.now(), physicsState.isOnGround, physicsState.slidingWall,
            physicsState.isInWater, physicsState.isSwimming, true
        )
    }

    override fun move(delta: Double) {
        if (flying) {
            val goX = clamp(speed.x * delta, -10.0, 10.0)
            val goY = clamp(speed.y * delta, -10.0, 10.0)
            val goZ = clamp(speed.z * delta, -10.0, 10.0)
            pos.add(Vector3d(goX, goY, goZ))
        } else {
            EntityPhysics.updateVelocity(
                delta, speed, world.gravity,
                gravitationMultiplier, airFriction, groundFriction,
                waterFriction, wallFriction, physicsState
            )
            val aabb = currentAABB()
            val aabbs = AABBS.get()
            EntityPhysics.collisions(
                delta, speed, world.terrain, aabb,
                stepHeight, aabbs
            )
            EntityPhysics.move(
                delta, pos, speed, aabb, stepHeight,
                physicsState, aabbs
            )
            if (isOnGround) {
                speed.setZ(speed.z / (1.0 + 4.0 * delta))
            }
            EntityPhysics.collide(delta, aabb, aabbs, physicsState) {}
            isHeadInWater = world.terrain.type(
                pos.x.floorToInt(),
                pos.y.floorToInt(), (pos.z + 0.7).floorToInt()
            ).isLiquid
            aabbs.reset()
            aabbs.forAllObjects { it.collision = BlockType.STANDARD_COLLISION }
        }
        sendPositionHandler.submitUpdate(
            uuid, pos.now(), speed.now(),
            rot.now(), physicsState.isOnGround, physicsState.slidingWall,
            physicsState.isInWater, physicsState.isSwimming
        )
        val lookX = cos(rot.z.toRad()) *
                cos(rot.x.toRad()) * 6.0
        val lookY = sin(rot.z.toRad()) *
                cos(rot.x.toRad()) * 6.0
        val lookZ = sin(rot.x.toRad()) * 6
        val viewOffset = viewOffset()
        viewField.setView(
            pos.x + viewOffset.x,
            pos.y + viewOffset.y,
            pos.z + viewOffset.z,
            pos.x + lookX, pos.y + lookY,
            pos.z + lookZ, 0.0, 0.0, 1.0
        )
        world.getEntities(
            viewField
        ).filterIsInstance<MobClient>().forEach { entity ->
            val pos2 = entity.getCurrentPos()
            if (!world.checkBlocked(
                    pos.x.floorToInt(),
                    pos.y.floorToInt(),
                    pos.z.floorToInt(),
                    pos2.x.floorToInt(),
                    pos2.y.floorToInt(),
                    pos2.z.floorToInt()
                )) {
                notice(entity)
            }
        }
        footStep -= delta
        if (footStep <= 0.0) {
            footStep = 0.0
            val currentSpeed = speed()
            if (max(
                    abs(currentSpeed.x),
                    abs(currentSpeed.y)
                ) > 0.1) {
                val x = pos.x.floorToInt()
                val y = pos.y.floorToInt()
                val block = world.terrain.block(
                    x, y,
                    (pos.z - 0.1).floorToInt()
                )
                var footStepSound = world.terrain.type(block).footStepSound(
                    world.terrain.data(block)
                )
                if (footStepSound == null && isOnGround) {
                    val blockBottom = world.terrain.block(
                        x, y,
                        (pos.z - 1.4).floorToInt()
                    )
                    footStepSound = world.terrain.type(
                        blockBottom
                    ).footStepSound(
                        world.terrain.data(blockBottom)
                    )
                }
                if (footStepSound != null) {
                    val random = threadLocalRandom()
                    game.engine.sounds.playSound(
                        footStepSound, "sound.World",
                        0.9 + random.nextDouble() * 0.2, 1.0
                    )
                    footStep = 1.0 / clamp(speed.now().length(), 1.0, 4.0)
                }
            }
        }
        if (invincibleTicks > 0) {
            invincibleTicks--
        }
    }

    protected fun breakParticles(
        terrain: TerrainClient,
        amount: Int,
        direction: Vector2d
    ) {
        val pane = block(6.0, direction)
        if (pane != null) {
            val block = terrain.block(pane.x, pane.y, pane.z)
            val type = terrain.type(block)
            val data = terrain.data(block)
            val texture = type.particleTexture(
                pane.face, terrain, pane.x,
                pane.y, pane.z, data
            )
            if (texture != null) {
                val blockPos = Vector3d(
                    pane.x.toDouble(), pane.y.toDouble(),
                    pane.z.toDouble()
                )
                val emitter = world.scene.particles().emitter(
                    ParticleEmitterBlock::class.java
                )
                val friction = type.particleFriction(
                    pane.face, terrain,
                    pane.x, pane.y, pane.z, data
                )
                val r = type.particleColorR(
                    pane.face, terrain, pane.x, pane.y,
                    pane.z, data
                )
                val g = type.particleColorG(
                    pane.face, terrain, pane.x, pane.y,
                    pane.z, data
                )
                val b = type.particleColorB(
                    pane.face, terrain, pane.x, pane.y,
                    pane.z, data
                )
                for (i in 0 until amount) {
                    emitter.add { instance ->
                        val random = threadLocalRandom()
                        val time = 3.0f
                        instance.pos.set(blockPos)
                        instance.pos.add(
                            Vector3d(
                                random.nextDouble(),
                                random.nextDouble(), random.nextDouble()
                            )
                        )
                        instance.speed.setXYZ(
                            -1.0 + random.nextDouble() * 2.0,
                            -1.0 + random.nextDouble() * 2.0,
                            random.nextDouble() * 2.0 + 1.0
                        )
                        instance.time = time
                        instance.friction = friction
                        instance.dir = random.nextFloat() * TWO_PI.toFloat()
                        instance.setTexture(texture, random)
                        instance.setColor(r, g, b, 1.0f)
                    }
                }
            }
        }
    }

    @Synchronized
    fun openGui(gui: Gui) {
        game.engine.guiStack.add("10-Menu", gui)
        game.setHudVisible(false)
    }

    fun currentGui(): Gui? {
        return game.engine.guiStack["10-Menu"]
    }

    fun hasGui(): Boolean {
        return game.engine.guiStack.has("10-Menu")
    }

    @Synchronized
    fun closeGui(): Boolean {
        if (game.engine.guiStack.remove("10-Menu") != null) {
            game.setHudVisible(true)
            world.send(
                PacketInteraction(
                    world.plugins.registry,
                    PacketInteraction.CLOSE_INVENTORY, 0
                )
            )
            return true
        }
        return false
    }

    fun game(): GameStateGameMP {
        return game
    }

    fun connection(): ClientConnection {
        return game.client()
    }

    @Synchronized
    fun setInputMode(input: InputModeScapes?) {
        inputEventDispatcherwner?.disable()
        game.inputGui.removeAll()
        if (input == null) {
            inputEventDispatcherwner = null
            return
        }
        input.createInGameGUI(game.inputGui, world)
        val inputEventDispatcher = EventDispatcher(game.engine.events) {
            inputMode(input)
        }
        this.input = input
        this.inputEventDispatcherwner = inputEventDispatcher
        inputEventDispatcher.enable()
    }

    protected abstract fun ListenerRegistrar.inputMode(input: InputModeScapes)

    class InputDirectionEvent(val direction: Vector2d)

    class HotbarChangeLeftEvent(val delta: Int) {
        var success: Boolean = true
    }

    class HotbarChangeRightEvent(val delta: Int) {
        var success: Boolean = true
    }

    class HotbarSetLeftEvent(val value: Int) {
        var success: Boolean = true
    }

    class HotbarSetRightEvent(val value: Int) {
        var success: Boolean = true
    }

    class MenuOpenEvent {
        var success: Boolean = true
    }

    class MenuInventoryEvent {
        var success: Boolean = true
    }

    class MenuChatEvent {
        var success: Boolean = true
    }

    companion object {
        private val AABBS = ThreadLocal { Pool { AABBElement() } }
    }
}

private val PLAYER_LISTENER_TOKEN = ListenerToken("Scapes:Player")
