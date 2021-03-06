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

import org.tobi29.io.tag.*
import kotlin.collections.set

class ChunkArraySection2x4(private val xSizeBits: Int,
                           private val ySizeBits: Int,
                           zSizeBits: Int) : TagMapWrite {
    private val size = 1 shl xSizeBits + ySizeBits + zSizeBits
    private var data: ByteArray? = null
    private var defaultValue: Byte = 0
    private var changed = false

    fun getData(x: Int,
                y: Int,
                z: Int,
                second: Boolean): Int {
        return getData(z shl ySizeBits or y shl xSizeBits or x, second)
    }

    fun getData(offset: Int,
                second: Boolean): Int {
        val data = this.data
        if (second) {
            if (data == null) {
                return (defaultValue.toInt() and 0xF0).ushr(4)
            }
            return (data[offset].toInt() and 0xF0).ushr(4)
        } else {
            if (data == null) {
                return (defaultValue.toInt() and 0xF)
            }
            return (data[offset].toInt() and 0xF)
        }
    }

    fun setData(x: Int,
                y: Int,
                z: Int,
                second: Boolean,
                value: Int) {
        setData(z shl ySizeBits or y shl xSizeBits or x, second, value)
    }

    fun setData(offset: Int,
                second: Boolean,
                value: Int) {
        var data = this.data
        if (second) {
            val value2 = value shl 4
            if (data == null) {
                if (value2 == defaultValue.toInt() and 0xF0) {
                    return
                }
                data = ByteArray(size) { defaultValue }
                data[offset] = (data[offset].toInt() and 0xF or value2).toByte()
                this.data = data
                changed = true
            } else {
                data[offset] = (data[offset].toInt() and 0xF or value2).toByte()
                changed = true
            }
        } else {
            if (data == null) {
                if (value == defaultValue.toInt() and 0xF) {
                    return
                }
                data = ByteArray(size) { defaultValue }
                data[offset] = (data[offset].toInt() and 0xF0 or value).toByte()
                this.data = data
                changed = true
            } else {
                data[offset] = (data[offset].toInt() and 0xF0 or value).toByte()
                changed = true
            }
        }
    }

    val isEmpty get() = data == null && defaultValue == 0.toByte()

    fun compress(): Boolean {
        val data = this.data ?: return true
        if (!changed) {
            return false
        }
        var flag = true
        for (i in 1 until data.size) {
            if (data[i] != data[0]) {
                flag = false
                break
            }
        }
        if (flag) {
            defaultValue = data[0]
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
            map["Default"] = defaultValue.toTag()
        } else {
            map["Array"] = data.toTag()
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
                map["Default"]?.toByte()?.let {
                    defaultValue = it
                    data = null
                }
            }
        }
    }
}
