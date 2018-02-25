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
package org.tobi29.scapes.chunk

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.tobi29.coroutines.TaskChannel
import org.tobi29.coroutines.offer
import org.tobi29.coroutines.processCurrent
import org.tobi29.logging.KLogging
import org.tobi29.scapes.block.Registries
import org.tobi29.math.vector.Vector3i
import org.tobi29.utils.ComponentHolder
import org.tobi29.utils.ComponentStorage
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.EntityContainer
import org.tobi29.scapes.plugins.Plugins
import kotlin.coroutines.experimental.CoroutineContext

abstract class World<E : Entity>(
        val plugins: Plugins,
        val taskExecutor: CoroutineContext,
        val registry: Registries,
        val seed: Long
) : CoroutineDispatcher(),
        ComponentHolder<Any>,
        EntityContainer<E> {
    override val componentStorage = ComponentStorage<Any>()
    val air = plugins.air
    private val queue = TaskChannel<() -> Unit>()
    @Volatile
    protected var thread: Thread? = null
    var spawn = Vector3i.ZERO
        protected set
    var tick = 0L
        protected set
    var gravity = 9.8
        protected set

    protected fun process() {
        queue.processCurrent()
    }

    override fun dispatch(context: CoroutineContext,
                          block: Runnable) {
        queue.offer {
            try {
                block.run()
            } catch (e: CancellationException) {
                logger.warn { "Job cancelled: ${e.message}" }
            }
        }
    }

    fun checkThread(): Boolean {
        return Thread.currentThread() === thread
    }

    companion object : KLogging()
}
