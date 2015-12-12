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
import org.tobi29.scapes.vanilla.basics.material.AlloyType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemIngot;

public class ToolUtil {
    public static boolean createTool(VanillaBasics plugin, ItemStack item,
            String type) {
        return createTool(plugin, item, id(type));
    }

    public static boolean createTool(VanillaBasics plugin, ItemStack item,
            int id) {
        VanillaMaterial materials = plugin.getMaterials();
        ItemIngot ingot = (ItemIngot) item.material();
        MetalUtil.Alloy alloy = ingot.alloy(item);
        AlloyType alloyType = alloy.type(plugin);
        double efficiency = alloyType.baseToolEfficiency();
        double strength = alloyType.baseToolStrength();
        double damage = alloyType.baseToolDamage();
        int level = alloyType.baseToolLevel();
        switch (id) {
            case 1:
                item.setMaterial(materials.metalPickaxe);
                strength *= 0.1;
                break;
            case 2:
                item.setMaterial(materials.metalAxe);
                strength *= 1.5;
                break;
            case 3:
                item.setMaterial(materials.metalShovel);
                strength *= 0.1;
                break;
            case 4:
                item.setMaterial(materials.metalHammer);
                efficiency = 0.0;
                strength *= 0.4;
                break;
            case 5:
                item.setMaterial(materials.metalSaw);
                strength *= 0.1;
                break;
            case 6:
                item.setMaterial(materials.metalHoe);
                strength *= 0.1;
                break;
            case 7:
                item.setMaterial(materials.metalSword);
                efficiency = 0.0;
                break;
            default:
                return false;
        }
        item.metaData("Vanilla").setDouble("ToolEfficiency", efficiency);
        item.metaData("Vanilla").setDouble("ToolStrength", strength);
        item.metaData("Vanilla").setDouble("ToolDamageAdd", damage);
        item.metaData("Vanilla").setInteger("ToolLevel", level);
        return true;
    }

    public static boolean createStoneTool(VanillaBasics plugin, ItemStack item,
            String type) {
        return createStoneTool(plugin, item, id(type));
    }

    public static boolean createStoneTool(VanillaBasics plugin, ItemStack item,
            int id) {
        VanillaMaterial materials = plugin.getMaterials();
        double efficiency = 1.0;
        double strength = 4.0;
        double damage = 0.004;
        int level = 10;
        switch (id) {
            case 1:
                item.setMaterial(materials.stonePickaxe);
                strength *= 0.1;
                break;
            case 2:
                item.setMaterial(materials.stoneAxe);
                strength *= 1.5;
                break;
            case 3:
                item.setMaterial(materials.stoneShovel);
                strength *= 0.1;
                break;
            case 4:
                item.setMaterial(materials.stoneHammer);
                efficiency = 0.0;
                strength *= 0.4;
                break;
            case 5:
                item.setMaterial(materials.stoneSaw);
                strength *= 0.1;
                break;
            case 6:
                item.setMaterial(materials.stoneHoe);
                strength *= 0.1;
                break;
            case 7:
                item.setMaterial(materials.stoneSword);
                efficiency = 0.0;
                break;
            default:
                return false;
        }
        item.metaData("Vanilla").setDouble("ToolEfficiency", efficiency);
        item.metaData("Vanilla").setDouble("ToolStrength", strength);
        item.metaData("Vanilla").setDouble("ToolDamageAdd", damage);
        item.metaData("Vanilla").setInteger("ToolLevel", level);
        return true;
    }

    public static int id(String type) {
        switch (type) {
            case "Pickaxe":
                return 1;
            case "Axe":
                return 2;
            case "Shovel":
                return 3;
            case "Hammer":
                return 4;
            case "Saw":
                return 5;
            case "Hoe":
                return 6;
            case "Sword":
                return 7;
            default:
                return -1;
        }
    }
}
