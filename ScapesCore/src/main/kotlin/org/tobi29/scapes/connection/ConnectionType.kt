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

package org.tobi29.scapes.connection

import java.util.*

enum class ConnectionType constructor(private val data: Byte) {
    GET_INFO(1.toByte()),
    PLAY(11.toByte()),
    CONTROL(21.toByte());

    fun data(): Byte {
        return data
    }

    companion object {
        private val VALUES = HashMap<Byte, ConnectionType>()

        init {
            for (type in values()) {
                VALUES.put(type.data, type)
            }
        }

        operator fun get(data: Byte): ConnectionType? {
            return VALUES[data]
        }
    }
}
