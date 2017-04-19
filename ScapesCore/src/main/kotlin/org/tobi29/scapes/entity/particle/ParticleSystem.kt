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

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.utils.Sync
import org.tobi29.scapes.engine.utils.chain
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.ConcurrentHashMap
import org.tobi29.scapes.engine.utils.task.Joiner
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.engine.utils.toArray

class ParticleSystem(val world: WorldClient,
                     tps: Double) {
    private val emitters = ConcurrentHashMap<Class<out ParticleEmitter<*>>, ParticleEmitter<*>>()
    private val joiner: Joiner

    init {
        joiner = world.game.engine.taskExecutor.runThread({ joiner ->
            val sync = Sync(tps, 0, false, "Particles")
            sync.init()
            while (!joiner.marked) {
                update(sync.delta())
                sync.cap(joiner)
            }
        }, "Particles", TaskExecutor.Priority.MEDIUM)
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
                      cam: Cam): () -> Unit {
        return chain(*emitters.values.asSequence().map {
            val render = it.addToPipeline(gl, width, height, cam);
            {
                it.pollRender()
                render()
            }
        }.toArray())
    }

    fun world(): WorldClient {
        return world
    }

    fun dispose() {
        joiner.join()
    }
}
