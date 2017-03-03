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

package org.tobi29.scapes.vanilla.basics

import org.tobi29.scapes.vanilla.basics.material.CropType

class VanillaBasicsCrops(reg: (String, (Int) -> CropType) -> CropType) {
    private val ROOT = "VanillaBasics:image/terrain/crops"

    val WHEAT = reg("vanilla.basics.crop.Wheat") {
        CropType(it, "Wheat", "Bread", ROOT, 4000.0, 0)
    }
}

