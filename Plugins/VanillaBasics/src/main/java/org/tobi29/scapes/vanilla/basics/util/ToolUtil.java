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
    public static boolean createTool(VanillaBasics plugin, ItemStack item,
            String type) {
        switch (type) {
            case "Pickaxe":
                createTool(plugin, item, 1);
                return true;
            case "Axe":
                createTool(plugin, item, 2);
                return true;
            case "Shovel":
                createTool(plugin, item, 3);
                return true;
            case "Hammer":
                createTool(plugin, item, 4);
                return true;
            case "Saw":
                createTool(plugin, item, 5);
                return true;
            case "Hoe":
                createTool(plugin, item, 6);
                return true;
            case "Sword":
                createTool(plugin, item, 7);
                return true;
            default:
        }
        return false;
    }

    public static void createTool(VanillaBasics plugin, ItemStack item,
            int id) {
        VanillaMaterial materials = plugin.getMaterials();
        MetalType metal = plugin.getMetalType(
                item.metaData("Vanilla").getString("MetalType"));
        double efficiency = metal.baseToolEfficiency();
        double strength = metal.baseToolStrength();
        double damage = metal.baseToolDamage();
        int level = metal.baseToolLevel();
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
                efficiency = 0.0;
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
                efficiency = 0.0;
                break;
        }
        item.metaData("Vanilla").setDouble("ToolEfficiency", efficiency);
        item.metaData("Vanilla").setDouble("ToolStrength", strength);
        item.metaData("Vanilla").setDouble("ToolDamageAdd", damage);
        item.metaData("Vanilla").setInteger("ToolLevel", level);
    }
}
