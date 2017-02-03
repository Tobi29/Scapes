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

package org.tobi29.scapes.block

import org.tobi29.scapes.chunk.terrain.TerrainServer

abstract class Update {
    protected var x = 0
    protected var y = 0
    protected var z = 0
    protected var delay = 0.0
    var isValid = true
        private set

    fun set(x: Int,
            y: Int,
            z: Int,
            delay: Double): Update {
        this.x = x
        this.y = y
        this.z = z
        this.delay = delay
        return this
    }

    fun markAsInvalid() {
        isValid = false
    }

    fun delay(delta: Double): Double {
        delay -= delta
        return delay
    }

    fun delay(): Double {
        return delay
    }

    fun id(registry: GameRegistry): Int {
        return registry.getSupplier<GameRegistry, Update>("Core", "Update").id(
                this)
    }

    fun x(): Int {
        return x
    }

    fun y(): Int {
        return y
    }

    fun z(): Int {
        return z
    }

    abstract fun run(terrain: TerrainServer.TerrainMutable)

    abstract fun isValidOn(type: BlockType,
                           terrain: TerrainServer): Boolean

    companion object {

        fun make(registry: GameRegistry,
                 x: Int,
                 y: Int,
                 z: Int,
                 delay: Double,
                 id: Int): Update {
            try {
                return registry.getSupplier<GameRegistry, Update>("Core",
                        "Update")[id](registry).set(x, y, z, delay)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                        "Failed to make update over reflection! (id: " +
                                id + ')', e)
            }

        }
    }
}
