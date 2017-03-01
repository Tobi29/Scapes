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

package org.tobi29.scapes.vanilla.basics.entity.model

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.entity.model.Box

class EntityModelBellowsShared(engine: ScapesEngine) {
    val side: Box
    val middle: Box
    val pipe: Box

    init {
        side = Box(engine, 0.0625f, -7f, -7f, -1f, 7f, 7f, 1f, 0f, 0f)
        middle = Box(engine, 0.0625f, -6f, -6f, -7f, 6f, 6f, 7f, 0f, 0f)
        pipe = Box(engine, 0.0625f, -2f, -2f, -16f, 2f, 2f, 0f, 0f, 0f)
    }
}