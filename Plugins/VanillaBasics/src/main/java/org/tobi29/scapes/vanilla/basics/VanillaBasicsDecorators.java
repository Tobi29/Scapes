package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator;
import org.tobi29.scapes.vanilla.basics.generator.decorator.*;
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
        plugin.c.decorator(BiomeGenerator.Biome.TUNDRA, "SprucePlains", 1,
                d -> {
                    d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 256));
                });

        // Taiga
        plugin.c.decorator(BiomeGenerator.Biome.TAIGA, "SprucePlains", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 64));
                });
        plugin.c.decorator(BiomeGenerator.Biome.TAIGA, "SpruceForest", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 16));
                });
        plugin.c.decorator(BiomeGenerator.Biome.TAIGA, "SequoiaForest", 2,
                d -> {
                    d.addLayer(new LayerTree(TreeSequoia.INSTANCE, 256));
                    d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 128));
                });

        // Wasteland
        plugin.c.decorator(BiomeGenerator.Biome.WASTELAND, "Waste", 10, d -> {
        });
        plugin.c.decorator(BiomeGenerator.Biome.WASTELAND, "Shrubland", 4,
                d -> {
                    shrubs(d, m, 64);
                });

        // Steppe
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "Waste", 3, d -> {
        });
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "BirchPlains", 5, d -> {
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 4096));
            shrubs(d, m, 128);
        });
        plugin.c.decorator(BiomeGenerator.Biome.STEPPE, "SprucePlains", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 2048));
                    shrubs(d, m, 128);
                });

        // Plains
        plugin.c.decorator(BiomeGenerator.Biome.PLAINS, "Waste", 1, d -> {
        });
        plugin.c.decorator(BiomeGenerator.Biome.PLAINS, "Plains", 10, d -> {
            d.addLayer(new LayerTree(TreeOak.INSTANCE, 8192));
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 8192));
            d.addLayer(new LayerTree(TreeMaple.INSTANCE, 8192));
            shrubs(d, m, 256);
        });
        plugin.c.decorator(BiomeGenerator.Biome.PLAINS, "BirchPlains", 5, d -> {
            d.addLayer(new LayerTree(TreeBirch.INSTANCE, 1024));
            shrubs(d, m, 256);
        });
        plugin.c.decorator(BiomeGenerator.Biome.PLAINS, "SprucePlains", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 512));
                    shrubs(d, m, 256);
                });
        plugin.c.decorator(BiomeGenerator.Biome.PLAINS, "DeciduousForest", 4,
                d -> {
                    d.addLayer(new LayerTree(TreeOak.INSTANCE, 256));
                    d.addLayer(new LayerTree(TreeBirch.INSTANCE, 512));
                    d.addLayer(new LayerTree(TreeMaple.INSTANCE, 512));
                    shrubs(d, m, 256);
                });

        // Forest
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "DeciduousForest", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeOak.INSTANCE, 96));
                    d.addLayer(new LayerTree(TreeBirch.INSTANCE, 128));
                    d.addLayer(new LayerTree(TreeMaple.INSTANCE, 96));
                    shrubs(d, m, 128);
                });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "BirchForest", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeBirch.INSTANCE, 48));
                    shrubs(d, m, 128);
                });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "SpruceForest", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeSpruce.INSTANCE, 16));
                    shrubs(d, m, 128);
                });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "WillowForest", 10,
                d -> {
                    d.addLayer(new LayerTree(TreeWillow.INSTANCE, 64));
                    shrubs(d, m, 128);
                });
        plugin.c.decorator(BiomeGenerator.Biome.FOREST, "SequoiaForest", 2,
                d -> {
                    d.addLayer(new LayerTree(TreeSequoia.INSTANCE, 256));
                    shrubs(d, m, 64);
                });

        // Desert
        plugin.c.decorator(BiomeGenerator.Biome.DESERT, "Waste", 10, d -> {
        });

        // Xeric Shrubland
        plugin.c.decorator(BiomeGenerator.Biome.XERIC_SHRUBLAND, "Waste", 4,
                d -> {
                });
        plugin.c.decorator(BiomeGenerator.Biome.XERIC_SHRUBLAND, "Shrubland",
                10, d -> {
                    shrubs(d, m, 64);
                });

        // Dry Savanna
        plugin.c.decorator(BiomeGenerator.Biome.DRY_SAVANNA, "Waste", 1, d -> {
        });
        plugin.c.decorator(BiomeGenerator.Biome.DRY_SAVANNA, "Shrubland", 10,
                d -> {
                    shrubs(d, m, 256);
                });

        // Wet Savanna
        plugin.c.decorator(BiomeGenerator.Biome.WET_SAVANNA, "DeciduousForest",
                10, d -> {
                    d.addLayer(new LayerTree(TreeOak.INSTANCE, 256));
                    d.addLayer(new LayerTree(TreeBirch.INSTANCE, 512));
                    d.addLayer(new LayerTree(TreePalm.INSTANCE, 512));
                    shrubs(d, m, 128);
                });

        // Oasis
        plugin.c.decorator(BiomeGenerator.Biome.OASIS, "PalmForest", 10, d -> {
            d.addLayer(new LayerTree(TreePalm.INSTANCE, 128));
            shrubs(d, m, 512);
        });

        // Rainforest
        plugin.c.decorator(BiomeGenerator.Biome.RAINFOREST, "DeciduousForest",
                10, d -> {
                    d.addLayer(new LayerTree(TreeOak.INSTANCE, 48));
                    d.addLayer(new LayerTree(TreeBirch.INSTANCE, 96));
                    d.addLayer(new LayerTree(TreePalm.INSTANCE, 128));
                    shrubs(d, m, 256);
                });
        plugin.c.decorator(BiomeGenerator.Biome.RAINFOREST, "WillowForest", 4,
                d -> {
                    d.addLayer(new LayerTree(TreeWillow.INSTANCE, 24));
                    d.addLayer(new LayerTree(TreePalm.INSTANCE, 256));
                    shrubs(d, m, 256);
                });
    }

    public static void shrubs(BiomeDecorator d, VanillaMaterial m,
            int density) {
        for (int i = 0; i < 20; i++) {
            d.addLayer(new LayerPatch(m.flower, i, 16, density >> 4,
                    (1 << 23) / density,
                    (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                            m.grass));
        }
        d.addLayer(
                new LayerPatch(m.bush, 0, 16, density >> 3, (1 << 18) / density,
                        (terrain, x, y, z) -> terrain.type(x, y, z - 1) ==
                                m.grass));
    }
}
