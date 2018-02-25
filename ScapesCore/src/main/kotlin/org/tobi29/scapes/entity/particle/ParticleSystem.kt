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
package org.tobi29.scapes.entity.particle

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import org.tobi29.coroutines.Timer
import org.tobi29.coroutines.launchThread
import org.tobi29.coroutines.loop
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.utils.chain
import org.tobi29.graphics.Cam
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

class ParticleSystem(val world: WorldClient,
                     tps: Double) {
    private val emitters = ConcurrentHashMap<Class<out ParticleEmitter<*>>, ParticleEmitter<*>>()
    private var updateJob: Pair<Job, AtomicBoolean>? = null

    init {
        // TODO: Make init method
        val stop = AtomicBoolean(false)
        updateJob = launchThread("Particles") {
            Timer().apply { init() }.loop(Timer.toDiff(tps),
                    { delay(it, TimeUnit.NANOSECONDS) }) { delta ->
                if (stop.get()) return@loop false

                update(delta.coerceIn(0.0001, 0.1))

                true
            }
        } to stop
    }

    fun <P : ParticleEmitter<*>> register(emitter: P) {
        emitters.put(emitter::class.java, emitter)
    }

    fun <P : ParticleEmitter<*>> emitter(clazz: Class<P>): P {
        val emitter = emitters[clazz] ?: throw IllegalStateException(
                "Particle emitter not registered")
        @Suppress("UNCHECKED_CAST")
        return emitter as P
    }

    fun update(delta: Double) {
        emitters.values.forEach { emitter ->
            emitter.poll()
            emitter.update(delta)
        }
    }

    fun addToPipeline(gl: GL,
                      width: Int,
                      height: Int,
                      cam: Cam): suspend () -> (Double) -> Unit {
        val emitters = emitters.values.map {
            it to it.addToPipeline(gl, width, height, cam)
        }
        return {
            chain(*emitters.map { (emitter, render) ->
                val r = render()
                ;{ delta: Double ->
                emitter.pollRender()
                r(delta)
            }
            }.toTypedArray())
        }
    }

    suspend fun dispose() {
        updateJob?.let { (job, stop) ->
            stop.set(true)
            job.join()
        }
    }
}
