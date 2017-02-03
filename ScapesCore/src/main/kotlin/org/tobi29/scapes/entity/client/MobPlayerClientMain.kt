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

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.input.InputMode
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.utils.*
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.length
import org.tobi29.scapes.entity.EntityPhysics
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.particle.ParticleEmitterBlock
import org.tobi29.scapes.packets.PacketInteraction
import java.util.concurrent.ThreadLocalRandom

abstract class MobPlayerClientMain(world: WorldClient,
                                   pos: Vector3d,
                                   speed: Vector3d,
                                   aabb: AABB,
                                   lives: Double,
                                   maxLives: Double,
                                   private val viewField: Frustum,
                                   nickname: String) : MobPlayerClient(
        world, pos, speed, aabb, lives, maxLives, nickname) {
    val game = world.game
    protected lateinit var input: InputMode
    protected var flying = false
    protected var gravitationMultiplier = 1.0
    protected var airFriction = 0.2
    protected var groundFriction = 1.6
    protected var wallFriction = 2.0
    protected var waterFriction = 8.0
    protected var stepHeight = 1.0
    protected val sendPositionHandler = MobPositionSenderClient(
            this.pos.now()) { world.connection.send(it) }
    private var inputListenerOwner: InputListenerOwner? = null

    fun updatePosition() {
        sendPositionHandler.submitUpdate(uuid, pos.now(), speed.now(),
                rot.now(), physicsState.isOnGround, physicsState.slidingWall,
                physicsState.isInWater, physicsState.isSwimming, true)
    }

    open fun onNotice(notice: MobClient) {
    }

    override fun onDamage(damage: Double) {
        world.scene.damageShake(damage)
    }

    override fun move(delta: Double) {
        if (flying) {
            val goX = clamp(speed.doubleX() * delta, -10.0, 10.0)
            val goY = clamp(speed.doubleY() * delta, -10.0, 10.0)
            val goZ = clamp(speed.doubleZ() * delta, -10.0, 10.0)
            pos.plus(Vector3d(goX, goY, goZ))
        } else {
            EntityPhysics.updateVelocity(delta, speed, world.gravity,
                    gravitationMultiplier, airFriction, groundFriction,
                    waterFriction, wallFriction, physicsState)
            val aabb = getAABB()
            val aabbs = AABBS.get()
            EntityPhysics.collisions(delta, speed, world.terrain, aabb,
                    stepHeight, aabbs)
            EntityPhysics.move(delta, pos, speed, aabb, stepHeight,
                    physicsState, aabbs)
            if (isOnGround) {
                speed.setZ(speed.doubleZ() / (1.0 + 4.0 * delta))
            }
            EntityPhysics.collide(delta, aabb, aabbs, physicsState) {}
            isHeadInWater = world.terrain.type(pos.intX(), pos.intY(),
                    floor(pos.doubleZ() + 0.7)).isLiquid
            aabbs.reset()
        }
        sendPositionHandler.submitUpdate(uuid, pos.now(), speed.now(),
                rot.now(), physicsState.isOnGround, physicsState.slidingWall,
                physicsState.isInWater, physicsState.isSwimming)
        val lookX = cos(rot.doubleZ().toRad()) *
                cos(rot.doubleX().toRad()) * 6.0
        val lookY = sin(rot.doubleZ().toRad()) *
                cos(rot.doubleX().toRad()) * 6.0
        val lookZ = sin(rot.doubleX().toRad()) * 6
        val viewOffset = viewOffset()
        viewField.setView(pos.doubleX() + viewOffset.x,
                pos.doubleY() + viewOffset.y,
                pos.doubleZ() + viewOffset.z,
                pos.doubleX() + lookX, pos.doubleY() + lookY,
                pos.doubleZ() + lookZ, 0.0, 0.0, 1.0)
        world.getEntities(viewField).filterMap<MobClient>().forEach { entity ->
            val pos2 = entity.getCurrentPos()
            if (!world.checkBlocked(pos.intX(), pos.intY(),
                    pos.intZ(), pos2.intX(), pos2.intY(),
                    pos2.intZ())) {
                onNotice(entity)
            }
        }
        footStep -= delta
        if (footStep <= 0.0) {
            footStep = 0.0
            val currentSpeed = speed()
            if (max(abs(currentSpeed.x),
                    abs(currentSpeed.y)) > 0.1) {
                val x = pos.intX()
                val y = pos.intY()
                val block = world.terrain.block(x, y,
                        floor(pos.doubleZ() - 0.1))
                var footStepSound = world.terrain.type(block).footStepSound(
                        world.terrain.data(block))
                if (footStepSound == null && isOnGround) {
                    val blockBottom = world.terrain.block(x, y,
                            floor(pos.doubleZ() - 1.4))
                    footStepSound = world.terrain.type(
                            blockBottom).footStepSound(
                            world.terrain.data(blockBottom))
                }
                if (footStepSound != null) {
                    val random = ThreadLocalRandom.current()
                    game.engine.sounds.playSound(footStepSound, "sound.World",
                            0.9f + random.nextFloat() * 0.2f, 1.0f)
                    footStep = 1.0 / clamp(speed.now().length(), 1.0, 4.0)
                }
            }
        }
        if (invincibleTicks > 0) {
            invincibleTicks--
        }
    }

    protected fun breakParticles(terrain: TerrainClient,
                                 amount: Int,
                                 direction: Vector2d) {
        val pane = block(6.0, direction)
        if (pane != null) {
            val block = terrain.block(pane.x, pane.y, pane.z)
            val type = terrain.type(block)
            val data = terrain.data(block)
            val texture = type.particleTexture(pane.face, terrain, pane.x,
                    pane.y, pane.z, data)
            if (texture != null) {
                val blockPos = Vector3d(pane.x.toDouble(), pane.y.toDouble(),
                        pane.z.toDouble())
                val emitter = world.scene.particles().emitter(
                        ParticleEmitterBlock::class.java)
                val friction = type.particleFriction(pane.face, terrain,
                        pane.x, pane.y, pane.z, data)
                val r = type.particleColorR(pane.face, terrain, pane.x, pane.y,
                        pane.z, data)
                val g = type.particleColorG(pane.face, terrain, pane.x, pane.y,
                        pane.z, data)
                val b = type.particleColorB(pane.face, terrain, pane.x, pane.y,
                        pane.z, data)
                for (i in 0..amount - 1) {
                    emitter.add { instance ->
                        val random = ThreadLocalRandom.current()
                        val time = 3.0f
                        instance.pos.set(blockPos)
                        instance.pos.plus(Vector3d(random.nextDouble(),
                                random.nextDouble(), random.nextDouble()))
                        instance.speed.set(-1.0f + random.nextFloat() * 2.0f,
                                -1.0f + random.nextFloat() * 2.0f,
                                random.nextFloat() * 2.0f + 1.0f)
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

    @Synchronized fun openGui(gui: Gui) {
        game.engine.guiStack.add("10-Menu", gui)
        game.setHudVisible(false)
    }

    fun currentGui(): Gui? {
        return game.engine.guiStack["10-Menu"]
    }

    fun hasGui(): Boolean {
        return game.engine.guiStack.has("10-Menu")
    }

    @Synchronized fun closeGui(): Boolean {
        if (game.engine.guiStack.remove("10-Menu") != null) {
            game.setHudVisible(true)
            world.send(PacketInteraction(
                    PacketInteraction.CLOSE_INVENTORY))
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
    fun setInputMode(input: InputMode) {
        inputListenerOwner?.invalidate()
        val inputListenerOwner = InputListenerOwner()
        val inputGui = game.input()
        inputGui.removeAll()
        input.createInGameGUI(inputGui, world)
        inputMode(input, inputListenerOwner)
        this.input = input
        this.inputListenerOwner = inputListenerOwner
    }

    protected abstract fun inputMode(input: InputMode,
                                     listenerOwner: ListenerOwner)

    class InputDirectionEvent(val direction: Vector2d)

    class HotbarChangeLeftEvent(val delta: Int)

    class HotbarChangeRightEvent(val delta: Int)

    class HotbarSetLeftEvent(val value: Int)

    class HotbarSetRightEvent(val value: Int)

    class MenuOpenEvent {
        var success: Boolean = true
    }

    class MenuInventoryEvent {
        var success: Boolean = true
    }

    class MenuChatEvent {
        var success: Boolean = true
    }

    private inner class InputListenerOwner : ListenerOwner {
        private var valid = true
        override val listenerOwner = ListenerOwnerHandle {
            valid && this@MobPlayerClientMain.listenerOwner.isValid
        }

        fun invalidate() {
            valid = false
        }
    }

    companion object {
        private val AABBS = ThreadLocal { Pool { AABBElement() } }
    }
}
