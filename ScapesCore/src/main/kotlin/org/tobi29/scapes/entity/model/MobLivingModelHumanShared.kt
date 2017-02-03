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

class MobLivingModelHumanShared(engine: ScapesEngine) {
    val body: Box
    val head: Box
    val legNormalLeft: Box
    val legNormalRight: Box
    val armNormalLeft: Box
    val armNormalRight: Box
    val legThinLeft: Box
    val legThinRight: Box
    val armThinLeft: Box
    val armThinRight: Box
    val bodyNoCull: Box
    val headNoCull: Box
    val legNormalLeftNoCull: Box
    val legNormalRightNoCull: Box
    val armNormalLeftNoCull: Box
    val armNormalRightNoCull: Box
    val legThinLeftNoCull: Box
    val legThinRightNoCull: Box
    val armThinLeftNoCull: Box
    val armThinRightNoCull: Box

    init {
        body = Box(engine, 0.015625f, -4f, -2f, -6f, 4f, 2f, 6f, 0f, 0f)
        head = Box(engine, 0.015625f, -4f, -4f, 0f, 4f, 4f, 8f, 0f, 32f)
        legNormalLeft = Box(engine, 0.015625f, -2f, -2f, -10f, 2f, 2f, 2f, 24f,
                0f)
        legNormalRight = Box(engine, 0.015625f, -2f, -2f, -10f, 2f, 2f, 2f, 40f,
                0f)
        armNormalLeft = Box(engine, 0.015625f, -4f, -2f, -10f, 0f, 2f, 2f, 24f,
                16f)
        armNormalRight = Box(engine, 0.015625f, 0f, -2f, -10f, 4f, 2f, 2f, 40f,
                16f)
        legThinLeft = Box(engine, 0.015625f, -1f, -1f, -10f, 1f, 1f, 2f, 24f,
                0f)
        legThinRight = Box(engine, 0.015625f, -1f, -1f, -10f, 1f, 1f, 2f, 32f,
                0f)
        armThinLeft = Box(engine, 0.015625f, -2f, -1f, -10f, 0f, 1f, 2f, 24f,
                16f)
        armThinRight = Box(engine, 0.015625f, 0f, -1f, -10f, 2f, 1f, 2f, 32f,
                16f)
        bodyNoCull = Box(engine, 0.015625f, -4f, -2f, -6f, 4f, 2f, 6f, 0f, 0f,
                false)
        headNoCull = Box(engine, 0.015625f, -4f, -4f, 0f, 4f, 4f, 8f, 0f, 32f,
                false)
        legNormalLeftNoCull = Box(engine, 0.015625f, -2f, -2f, -10f, 2f, 2f, 2f,
                24f, 0f, false)
        legNormalRightNoCull = Box(engine, 0.015625f, -2f, -2f, -10f, 2f, 2f,
                2f, 40f, 0f, false)
        armNormalLeftNoCull = Box(engine, 0.015625f, -4f, -2f, -10f, 0f, 2f, 2f,
                24f, 16f, false)
        armNormalRightNoCull = Box(engine, 0.015625f, 0f, -2f, -10f, 4f, 2f, 2f,
                40f, 16f, false)
        legThinLeftNoCull = Box(engine, 0.015625f, -1f, -1f, -10f, 1f, 1f, 2f,
                24f, 0f, false)
        legThinRightNoCull = Box(engine, 0.015625f, -1f, -1f, -10f, 1f, 1f, 2f,
                32f, 0f, false)
        armThinLeftNoCull = Box(engine, 0.015625f, -2f, -1f, -10f, 0f, 1f, 2f,
                24f, 16f, false)
        armThinRightNoCull = Box(engine, 0.015625f, 0f, -1f, -10f, 2f, 1f, 2f,
                32f, 16f, false)
    }
}
