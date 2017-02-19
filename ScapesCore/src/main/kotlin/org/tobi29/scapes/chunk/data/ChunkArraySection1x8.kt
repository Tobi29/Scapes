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

package org.tobi29.scapes.chunk.data

import org.tobi29.scapes.engine.utils.io.tag.*

class ChunkArraySection1x8(private val xSizeBits: Int,
                           private val ySizeBits: Int,
                           zSizeBits: Int) : ChunkArraySection {
    private val size: Int
    private var data: ByteArray? = null
    private var defaultValue: Byte = 0
    private var changed = false

    init {
        size = 1 shl xSizeBits + ySizeBits + zSizeBits
    }

    override fun getData(x: Int,
                         y: Int,
                         z: Int,
                         offset: Int): Int {
        return getData((z shl ySizeBits or y shl xSizeBits or x) + offset)
    }

    override fun getData(offset: Int): Int {
        val data = this.data ?: return defaultValue.toInt()
        return data[offset].toInt()
    }

    override fun setData(x: Int,
                         y: Int,
                         z: Int,
                         offset: Int,
                         value: Int) {
        setData((z shl ySizeBits or y shl xSizeBits or x) + offset, value)
    }

    override fun setData(offset: Int,
                         value: Int) {
        var data = this.data
        val bValue = value.toByte()
        if (data == null) {
            if (bValue == defaultValue) {
                return
            }
            val newData = ByteArray(size)
            for (i in 0..size - 1) {
                newData[i] = defaultValue
            }
            data = newData
            data[offset] = bValue
            this.data = data
            changed = true
        } else {
            data[offset] = bValue
            changed = true
        }
    }

    override val isEmpty: Boolean
        get() = data == null && defaultValue == 0.toByte()

    override fun compress(): Boolean {
        val data = this.data ?: return true
        if (!changed) {
            return false
        }
        var flag = true
        for (i in 1..data.size - 1) {
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
            map["Default"] = defaultValue
        } else {
            map["Array"] = data
        }
    }

    override fun read(map: TagMap?) {
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
