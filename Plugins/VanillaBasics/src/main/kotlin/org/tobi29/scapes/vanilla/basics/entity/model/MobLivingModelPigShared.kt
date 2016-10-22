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

package org.tobi29.scapes.vanilla.basics.entity.model

import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.entity.model.Box

class MobLivingModelPigShared(engine: ScapesEngine) {
    val body: Box
    val head: Box
    val legFrontLeft: Box
    val legFrontRight: Box
    val legBackLeft: Box
    val legBackRight: Box

    init {
        body = Box(engine, 0.015625f, -5f, -6f, -5f, 5f, 6f, 5f, 0f, 0f)
        head = Box(engine, 0.015625f, -4f, 0f, -5f, 4f, 8f, 4f, 0f, 22f)
        legFrontLeft = Box(engine, 0.015625f, -2f, -2f, -6f, 2f, 2f, 0f, 44f, 0f)
        legFrontRight = Box(engine, 0.015625f, -2f, -2f, -6f, 2f, 2f, 0f, 44f, 10f)
        legBackLeft = Box(engine, 0.015625f, -2f, -2f, -6f, 2f, 2f, 0f, 44f, 20f)
        legBackRight = Box(engine, 0.015625f, -2f, -2f, -6f, 2f, 2f, 0f, 44f, 30f)
    }
}
