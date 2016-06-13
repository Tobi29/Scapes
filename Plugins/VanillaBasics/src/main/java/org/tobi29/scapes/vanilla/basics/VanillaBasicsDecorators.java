package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerGround;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerPatch;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerRock;
import org.tobi29.scapes.vanilla.basics.generator.decorator.LayerTree;
import org.tobi29.scapes.vanilla.basics.generator.tree.*;
import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

class VanillaBasicsDecorators {
    @SuppressWarnings("CodeBlock2Expr")
    static void registerDecorators(VanillaBasics plugin) {
        VanillaMaterial m = plugin.getMaterials();
        // Overlays
        plugin.c.decorator("Rocks", d -> {
            d.addLayer(new LayerRock(m.stoneRock, m.stoneRaw, 256,
                    (terrain, x, y, z) ->
                            terrain.type(x, y, z - 1) == m.grass &&
                                    terrain.type(x, y, z) == m.air));
        });
        plugin.c.decorator("Flint", d -> {
            int data = StoneType.FLINT.data(m.registry);
            d.addLayer(new LayerGround(m.stoneRock,
                    (terrain, x, y, z, random) -> data, 1024,
                    (terrain, x, y, z) ->
                            terrain.type(x, y, z - 1) == m.grass &&
                                    terrain.type(x, y, z) == m.air));
        });
        plugin.c.decorator("Gravel", d -> {
            int data = StoneType.DIRT_STONE.data(m.registry);
            int flintData = StoneType.FLINT.data(m.registry);
            d.addLayer(new LayerRock(m.stoneRock, m.stoneRaw, 8,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) == m.sand &&
                            terrain.data(x, y, z - 1) == 1 &&
                            terrain.type(x, y, z) == m.air));
            d.addLayer(new LayerGround(m.stoneRock,
                    (terrain, x, y, z, random) -> data, 4,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) == m.sand &&
                            terrain.data(x, y, z - 1) == 1 &&
                            terrain.type(x, y, z) == m.air));
            d.addLayer(new LayerGround(m.stoneRock,
                    (terrain, x, y, z, random) -> flintData, 12,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) == m.sand &&
                            terrain.data(x, y, z - 1) == 1 &&
                            terrain.type(x, y, z) == m.air));
        });

        // Polar
        plugin.c.decorator(BiomeGenerator.Biome.POLAR, "Waste", 10, d -> {
        });

        // Tundra
        plugin.c.decorator(BiomeGenerator.Biome.TUNDRA, "Waste", 10, d -> {
        });
        plugin.c.decorator(BiomeGenerator.Biome.TUNDRA, "Spruce", 1, d -> {
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 256));
        });

        // Taiga
        plugin.c.decorator(BiomeGenerator.Biome.TAIGA, "Spruce", 10, d -> {
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 32));
        });
        plugin.c.decorator(BiomeGenerator.Biome.TAIGA, "Sequoia", 2, d -> {
            d.addLayer(new LayerTree(TreeSequoia.INSTANCE, 256));
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 128));
        });

        // Wasteland
        plugin.c.decorator(BiomeGenerator.Biome.WASTELAND, "Waste", 10, d -> {
        });

        // Steppe
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "Waste", 10, d -> {
        });
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "Birch", 5, d -> {
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 1024));
            d.addLayer(new LayerPatch(m.bush, 0, 16, 64, 1 << 16,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "Spruce", 10, d -> {
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 512));
            d.addLayer(new LayerPatch(m.bush, 0, 16, 64, 1 << 17,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });

        // Forest
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Deciduous", 10, d -> {
            d.addLayer(new LayerTree(TreeOak.INSTANCE, 64));
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 128));
            d.addLayer(new LayerTree(TreeMaple.INSTANCE, 32));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 14,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
            d.addLayer(new LayerPatch(m.bush, 0, 6, 8, 2048,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Birch", 10, d -> {
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 64));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 15,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
            d.addLayer(new LayerPatch(m.bush, 0, 16, 64, 1 << 13,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Spruce", 10, d -> {
            d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 32));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 14,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
            d.addLayer(new LayerPatch(m.bush, 0, 4, 8, 4096,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Willow", 10, d -> {
            d.addLayer(new LayerTree(TreeWillow.INSTANCE, 64));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 14,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
        });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "Sequoia", 2, d -> {
            d.addLayer(new LayerTree(TreeSequoia.INSTANCE, 256));
        });

        // Desert
        plugin.c.decorator(BiomeGenerator.Biome.DESERT, "Waste", 10, d -> {
        });

        // Waste
        plugin.c.decorator(BiomeGenerator.Biome.SAVANNA, "Waste", 10, d -> {
        });

        // Oasis
        plugin.c.decorator(BiomeGenerator.Biome.OASIS, "Palm", 10, d -> {
            d.addLayer(new LayerTree(TreePalm.INSTANCE, 128));
        });

        // Rainforest
        plugin.c.decorator(BiomeGenerator.Biome.RAINFOREST, "Deciduous", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeOak.INSTANCE, 64));
                    d.addLayer(new LayerTree(TreeBirch.INSTANCE, 96));
                    d.addLayer(new LayerTree(TreePalm.INSTANCE, 256));
                    d.addLayer(new LayerTree(TreeMaple.INSTANCE, 32));
                    for (int i = 0; i < 20; i++) {
                        d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 13,
                                (terrain, x, y, z) ->
                                        terrain.type(x, y, z - 1) == m.grass));
                    }
                    d.addLayer(new LayerPatch(m.bush, 0, 4, 8, 1024,
                            (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                    m.grass));
                });
        plugin.c.decorator(BiomeGenerator.Biome.RAINFOREST, "Willow", 4, d -> {
            d.addLayer(new LayerTree(TreeWillow.INSTANCE, 48));
            for (int i = 0; i < 20; i++) {
                d.addLayer(new LayerPatch(m.flower, i, 16, 24, 1 << 13,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
            }
            d.addLayer(new LayerPatch(m.bush, 0, 4, 8, 1024,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        });
    }
}
