/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.vanilla.basics.material;

import java.util.Map;

public class MetalType {
    private final Map<String, Double> ingredients;
    private final String id, name, ingotName;
    private final float meltingPoint, r, g, b;
    private final double toolEfficiency, toolStrength, toolDamage;
    private final int toolLevel;

    public MetalType(String id, String name, Map<String, Double> ingredients,
            float r, float g, float b, float meltingPoint,
            double toolEfficiency, double toolStrength, double toolDamage,
            int toolLevel) {
        this(id, name, name, ingredients, r, g, b, meltingPoint, toolEfficiency,
                toolStrength, toolDamage, toolLevel);
    }

    public MetalType(String id, String name, String ingotName,
            Map<String, Double> ingredients, float r, float g, float b,
            float meltingPoint, double toolEfficiency, double toolStrength,
            double toolDamage, int toolLevel) {
        this.id = id;
        this.name = name;
        this.ingotName = ingotName;
        this.ingredients = ingredients;
        this.r = r;
        this.g = g;
        this.b = b;
        this.meltingPoint = meltingPoint;
        this.toolEfficiency = toolEfficiency;
        this.toolStrength = toolStrength;
        this.toolDamage = toolDamage;
        this.toolLevel = toolLevel;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String ingotName() {
        return ingotName;
    }

    public Map<String, Double> ingredients() {
        return ingredients;
    }

    public float r() {
        return r;
    }

    public float g() {
        return g;
    }

    public float b() {
        return b;
    }

    public float meltingPoint() {
        return meltingPoint;
    }

    public double baseToolEfficiency() {
        return toolEfficiency;
    }

    public double baseToolStrength() {
        return toolStrength;
    }

    public double baseToolDamage() {
        return toolDamage;
    }

    public int baseToolLevel() {
        return toolLevel;
    }
}
