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

package org.tobi29.scapes.chunk

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.utils.graphics.Cam

interface WorldSkybox {
    fun update(delta: Double)

    fun init(gl: GL)

    fun renderUpdate(cam: Cam,
                     delta: Double)

    fun render(gl: GL,
               cam: Cam)

    fun dispose(gl: GL)

    fun exposure(): Float

    fun fogR(): Float

    fun fogG(): Float

    fun fogB(): Float

    fun fogDistance(): Float
}
