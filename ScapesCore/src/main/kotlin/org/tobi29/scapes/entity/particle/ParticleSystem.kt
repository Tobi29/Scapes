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
package org.tobi29.scapes.entity.particle

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.utils.Sync
import org.tobi29.scapes.engine.utils.graphics.Cam
import org.tobi29.scapes.engine.utils.task.Joiner
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import java.util.concurrent.ConcurrentHashMap

class ParticleSystem(val world: WorldClient, tps: Double) {
    @SuppressWarnings("rawtypes")
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
        emitters.put(emitter.javaClass, emitter)
    }

    @SuppressWarnings("unchecked")
    fun <P : ParticleEmitter<*>> emitter(clazz: Class<P>): P {
        val emitter = emitters[clazz] ?: throw IllegalStateException(
                "Particle emitter not registered")
        return emitter as P
    }

    fun update(delta: Double) {
        emitters.values.forEach { emitter ->
            emitter.poll()
            emitter.update(delta)
        }
    }

    fun render(gl: GL,
               cam: Cam) {
        emitters.values.forEach { emitter ->
            emitter.pollRender()
            emitter.render(gl, cam)
        }
    }

    fun world(): WorldClient {
        return world
    }

    fun dispose() {
        joiner.join()
    }
}
