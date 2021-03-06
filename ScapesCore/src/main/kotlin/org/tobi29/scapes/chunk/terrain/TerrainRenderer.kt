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
package org.tobi29.scapes.chunk.terrain

import org.tobi29.graphics.Cam
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.stdex.ConcurrentHashMap

interface TerrainRenderer {
    var lodDistance: Int

    fun renderUpdate(cam: Cam)

    fun render(gl: GL,
               shader1: Shader,
               shader2: Shader,
               cam: Cam,
               debug: Boolean)

    fun renderAlpha(gl: GL,
                    shader1: Shader,
                    shader2: Shader,
                    cam: Cam)

    fun blockChange(x: Int,
                    y: Int,
                    z: Int)

    fun actualRenderDistance(): Double
}

class TerrainRenderInfo(
        layers: Map<String, () -> TerrainRenderInfo.InfoLayer>) {
    private val layers = ConcurrentHashMap<String, InfoLayer>()

    init {
        layers.forEach { this.layers.put(it.key, it.value()) }
    }

    fun init(x: Int,
             y: Int,
             z: Int,
             xSize: Int,
             ySize: Int,
             zSize: Int) {
        layers.values.forEach { it.init(x, y, z, xSize, ySize, zSize) }
    }

    operator fun <E : InfoLayer> get(name: String): E {
        @Suppress("UNCHECKED_CAST")
        return layers[name] as E
    }

    interface InfoLayer {
        fun init(x: Int,
                 y: Int,
                 z: Int,
                 xSize: Int,
                 ySize: Int,
                 zSize: Int)
    }
}
