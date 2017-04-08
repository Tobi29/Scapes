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

package org.tobi29.scapes.vanilla.basics.material

import org.tobi29.scapes.engine.utils.readOnly
import org.tobi29.scapes.vanilla.basics.util.Alloy

class AlloyType(val id: String,
                val name: String,
                val ingotName: String,
                ingredients: Map<MetalType, Double>,
                val r: Double,
                val g: Double,
                val b: Double,
                val toolEfficiency: Double,
                val toolStrength: Double,
                val toolDamage: Double,
                val toolLevel: Int) {
    val ingredients = ingredients.readOnly()
}

fun Alloy.add(type: AlloyType,
              amount: Double = 1.0) {
    type.ingredients.forEach { type, ratio ->
        add(type, ratio * amount)
    }
}
