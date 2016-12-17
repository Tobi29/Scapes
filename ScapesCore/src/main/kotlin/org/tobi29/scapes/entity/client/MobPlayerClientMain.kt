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

package org.tobi29.scapes.entity.client

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.terrain.TerrainClient
import org.tobi29.scapes.client.InputModeChangeEvent
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.length
import org.tobi29.scapes.entity.getEntities
import org.tobi29.scapes.entity.particle.ParticleEmitterBlock
import org.tobi29.scapes.packets.PacketInteraction
import java.util.concurrent.ThreadLocalRandom

abstract class MobPlayerClientMain protected constructor(world: WorldClient, pos: Vector3d, speed: Vector3d,
                                                         aabb: AABB, lives: Double, maxLives: Double, private val viewField: Frustum,
                                                         nickname: String) : MobPlayerClient(
        world, pos, speed, aabb, lives, maxLives, nickname) {
    val game: GameStateGameMP
    protected var controller: Controller
    protected var flying = false
    protected var swim = 0
    protected var gravitationMultiplier = 1.0
    protected var airFriction = 0.2
    protected var groundFriction = 1.6
    protected var wallFriction = 2.0
    protected var waterFriction = 8.0
    protected var stepHeight = 1.0
    protected val sendPositionHandler: MobPositionSenderClient

    init {
        game = world.game
        val game = this.game.engine.game as ScapesClient
        game.engine.events.listener<InputModeChangeEvent>(this) { event ->
            val inputGui = this.game.input()
            inputGui.removeAll()
            event.inputMode.createInGameGUI(inputGui, world)
            controller = event.inputMode.playerController(this)
        }
        val inputGui = this.game.input()
        inputGui.removeAll()
        game.inputMode().createInGameGUI(inputGui, world)
        controller = game.inputMode().playerController(this)
        sendPositionHandler = MobPositionSenderClient(this.pos.now(),
                { world.connection.send(it) })
    }

    protected fun updateVelocity(gravitation: Double,
                                 delta: Double) {
        speed.div(1.0 + airFriction * delta)
        if (isInWater) {
            speed.div(1.0 + waterFriction * delta)
        } else {
            if (isOnGround) {
                speed.div(1.0 + groundFriction * delta * gravitation)
            }
            if (slidingWall) {
                speed.div(1.0 + wallFriction * delta)
            }
        }
        speed.plusZ(-gravitation * gravitationMultiplier * delta)
    }

    protected fun move(aabb: AABB,
                       aabbs: Pool<AABBElement>,
                       goX: Double,
                       goY: Double,
                       goZ: Double) {
        var goX = goX
        var goY = goY
        var ground = false
        var slidingWall = false
        val lastGoZ = aabb.moveOutZ(collisions(aabbs), goZ)
        pos.plusZ(lastGoZ)
        aabb.add(0.0, 0.0, lastGoZ)
        if (lastGoZ - goZ > 0) {
            ground = true
        }
        // Walk
        var walking = true
        while (walking) {
            walking = false
            if (goX != 0.0) {
                val lastGoX = aabb.moveOutX(collisions(aabbs), goX)
                if (lastGoX != 0.0) {
                    pos.plusX(lastGoX)
                    aabb.add(lastGoX, 0.0, 0.0)
                    goX -= lastGoX
                    walking = true
                }
            }
            if (goY != 0.0) {
                val lastGoY = aabb.moveOutY(collisions(aabbs), goY)
                if (lastGoY != 0.0) {
                    pos.plusY(lastGoY)
                    aabb.add(0.0, lastGoY, 0.0)
                    goY -= lastGoY
                    walking = true
                }
            }
        }
        // Check collision
        val slidingX = goX != 0.0
        val slidingY = goY != 0.0
        if (slidingX || slidingY) {
            if (stepHeight > 0.0 && (this.isOnGround || isInWater)) {
                // Step
                // Calculate step height
                var aabbStep = AABB(aabb).add(goX, 0.0, 0.0)
                val stepX = aabbStep.moveOutZ(collisions(aabbs), stepHeight)
                aabbStep = AABB(aabb).add(0.0, goY, 0.0)
                val stepY = aabbStep.moveOutZ(collisions(aabbs), stepHeight)
                var step = max(stepX, stepY)
                aabbStep = AABB(aabb).add(goX, goY, step)
                step += aabbStep.moveOutZ(collisions(aabbs), -step)
                // Check step height
                aabbStep.copy(aabb).add(0.0, 0.0, step)
                step = aabb.moveOutZ(collisions(aabbs), step)
                // Attempt walk at new height
                val lastGoX = aabbStep.moveOutX(collisions(aabbs), goX)
                aabbStep.add(lastGoX, 0.0, 0.0)
                val lastGoY = aabbStep.moveOutY(collisions(aabbs), goY)
                // Check if walk was successful
                if (lastGoX != 0.0 || lastGoY != 0.0) {
                    pos.plusX(lastGoX)
                    pos.plusY(lastGoY)
                    aabb.copy(aabbStep).add(0.0, lastGoY, 0.0)
                    pos.plusZ(step)
                } else {
                    // Collide
                    slidingWall = true
                    if (slidingX) {
                        speed.setX(0.0)
                    }
                    if (slidingY) {
                        speed.setY(0.0)
                    }
                }
            } else {
                // Collide
                slidingWall = true
                if (slidingX) {
                    speed.setX(0.0)
                }
                if (slidingY) {
                    speed.setY(0.0)
                }
            }
        }
        this.isOnGround = ground
        this.slidingWall = slidingWall
    }

    protected fun collide(aabb: AABB,
                          aabbs: Pool<AABBElement>) {
        var inWater = false
        val swimming: Boolean
        for (element in aabbs) {
            if (aabb.overlay(element.aabb)) {
                if (element.collision.isLiquid) {
                    inWater = true
                }
            }
        }
        aabb.minZ = mix(aabb.minZ, aabb.maxZ, 0.6)
        var water = false
        for (element in aabbs) {
            if (aabb.overlay(element.aabb)) {
                if (element.collision.isLiquid) {
                    water = true
                }
            }
        }
        if (water) {
            swim++
            swimming = swim > 1
        } else {
            swimming = false
            swim = 0
        }
        this.isInWater = inWater
        this.isSwimming = swimming
    }

    fun updatePosition() {
        sendPositionHandler.submitUpdate(uuid, pos.now(), speed.now(),
                rot.now(),
                isOnGround, slidingWall, isInWater, isSwimming, true)
    }

    open fun onNotice(notice: MobClient) {
    }

    override fun onDamage(damage: Double) {
        world.scene.damageShake(damage)
    }

    override fun move(delta: Double) {
        updateVelocity(if (flying) 0.0 else world.gravity, delta)
        val goX = clamp(speed.doubleX() * delta, -1.0, 1.0)
        val goY = clamp(speed.doubleY() * delta, -1.0, 1.0)
        val goZ = clamp(speed.doubleZ() * delta, -1.0, 1.0)
        if (flying) {
            pos.plus(Vector3d(goX, goY, goZ))
        } else {
            val aabb = getAABB()
            val aabbs = world.terrain.collisions(
                    floor(aabb.minX + min(goX, 0.0)),
                    floor(aabb.minY + min(goY, 0.0)),
                    floor(aabb.minZ + min(goZ, 0.0)),
                    floor(aabb.maxX + max(goX, 0.0)),
                    floor(aabb.maxY + max(goY, 0.0)),
                    floor(aabb.maxZ + max(goZ, stepHeight)))
            move(aabb, aabbs, goX, goY, goZ)
            if (isOnGround) {
                speed.setZ(speed.doubleZ() / (1.0 + 4.0 * delta))
            }
            isHeadInWater = world.terrain.type(pos.intX(), pos.intY(),
                    floor(pos.doubleZ() + 0.7)).isLiquid
            collide(aabb, aabbs)
        }
        sendPositionHandler.submitUpdate(uuid, pos.now(), speed.now(),
                rot.now(),
                isOnGround, slidingWall, isInWater, isSwimming)
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
                        var size = texture.realSize() * 0.25
                        val tx = (random.nextInt(3) + 0.005) * size
                        val ty = (random.nextInt(3) + 0.005) * size
                        size *= 0.99f
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
                        instance.textureOffset.set(
                                Vector2d(texture.realX() + tx,
                                        texture.realY() + ty))
                        instance.textureSize.set(Vector2d(size, size))
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

    interface Controller {
        fun walk(): Vector2d

        fun camera(delta: Double): Vector2d

        fun hitDirection(): Vector2d

        fun left(): Boolean

        fun right(): Boolean

        fun jump(): Boolean

        fun hotbarLeft(previous: Int): Int

        fun hotbarRight(previous: Int): Int
    }

    companion object {
        protected fun collisions(aabbs: Pool<AABBElement>): Sequence<AABB> {
            return aabbs.asSequence().filter { it.isSolid }.map { it.aabb() }
        }
    }
}
