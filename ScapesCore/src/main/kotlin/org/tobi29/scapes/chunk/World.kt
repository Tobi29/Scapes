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
package org.tobi29.scapes.chunk

import java8.util.stream.Stream
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.engine.utils.math.vector.Vector3i
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.entity.Entity
import org.tobi29.scapes.entity.EntityContainer
import org.tobi29.scapes.plugins.Plugins

abstract class World<E : Entity> protected constructor(val plugins: Plugins, val taskExecutor: TaskExecutor,
                                                       val registry: GameRegistry, val seed: Long) : EntityContainer<E> {
    val air: BlockType
    protected var thread: Thread? = null
    var spawn = Vector3i.ZERO
        protected set
    var tick = 0L
        protected set
    var gravity = 9.8
        protected set

    init {
        air = registry.air()
    }

    fun checkThread(): Boolean {
        return Thread.currentThread() === thread
    }

    abstract fun getWorldEntities(): Stream<out E>
}