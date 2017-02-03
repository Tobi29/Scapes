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

package org.tobi29.scapes.block

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.TextureAtlasEngine

class TerrainTextureRegistry(engine: ScapesEngine) : TextureAtlasEngine<TerrainTexture>(
        engine, 16) {

    fun registerTexture(vararg paths: String): TerrainTexture {
        return registerTexture(paths, false, ShaderAnimation.NONE)
    }

    fun registerTexture(path: String,
                        shaderAnimation: ShaderAnimation): TerrainTexture {
        return registerTexture(path, false, shaderAnimation)
    }

    fun registerTexture(paths: Array<out String>,
                        shaderAnimation: ShaderAnimation): TerrainTexture {
        return registerTexture(paths, false, shaderAnimation)
    }

    fun registerTexture(path: String,
                        animated: Boolean = false,
                        shaderAnimation: ShaderAnimation = ShaderAnimation.NONE): TerrainTexture {
        return registerTexture(arrayOf(path), animated, shaderAnimation)
    }

    fun registerTexture(paths: Array<out String>,
                        animated: Boolean,
                        shaderAnimation: ShaderAnimation = ShaderAnimation.NONE): TerrainTexture {
        val path = path(paths)
        textures[path]?.let { return it }
        val image = load(paths)
        val texture = if (animated) {
            AnimatedTerrainTexture(image.buffer, image.width, image.height,
                    shaderAnimation, engine, { texture })
        } else {
            TerrainTexture(image.buffer, image.width, image.height,
                    shaderAnimation, { texture })
        }
        textures[path] = texture
        return texture
    }

    fun render(gl: GL) {
        for (texture in textures.values) {
            texture.renderAnim(gl)
        }
    }

    fun update(delta: Double) {
        textures.values.forEach { it.updateAnim(delta) }
    }
}
