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

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.utils.ConcurrentLinkedQueue
import org.tobi29.scapes.engine.utils.assert
import org.tobi29.scapes.engine.utils.graphics.Cam

abstract class ParticleEmitter<P : ParticleInstance> protected constructor(protected val system: ParticleSystem,
                                                                           protected val instances: Array<P>) {
    protected val maxInstances: Int
    private val queue = ConcurrentLinkedQueue<(P) -> Unit>()
    private val activateQueue = ConcurrentLinkedQueue<P>()
    protected var hasAlive = false
    private var lastFree = 0

    init {
        maxInstances = instances.size
    }

    fun add(consumer: (P) -> Unit) {
        queue.add(consumer)
    }

    fun poll() {
        while (!queue.isEmpty()) {
            val consumer = queue.poll() ?: continue
            if (!findInstance(consumer)) {
                // Drain queue and exit because no particles left
                queue.clear()
                return
            }
        }
    }

    fun pollRender() {
        while (!activateQueue.isEmpty()) {
            val instance = activateQueue.poll() ?: continue
            assert { instance.state == ParticleInstance.State.NEW }
            instance.state = ParticleInstance.State.ALIVE
            hasAlive = true
        }
    }

    private fun findInstance(consumer: (P) -> Unit): Boolean {
        for (i in instances.indices) {
            val j = (i + lastFree) % instances.size
            val instance = instances[j]
            if (instance.state == ParticleInstance.State.DEAD) {
                lastFree = j + 1
                initInstance(instance, consumer)
                instance.state = ParticleInstance.State.NEW
                activateQueue.add(instance)
                return true
            }
        }
        return false
    }

    protected open fun initInstance(instance: P,
                                    consumer: (P) -> Unit) {
        consumer(instance)
    }

    abstract fun addToPipeline(gl: GL,
                               width: Int,
                               height: Int,
                               cam: Cam): suspend () -> (Double) -> Unit

    abstract fun update(delta: Double)
}
