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

package org.tobi29.scapes.entity.model

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.RenderType
import org.tobi29.scapes.engine.graphics.Texture
import org.tobi29.scapes.engine.graphics.createVTNI

class EntityModelBlockBreakShared(engine: ScapesEngine,
                                  val textures: Array<Texture>) {
    val model = engine.graphics.createVTNI(
            floatArrayOf(-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f,
                    0.5f, -0.5f, 0.5f, 0.5f),
            floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f),
            floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 1.0f), intArrayOf(0, 1, 2, 0, 2, 3),
            RenderType.TRIANGLES)
}
