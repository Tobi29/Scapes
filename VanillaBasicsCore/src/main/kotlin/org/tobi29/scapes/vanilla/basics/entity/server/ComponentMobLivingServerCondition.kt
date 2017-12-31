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

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.math.threadLocalRandom
import org.tobi29.scapes.engine.math.vector.lengthSqr
import org.tobi29.scapes.engine.utils.*
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.tag.*
import org.tobi29.scapes.engine.utils.task.Timer
import org.tobi29.scapes.engine.utils.task.loop
import org.tobi29.scapes.entity.*
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.packets.PacketEntityComponentData
import org.tobi29.scapes.vanilla.basics.world.EnvironmentOverworldServer
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class ComponentMobLivingServerCondition(
        val entity: MobLiving
) : ComponentRegistered,
        ComponentMapSerializable {
    override val id = "VanillaBasics:Condition"
    var stamina: Double = 0.0
        @Synchronized get
        @Synchronized set
    var wake = 0.0
        @Synchronized get
        @Synchronized set
    var hunger = 0.0
        @Synchronized get
        @Synchronized set
    var thirst = 0.0
        @Synchronized get
        @Synchronized set
    var bodyTemperature = 0.0
        @Synchronized get
        @Synchronized set
    var sleeping = false
        @Synchronized get
        @Synchronized set

    override fun read(map: TagMap) {
        map["Stamina"]?.toDouble()?.let { stamina = it }
        map["Wake"]?.toDouble()?.let { wake = it }
        map["Hunger"]?.toDouble()?.let { hunger = it }
        map["Thirst"]?.toDouble()?.let { thirst = it }
        map["BodyTemperature"]?.toDouble()?.let { bodyTemperature = it }
        map["Sleeping"]?.toBoolean()?.let { sleeping = it }
    }

    override fun write(map: ReadWriteTagMap) {
        map["Stamina"] = stamina.toTag()
        map["Wake"] = wake.toTag()
        map["Hunger"] = hunger.toTag()
        map["Thirst"] = thirst.toTag()
        map["BodyTemperature"] = bodyTemperature.toTag()
        map["Sleeping"] = sleeping.toTag()
    }

    override fun init() {
        if (entity is MobLivingServer) {
            entity.onSpawn[CONDITION_LISTENER_TOKEN] = {
                synchronized(this@ComponentMobLivingServerCondition) {
                    stamina = 1.0
                    wake = 1.0
                    hunger = 1.0
                    thirst = 1.0
                    bodyTemperature = 37.0
                    sleeping = false
                }
            }
        }
        entity.onJump[CONDITION_LISTENER_TOKEN] = {
            synchronized(this@ComponentMobLivingServerCondition) {
                stamina -= 0.15
                bodyTemperature += 0.1
            }
        }
        if (entity is MobPlayerServer) {
            entity.onPunch[CONDITION_LISTENER_TOKEN] = { strength ->
                var attackStrength = strength
                if (entity.wieldMode() != WieldMode.DUAL) {
                    attackStrength *= 1.7
                }
                synchronized(this@ComponentMobLivingServerCondition) {
                    stamina -= 0.04 * attackStrength
                    bodyTemperature += 0.03 * attackStrength
                }
            }
            entity.world[ComponentConditionUpdater.COMPONENT]
                    .instances.add(this to entity)
        }
    }

    override fun dispose() {
        if (entity is MobLivingServer) {
            entity.onSpawn.remove(CONDITION_LISTENER_TOKEN)
        }
        entity.onJump.remove(CONDITION_LISTENER_TOKEN)
        if (entity is MobPlayerServer) {
            entity.onPunch.remove(CONDITION_LISTENER_TOKEN)
            entity.world[ComponentConditionUpdater.COMPONENT]
                    .instances.remove(this to entity)
        }
    }

    companion object {
        val COMPONENT = ComponentTypeRegisteredEntity<MobLivingServer, ComponentMobLivingServerCondition>()
    }
}

class ComponentConditionUpdater : ComponentRegisteredHolder<WorldServer> {
    private var job: Pair<Job, AtomicBoolean>? = null
    val instances = ConcurrentHashSet<Pair<ComponentMobLivingServerCondition, MobLivingServer>>()

    @Synchronized
    override fun init(holder: WorldServer) {
        val environment = holder.environment as EnvironmentOverworldServer
        val climateGenerator = environment.climate()

        val stop = AtomicBoolean(false)
        job = launch(holder.taskExecutor + CoroutineName("Condition-Updater")) {
            val timer = Timer()
            timer.init()
            timer.loop(Timer.toDiff(4.0),
                    { delay(it, TimeUnit.NANOSECONDS) }) {
                if (stop.get()) return@loop false

                val random = threadLocalRandom()
                for ((condition, entity) in instances) {
                    val health = entity.health()
                    val maxHealth = entity.maxHealth()

                    condition.run {
                        synchronized(this) {
                            val ground = entity.isOnGround
                            val inWater = entity.isInWater
                            val pos = entity.getCurrentPos()
                            val temperature = climateGenerator.temperature(
                                    pos.intX(), pos.intY(), pos.intZ())
                            val regenFactor = if (sleeping) 1.5 else 1.0
                            val depleteFactor = if (sleeping) 0.05 else 1.0
                            if (stamina > 0.2 && health < maxHealth) {
                                val rate = stamina * 0.5
                                entity.heal(rate)
                                stamina -= rate * 0.1
                            }
                            if (inWater) {
                                val rate = clamp(
                                        entity.speed().lengthSqr() * 0.00125,
                                        0.0, 0.05)
                                stamina -= rate
                                bodyTemperature += rate
                                thirst -= rate * 0.075
                            } else if (ground) {
                                val rate = clamp(
                                        entity.speed().lengthSqr() * 0.00025,
                                        0.0, 0.05)
                                stamina -= rate
                                bodyTemperature += rate
                            }
                            stamina -= depleteFactor * 0.00025
                            if (inWater && thirst < 1.0) {
                                thirst += 0.025
                            }
                            if (stamina < 1.0) {
                                val rate = regenFactor * hunger * thirst * 0.05 *
                                        (1 - stamina)
                                stamina += rate
                                wake -= rate * 0.005
                                hunger -= rate * 0.003
                                thirst -= rate * 0.01
                            }
                            bodyTemperature += (temperature - bodyTemperature) / 2000.0
                            if (bodyTemperature < 37.0) {
                                var rate = max(37.0 - bodyTemperature, 0.0)
                                rate = min(rate * 8.0 * stamina, 1.0) * 0.04
                                bodyTemperature += rate
                                stamina -= rate * 0.5
                            } else if (bodyTemperature > 37.0) {
                                var rate = max(bodyTemperature - 37.0, 0.0)
                                rate = min(rate * thirst, 1.0) * 0.06
                                bodyTemperature -= rate
                                thirst -= rate * 0.05
                            }
                            if (sleeping) {
                                wake += 0.0002
                                val wakeChance = 7.0 - wake * 7.0
                                if (random.nextDouble() > wakeChance) {
                                    sleeping = false
                                }
                            } else {
                                val sleepChance = wake * 10.0
                                if (random.nextDouble() > sleepChance) {
                                    sleeping = true
                                }
                            }
                            stamina = min(stamina, 1.0)
                            if (stamina <= 0.0) {
                                entity.damage(5.0)
                            }
                            wake = clamp(wake, 0.0, 1.0)
                            hunger = clamp(hunger, 0.0, 1.0)
                            thirst = clamp(thirst, 0.0, 1.0)
                        }
                    }
                    if (entity is MobPlayerServer) {
                        entity.connection().send(
                                PacketEntityComponentData(holder.registry,
                                        entity))
                    }
                }

                true
            }
        } to stop
    }

    @Synchronized
    override fun dispose() {
        job?.let { (job, stop) ->
            stop.getAndSet(true)
            // job.join() TODO: Delay world shutdown until job completes
            this.job = null
        }
    }

    companion object {
        val COMPONENT = ComponentType.of<WorldServer, ComponentConditionUpdater, Any> {
            ComponentConditionUpdater()
        }
    }
}

private val CONDITION_LISTENER_TOKEN = ListenerToken("VanillaBasics:Condition")
