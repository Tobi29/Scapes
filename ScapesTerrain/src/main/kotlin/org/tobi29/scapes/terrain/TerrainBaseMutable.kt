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

interface TerrainBaseMutable<B : VoxelType> : TerrainBase<B> {
    fun block(x: Int,
              y: Int,
              z: Int,
              block: Long) {
        typeData(x, y, z, type(block), data(block))
    }

    fun type(x: Int,
             y: Int,
             z: Int,
             type: B)

    fun data(x: Int,
             y: Int,
             z: Int,
             data: Int)

    fun typeData(x: Int,
                 y: Int,
                 z: Int,
                 type: B,
                 data: Int)
}
