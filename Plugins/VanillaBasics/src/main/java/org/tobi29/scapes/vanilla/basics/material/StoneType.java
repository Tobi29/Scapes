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

public class StoneType {
    private static final Pattern REPLACE = Pattern.compile(" ");
    private static final String ROOT = "VanillaBasics:image/terrain/stone";
    public static final StoneType DIRT_STONE =
            new StoneType("Dirt Stone", ROOT, 0.1);
    public static final StoneType FLINT = new StoneType("Flint", ROOT, 2.1);
    public static final StoneType CHALK = new StoneType("Chalk", ROOT, 0.4);
    public static final StoneType CHERT = new StoneType("Chert", ROOT, 0.4);
    public static final StoneType CLAYSTONE =
            new StoneType("Claystone", ROOT, 0.4);
    public static final StoneType CONGLOMERATE =
            new StoneType("Conglomerate", ROOT, 0.4);
    public static final StoneType MARBLE = new StoneType("Marble", ROOT, 0.6);
    public static final StoneType ANDESITE =
            new StoneType("Andesite", ROOT, 1.4);
    public static final StoneType BASALT = new StoneType("Basalt", ROOT, 1.4);
    public static final StoneType DACITE = new StoneType("Dacite", ROOT, 1.4);
    public static final StoneType RHYOLITE =
            new StoneType("Rhyolite", ROOT, 1.4);
    public static final StoneType DIORITE = new StoneType("Diorite", ROOT, 1.2);
    public static final StoneType GABBRO = new StoneType("Gabbro", ROOT, 1.3);
    public static final StoneType GRANITE = new StoneType("Granite", ROOT, 1.5);
    private final String name, texture, textureRoot;
    private final double resistance;

    public StoneType(String name, String textureRoot, double resistance) {
        this.name = name;
        texture = REPLACE.matcher(name).replaceAll("");
        this.textureRoot = textureRoot;
        this.resistance = resistance;
    }

    public static StoneType get(GameRegistry registry, int data) {
        return registry.<StoneType>get("VanillaBasics", "StoneType").get(data);
    }

    public String name() {
        return name;
    }

    public String texture() {
        return texture;
    }

    public String textureRoot() {
        return textureRoot;
    }

    public double resistance() {
        return resistance;
    }

    public int data(GameRegistry registry) {
        return registry.get("VanillaBasics", "StoneType").get(this);
    }
}
