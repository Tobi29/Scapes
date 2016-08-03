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

package org.tobi29.scapes.vanilla.basics.material;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3f;
import org.tobi29.scapes.vanilla.basics.generator.tree.*;

import java.util.regex.Pattern;

public class TreeType {
    private static final Pattern REPLACE = Pattern.compile(" ");
    private static final String ROOT = "VanillaBasics:image/terrain/tree";
    public static final TreeType OAK =
            new TreeType("Oak", ROOT, new Vector3f(0.5f, 0.9f, 0.4f),
                    new Vector3f(0.5f, 0.8f, 0.0f),
                    new Vector3f(1.0f, 0.7f, 0.0f), 80, new TreeOak());
    public static final TreeType BIRCH =
            new TreeType("Birch", ROOT, new Vector3f(0.6f, 0.9f, 0.5f),
                    new Vector3f(0.6f, 0.8f, 0.1f),
                    new Vector3f(1.0f, 0.8f, 0.0f), 20, new TreeBirch());
    public static final TreeType SPRUCE =
            new TreeType("Spruce", ROOT, new Vector3f(0.2f, 0.5f, 0.2f),
                    new Vector3f(0.2f, 0.5f, 0.0f), 10, new TreeSpruce());
    public static final TreeType PALM =
            new TreeType("Palm", ROOT, new Vector3f(0.5f, 0.9f, 0.4f),
                    new Vector3f(0.5f, 0.8f, 0.0f), 3, new TreePalm());
    public static final TreeType MAPLE =
            new TreeType("Maple", ROOT, new Vector3f(0.5f, 0.9f, 0.4f),
                    new Vector3f(0.5f, 0.8f, 0.0f),
                    new Vector3f(1.0f, 0.4f, 0.0f), 70, new TreeMaple());
    public static final TreeType SEQUOIA =
            new TreeType("Sequoia", ROOT, new Vector3f(0.2f, 0.5f, 0.2f),
                    new Vector3f(0.2f, 0.5f, 0.0f), 200, new TreeSequoia());
    public static final TreeType WILLOW =
            new TreeType("Willow", ROOT, new Vector3f(0.5f, 0.9f, 0.4f),
                    new Vector3f(0.5f, 0.8f, 0.0f),
                    new Vector3f(0.9f, 0.7f, 0.0f), 100, new TreeWillow());
    private final String name, texture;
    private final Vector3 colorCold, colorWarm, colorAutumn;
    private final int dropChance;
    private final Tree generator;
    private final boolean evergreen;

    public TreeType(String name, String textureRoot, Vector3 colorCold,
            Vector3 colorWarm, int dropChance, Tree generator) {
        this(name, textureRoot, colorCold, colorWarm, Vector3f.ZERO, dropChance,
                generator, true);
    }

    public TreeType(String name, String textureRoot, Vector3 colorCold,
            Vector3 colorWarm, Vector3 colorAutumn, int dropChance,
            Tree generator) {
        this(name, textureRoot, colorCold, colorWarm, colorAutumn, dropChance,
                generator, false);
    }

    TreeType(String name, String textureRoot, Vector3 colorCold,
            Vector3 colorWarm, Vector3 colorAutumn, int dropChance,
            Tree generator, boolean evergreen) {
        this.name = name;
        texture = textureRoot + '/' +
                REPLACE.matcher(name).replaceAll("").toLowerCase();
        this.colorCold = colorCold;
        this.colorWarm = colorWarm;
        this.colorAutumn = colorAutumn;
        this.dropChance = dropChance;
        this.generator = generator;
        this.evergreen = evergreen;
    }

    public static TreeType get(GameRegistry registry, int data) {
        return registry.<TreeType>get("VanillaBasics", "TreeType").get(data);
    }

    public String name() {
        return name;
    }

    public String texture() {
        return texture;
    }

    public Vector3 colorCold() {
        return colorCold;
    }

    public Vector3 colorWarm() {
        return colorWarm;
    }

    public Vector3 colorAutumn() {
        return colorAutumn;
    }

    public int dropChance() {
        return dropChance;
    }

    public Tree generator() {
        return generator;
    }

    public boolean isEvergreen() {
        return evergreen;
    }

    public int data(GameRegistry registry) {
        return registry.get("VanillaBasics", "TreeType").get(this);
    }
}
