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

import org.tobi29.scapes.block.GameRegistry;

import java.util.regex.Pattern;

public class CropType {
    private static final Pattern REPLACE = Pattern.compile(" ");
    private static final String ROOT = "VanillaBasics:image/terrain/crops";
    public static final CropType WHEAT =
            new CropType("Wheat", "Bread", ROOT, 10000.0, 0);
    private final String name, bakedName, texture;
    private final double time;
    private final int nutrient;

    public CropType(String name, String bakedName, String textureRoot,
            double time, int nutrient) {
        this.name = name;
        this.bakedName = bakedName;
        texture = textureRoot + '/' +
                REPLACE.matcher(name).replaceAll("").toLowerCase();
        this.time = time;
        this.nutrient = nutrient;
    }

    public String getName() {
        return name;
    }

    public String getBakedName() {
        return bakedName;
    }

    public String getTexture() {
        return texture;
    }

    public double getTime() {
        return time;
    }

    public int getNutrient() {
        return nutrient;
    }

    public int getData(GameRegistry registry) {
        return registry.get("VanillaBasics", "CropType").get(this);
    }
}
