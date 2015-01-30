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

package org.tobi29.scapes.vanilla.basics.util;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.MetalType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class ToolUtil {
    public static void createTool(VanillaBasics plugin, ItemStack item,
            String type) {
        switch (type) {
            case "Pickaxe":
                createTool(plugin, item, 1);
                break;
            case "Axe":
                createTool(plugin, item, 2);
                break;
            case "Shovel":
                createTool(plugin, item, 3);
                break;
            case "Hammer":
                createTool(plugin, item, 4);
                break;
            case "Saw":
                createTool(plugin, item, 5);
                break;
            case "Hoe":
                createTool(plugin, item, 6);
                break;
            case "Sword":
                createTool(plugin, item, 7);
                break;
            default:
                throw new IllegalArgumentException("Unknown tool type!");
        }
    }

    public static void createTool(VanillaBasics plugin, ItemStack item,
            int id) {
        VanillaMaterial materials = plugin.getMaterials();
        MetalType metal = plugin.getMetalType(
                item.getMetaData("Vanilla").getString("MetalType"));
        double efficiency = metal.getBaseToolEfficiency();
        double strength = metal.getBaseToolStrength();
        double damage = metal.getBaseToolDamage();
        int level = metal.getBaseToolLevel();
        switch (id) {
            case 1:
                item.setMaterial(materials.pickaxe);
                strength *= 0.1;
                break;
            case 2:
                item.setMaterial(materials.axe);
                strength *= 1.5;
                break;
            case 3:
                item.setMaterial(materials.shovel);
                strength *= 0.1;
                break;
            case 4:
                item.setMaterial(materials.hammer);
                efficiency = 0.0d;
                strength *= 0.4;
                break;
            case 5:
                item.setMaterial(materials.saw);
                strength *= 0.1;
                break;
            case 6:
                item.setMaterial(materials.hoe);
                strength *= 0.1;
                break;
            case 7:
                item.setMaterial(materials.sword);
                efficiency = 0.0d;
                break;
        }
        item.getMetaData("Vanilla").setDouble("ToolEfficiency", efficiency);
        item.getMetaData("Vanilla").setDouble("ToolStrength", strength);
        item.getMetaData("Vanilla").setDouble("ToolDamageAdd", damage);
        item.getMetaData("Vanilla").setInteger("ToolLevel", level);
    }
}
