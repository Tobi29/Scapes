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

internal fun VanillaBasics.registerMetals() {
    metal {
        id = "Tin"
        name = "Tin"
        ingotName = "Tin"
        meltingPoint = 231.0
        toolEfficiency = 0.1
        toolStrength = 2.0
        toolDamage = 0.01
        toolLevel = 10
        r = 1.0f
        g = 1.0f
        b = 1.0f
    }
    metal {
        id = "Zinc"
        name = "Zinc"
        ingotName = "Zinc"
        meltingPoint = 419.0
        toolEfficiency = 1.0
        toolStrength = 4.0
        toolDamage = 0.004
        toolLevel = 10
        r = 1.0f
        g = 0.9f
        b = 0.9f
    }
    metal {
        id = "Bismuth"
        name = "Bismuth"
        ingotName = "Bismuth"
        meltingPoint = 271.0
        toolEfficiency = 1.0
        toolStrength = 4.0
        toolDamage = 0.004
        toolLevel = 10
        r = 0.8f
        g = 0.9f
        b = 0.9f
    }
    metal {
        id = "Copper"
        name = "Copper"
        ingotName = "Copper"
        meltingPoint = 1084.0
        toolEfficiency = 6.0
        toolStrength = 8.0
        toolDamage = 0.001
        toolLevel = 20
        r = 0.8f
        g = 0.2f
        b = 0.0f
    }
    metal {
        id = "Iron"
        name = "Iron"
        ingotName = "Iron"
        meltingPoint = 1538.0
        toolEfficiency = 30.0
        toolStrength = 12.0
        toolDamage = 0.0001
        toolLevel = 40
        r = 0.7f
        g = 0.7f
        b = 0.7f
    }
    metal {
        id = "Silver"
        name = "Silver"
        ingotName = "Silver"
        meltingPoint = 961.0
        toolEfficiency = 1.0
        toolStrength = 4.0
        toolDamage = 0.004
        toolLevel = 10
        r = 0.9f
        g = 0.9f
        b = 1.0f
    }
    metal {
        id = "Gold"
        name = "Gold"
        ingotName = "Gold"
        meltingPoint = 1064.0
        toolEfficiency = 0.1
        toolStrength = 2.0
        toolDamage = 0.01
        toolLevel = 10
        r = 0.9f
        g = 0.9f
        b = 1.0f
    }
    alloy {
        id = "Bronze"
        name = "Bronze"
        ingotName = "Bronze"
        toolEfficiency = 10.0
        toolStrength = 10.0
        toolDamage = 0.0005
        toolLevel = 20
        r = 0.6f
        g = 0.4f
        b = 0.0f
        ingredients.put("Tin", 0.25)
        ingredients.put("Copper", 0.75)
    }
    alloy {
        id = "BismuthBronze"
        name = "Bismuth Bronze"
        ingotName = "Bismuth Bronze"
        toolEfficiency = 10.0
        toolStrength = 10.0
        toolDamage = 0.0005
        toolLevel = 20
        r = 0.6f
        g = 0.4f
        b = 0.0f
        ingredients.put("Bismuth", 0.2)
        ingredients.put("Zinc", 0.2)
        ingredients.put("Copper", 0.6)
    }
}
