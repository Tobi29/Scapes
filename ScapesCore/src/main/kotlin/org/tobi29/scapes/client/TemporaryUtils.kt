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

package org.tobi29.scapes.client

import org.tobi29.scapes.engine.graphics.GraphicsSystem
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.graphics.loadShader
import org.tobi29.scapes.engine.resource.Resource
import org.tobi29.io.asString
import org.tobi29.scapes.engine.shader.Expression
import org.tobi29.scapes.engine.shader.frontend.clike.CLikeShader
import org.tobi29.scapes.engine.shader.frontend.clike.compileCached

fun GraphicsSystem.loadShader(
        asset: String,
        properties: Map<String, Expression> = emptyMap()
): Resource<Shader> =
        loadShaderSource({
            engine.files["$asset.program"].readAsync { it.asString() }
        }, properties)

fun GraphicsSystem.loadShaderSource(
        source: suspend () -> String,
        properties: Map<String, Expression> = emptyMap()
): Resource<Shader> =
        loadShader({ CLikeShader.compileCached(source()) }, properties)
