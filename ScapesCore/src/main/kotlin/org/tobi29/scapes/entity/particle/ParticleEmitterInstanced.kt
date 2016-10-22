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

package org.tobi29.scapes.entity.particle

import org.tobi29.scapes.engine.graphics.*
import org.tobi29.scapes.engine.utils.graphics.Cam
import java.nio.ByteBuffer

abstract class ParticleEmitterInstanced<P : ParticleInstance> protected constructor(system: ParticleSystem, protected val texture: Texture,
                                                                                    attributes: List<ModelAttribute>, length: Int,
                                                                                    attributesStream: List<ModelAttribute>, renderType: RenderType,
                                                                                    instances: Array<P>) : ParticleEmitter<P>(
        system, instances) {
    protected val vao: ModelHybrid
    protected val buffer: ByteBuffer

    init {
        vao = system.world.game.engine.graphics.createModelHybrid(attributes,
                length, attributesStream, 0,
                renderType)
        buffer = system.world.game.engine.allocate(
                vao.strideStream() * maxInstances)
    }

    override fun render(gl: GL,
                        cam: Cam) {
        if (!hasAlive) {
            return
        }
        texture.bind(gl)
        val shader = prepareShader(gl, cam)
        buffer.clear()
        vao.ensureStored(gl)
        val count = prepareBuffer(cam)
        if (count > 0) {
            buffer.flip()
            vao.bufferStream(gl, buffer)
            vao.renderInstanced(gl, shader, 6, count)
        }
    }

    protected abstract fun prepareShader(gl: GL,
                                         cam: Cam): Shader

    protected abstract fun prepareBuffer(cam: Cam): Int
}
