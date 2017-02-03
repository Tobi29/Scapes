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

import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getShort
import org.tobi29.scapes.engine.utils.io.tag.setShort

class ChunkArraySection1x16(private val xSizeBits: Int,
                            private val ySizeBits: Int,
                            zSizeBits: Int) : ChunkArraySection {
    private val size: Int
    private var data: ByteArray? = null
    private var defaultValue: Short = 0
    private var changed = false

    init {
        size = 2 shl xSizeBits + ySizeBits + zSizeBits
    }

    override fun getData(x: Int,
                         y: Int,
                         z: Int,
                         offset: Int): Int {
        return getData((z shl ySizeBits or y shl xSizeBits or x) + offset)
    }

    override fun getData(offset: Int): Int {
        val data = this.data ?: return defaultValue.toInt()
        val offset2 = offset shl 1
        return ((data[offset2].toInt() shl 8) + (data[offset2 + 1].toInt() and 0xFF))
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
        val offset2 = offset shl 1
        var data = this.data
        val sValue = value.toShort()
        if (data == null) {
            if (sValue == defaultValue) {
                return
            }
            val newData = ByteArray(size)
            val value1 = (defaultValue.toLong() shr 8).toByte()
            val value2 = defaultValue.toByte()
            var i = 0
            while (i < size) {
                newData[i] = value1
                newData[i + 1] = value2
                i += 2
            }
            data = newData
            data[offset2] = (sValue.toInt() shr 8).toByte()
            data[offset2 + 1] = sValue.toByte()
            this.data = data
            changed = true
        } else {
            data[offset2] = (sValue.toInt() shr 8).toByte()
            data[offset2 + 1] = sValue.toByte()
            changed = true
        }
    }

    override val isEmpty: Boolean
        get() = data == null && defaultValue == 0.toShort()

    override fun compress(): Boolean {
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

    override fun save(): TagStructure? {
        val data = this.data
        val tag = TagStructure()
        if (data == null) {
            if (defaultValue == 0.toShort()) {
                return null
            } else {
                tag.setShort("Default", defaultValue)
            }
        } else {
            tag.setByteArray("Array", *data)
        }
        return tag
    }

    override fun load(tag: TagStructure?) {
        if (tag == null) {
            defaultValue = 0
            data = null
        } else {
            val array = tag.getByteArray("Array")
            if (array != null) {
                data = array
                defaultValue = 0
            } else if (tag.has("Default")) {
                defaultValue = tag.getShort("Default") ?: 0
                data = null
            }
        }
    }
}
