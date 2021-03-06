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

package org.tobi29.scapes.terrain

import org.tobi29.stdex.assert

interface TerrainGlobals<out B : VoxelType> {
    val air: B
    val blocks: Array<out B?>

    fun getThreadContext(): TerrainLock

    fun sunLightReduction(x: Int,
                          y: Int): Int

    fun type(block: Long) = typeOrNull(block) ?: air

    fun typeOrNull(block: Long): B? {
        val id = (block and 0xFFFFFFFF).toInt()
        if (id == -1) {
            return null
        }
        return type(id)
    }

    fun data(block: Long): Int {
        return (block ushr 32).toInt()
    }

    fun dataSafe(block: Long): Int {
        assert { block != -1L }
        return (block ushr 32).toInt()
    }

    fun type(id: Int): B
}

interface TerrainLock {
    val locked: Boolean

    fun getThreadContext() = this

    fun lock()

    fun unlock()
}
