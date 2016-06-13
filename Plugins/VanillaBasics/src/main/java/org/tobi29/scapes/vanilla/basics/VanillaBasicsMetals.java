package org.tobi29.scapes.vanilla.basics;

class VanillaBasicsMetals {
    static void registerMetals(VanillaBasics plugin) {
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Tin";
            m.meltingPoint = 231.0;
            m.toolEfficiency = 0.1;
            m.toolStrength = 2.0;
            m.toolDamage = 0.01;
            m.toolLevel = 10;
            m.r = 1.0f;
            m.g = 1.0f;
            m.b = 1.0f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Zinc";
            m.meltingPoint = 419.0;
            m.toolEfficiency = 1.0;
            m.toolStrength = 4.0;
            m.toolDamage = 0.004;
            m.toolLevel = 10;
            m.r = 1.0f;
            m.g = 0.9f;
            m.b = 0.9f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Bismuth";
            m.meltingPoint = 271.0;
            m.toolEfficiency = 1.0;
            m.toolStrength = 4.0;
            m.toolDamage = 0.004;
            m.toolLevel = 10;
            m.r = 0.8f;
            m.g = 0.9f;
            m.b = 0.9f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Copper";
            m.meltingPoint = 1084.0;
            m.toolEfficiency = 6.0;
            m.toolStrength = 8.0;
            m.toolDamage = 0.001;
            m.toolLevel = 20;
            m.r = 0.8f;
            m.g = 0.2f;
            m.b = 0.0f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Iron";
            m.meltingPoint = 1538.0;
            m.toolEfficiency = 30.0;
            m.toolStrength = 12.0;
            m.toolDamage = 0.0001;
            m.toolLevel = 40;
            m.r = 0.7f;
            m.g = 0.7f;
            m.b = 0.7f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Silver";
            m.meltingPoint = 961.0;
            m.toolEfficiency = 1.0;
            m.toolStrength = 4.0;
            m.toolDamage = 0.004;
            m.toolLevel = 10;
            m.r = 0.9f;
            m.g = 0.9f;
            m.b = 1.0f;
        });
        plugin.c.metal(m -> {
            m.id = m.name = m.ingotName = "Gold";
            m.meltingPoint = 1064.0;
            m.toolEfficiency = 0.1;
            m.toolStrength = 2.0;
            m.toolDamage = 0.01;
            m.toolLevel = 10;
            m.r = 0.9f;
            m.g = 0.9f;
            m.b = 1.0f;
        });
        plugin.c.alloy(m -> {
            m.id = m.name = m.ingotName = "Bronze";
            m.toolEfficiency = 10.0;
            m.toolStrength = 10.0;
            m.toolDamage = 0.0005;
            m.toolLevel = 20;
            m.r = 0.6f;
            m.g = 0.4f;
            m.b = 0.0f;
            m.ingredients.put("Tin", 0.25);
            m.ingredients.put("Copper", 0.75);
        });
        plugin.c.alloy(m -> {
            m.name = m.ingotName = "Bismuth Bronze";
            m.id = "BismuthBronze";
            m.toolEfficiency = 10.0;
            m.toolStrength = 10.0;
            m.toolDamage = 0.0005;
            m.toolLevel = 20;
            m.r = 0.6f;
            m.g = 0.4f;
            m.b = 0.0f;
            m.ingredients.put("Bismuth", 0.2);
            m.ingredients.put("Zinc", 0.2);
            m.ingredients.put("Copper", 0.6);
        });
    }
}
