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

package org.tobi29.scapes.chunk.generator

import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.block.Update
import org.tobi29.stdex.ThreadLocal

class GeneratorOutput() {
    var height: Int = 0
        set(value) {
            if (field != value) {
                field = value
                type = IntArray(value)
                data = IntArray(value)
            }
        }
    var type = IntArray(height)
        private set
    var data = IntArray(height)
        private set
    val updates = ArrayList<(Registries) -> Update>()

    constructor(height: Int) : this() {
        this.height = height
    }

    fun type(z: Int,
             type: BlockType) {
        this.type[z] = type.id
    }

    fun data(z: Int,
             data: Int) {
        this.data[z] = data
    }

    companion object {
        private val TL = ThreadLocal { GeneratorOutput() }

        fun current(): GeneratorOutput = TL.get()
    }
}
