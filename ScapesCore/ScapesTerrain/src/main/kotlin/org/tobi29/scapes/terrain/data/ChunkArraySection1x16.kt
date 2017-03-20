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

package org.tobi29.scapes.terrain.data

import org.tobi29.scapes.engine.utils.tag.*

class ChunkArraySection1x16(private val xSizeBits: Int,
                            private val ySizeBits: Int,
                            zSizeBits: Int) : TagMapWrite {
    private val size = 2 shl xSizeBits + ySizeBits + zSizeBits
    private var data: ByteArray? = null
    private var defaultValue: Short = 0
    private var changed = false

    fun getData(x: Int,
                y: Int,
                z: Int): Int {
        return getData(z shl ySizeBits or y shl xSizeBits or x)
    }

    fun getData(offset: Int): Int {
        val data = this.data ?: return defaultValue.toInt()
        val offset2 = offset shl 1
        return ((data[offset2].toInt() shl 8) + (data[offset2 + 1].toInt() and 0xFF))
    }

    fun setData(x: Int,
                y: Int,
                z: Int,
                value: Int) {
        setData(z shl ySizeBits or y shl xSizeBits or x, value)
    }

    fun setData(offset: Int,
                value: Int) {
        val offset2 = offset shl 1
        var data = this.data
        val sValue1 = (value shr 8).toByte()
        val sValue2 = (value and 0xFF).toByte()
        if (data == null) {
            if (value.toShort() == defaultValue) {
                return
            }
            val newData = ByteArray(size)
            val value1 = (defaultValue.toInt() shr 8).toByte()
            val value2 = (defaultValue.toInt() and 0xFF).toByte()
            var i = 0
            while (i < size) {
                newData[i] = value1
                newData[i + 1] = value2
                i += 2
            }
            data = newData
            data[offset2] = sValue1
            data[offset2 + 1] = sValue2
            this.data = data
            changed = true
        } else {
            if (sValue1 == data[offset2] && sValue2 == data[offset2 + 1]) {
                return
            }
            data[offset2] = sValue1
            data[offset2 + 1] = sValue2
            changed = true
        }
    }

    val isEmpty get() = data == null && defaultValue == 0.toShort()

    fun compress(): Boolean {
        val data = this.data ?: return true
        if (!changed) {
            return false
        }
        var flag = true
        var i = 2
        while (i < size) {
            if (data[i] != data[0] || data[i + 1] != data[1]) {
                flag = false
                break
            }
            i += 2
        }
        if (flag) {
            defaultValue = (data[0].toInt() shl 8 or data[1].toInt()).toShort()
            this.data = null
            changed = false
            return true
        }
        changed = false
        return false
    }

    override fun write(map: ReadWriteTagMap) {
        val data = this.data
        if (data == null) {
            map["Default"] = defaultValue
        } else {
            map["Array"] = data
        }
    }

    fun read(map: TagMap?) {
        if (map == null) {
            defaultValue = 0
            data = null
        } else {
            val array = map["Array"]?.toByteArray()
            if (array != null) {
                data = array
                defaultValue = 1
            } else {
                map["Default"]?.toShort()?.let {
                    defaultValue = it
                    data = null
                }
            }
        }
    }
}
