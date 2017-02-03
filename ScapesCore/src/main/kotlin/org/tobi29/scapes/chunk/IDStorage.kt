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

import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getInt
import org.tobi29.scapes.engine.utils.io.tag.setInt

class IDStorage(private val tagStructure: TagStructure) {

    operator fun get(module: String,
                     type: String,
                     name: String): Int {
        return get(module, type, name, 0, Int.MAX_VALUE)
    }

    @Synchronized operator fun get(module: String,
                                   type: String,
                                   name: String,
                                   min: Int,
                                   max: Int): Int {
        val typeTag = tagStructure.structure(module).structure(type)
        typeTag.getInt(name)?.let { return it }
        var i = min
        while (true) {
            if (i > max) {
                throw IllegalStateException(
                        "Overflowed IDs for: $module->$type")
            }
            var contains = false
            for ((key, value) in typeTag.tagEntrySet) {
                if (value is Number) {
                    if (value.toInt() == i) {
                        contains = true
                        break
                    }
                }
            }
            if (contains) {
                i++
            } else {
                break
            }
        }
        typeTag.setInt(name, i)
        return i
    }

    @Synchronized fun set(module: String,
                          type: String,
                          name: String,
                          value: Int) {
        tagStructure.structure(module).structure(type).setInt(name, value)
    }

    fun save(): TagStructure {
        return tagStructure
    }
}
