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

class AlloyType(private val id: String,
                private val name: String,
                private val ingotName: String,
                private val ingredients: Map<MetalType, Double>,
                private val r: Float,
                private val g: Float,
                private val b: Float,
                private val toolEfficiency: Double,
                private val toolStrength: Double,
                private val toolDamage: Double,
                private val toolLevel: Int) {

    fun id(): String {
        return id
    }

    fun name(): String {
        return name
    }

    fun ingotName(): String {
        return ingotName
    }

    fun ingredients(): Map<MetalType, Double> {
        return ingredients
    }

    fun r(): Float {
        return r
    }

    fun g(): Float {
        return g
    }

    fun b(): Float {
        return b
    }

    fun baseToolEfficiency(): Double {
        return toolEfficiency
    }

    fun baseToolStrength(): Double {
        return toolStrength
    }

    fun baseToolDamage(): Double {
        return toolDamage
    }

    fun baseToolLevel(): Int {
        return toolLevel
    }
}
