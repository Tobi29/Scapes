/*
 * Copyright 2012-2017 Tobi29
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
package org.tobi29.scapes.vanilla.basics

import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.world.decorator.*
import org.tobi29.scapes.vanilla.basics.world.tree.*
import org.tobi29.scapes.vanilla.basics.material.StoneType
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial

internal fun VanillaBasics.registerDecorators() {
    // Overlays
    decorator("Rocks") {
        addLayer(LayerRock(materials.stoneRock, materials.stoneRaw, 256, {
            terrain, x, y, z ->
            terrain.type(x, y, z - 1) === materials.grass && terrain.type(x, y,
                    z) === materials.air
        }))
    }
    decorator("Flint") {
        val data = StoneType.FLINT.data(materials.registry)
        addLayer(LayerGround(materials.stoneRock,
                { terrain, x, y, z, random -> data }, 1024, {
            terrain, x, y, z ->
            terrain.type(x, y, z - 1) === materials.grass && terrain.type(x, y,
                    z) === materials.air
        }))
    }
    decorator("Gravel") {
        val data = StoneType.DIRT_STONE.data(materials.registry)
        val flintData = StoneType.FLINT.data(materials.registry)
        val gravelBlock = materials.sand.block(1)
        val airBlock = materials.air.block(0)
        addLayer(LayerRock(materials.stoneRock, materials.stoneRaw, 8
                , { terrain, x, y, z ->
            terrain.block(x, y, z - 1) == gravelBlock && terrain.block(x, y,
                    z) == airBlock
        }))
        addLayer(LayerGround(materials.stoneRock,
                { terrain, x, y, z, random -> data }, 4
                , { terrain, x, y, z ->
            terrain.block(x, y, z - 1) == gravelBlock && terrain.block(x, y,
                    z) == airBlock
        }))
        addLayer(LayerGround(materials.stoneRock,
                { terrain, x, y, z, random -> flintData }, 12
                , { terrain, x, y, z ->
            terrain.block(x, y, z - 1) == gravelBlock && terrain.block(x, y,
                    z) == airBlock
        }))
    }

    // Polar
    decorator(BiomeGenerator.Biome.POLAR, "Waste", 10) { }

    // Tundra
    decorator(BiomeGenerator.Biome.TUNDRA, "Waste", 10) { }
    decorator(BiomeGenerator.Biome.TUNDRA, "SprucePlains", 1) {
        addLayer(LayerTree(TreeSpruce.INSTANCE, 256))
    }

    // Taiga
    decorator(BiomeGenerator.Biome.TAIGA, "SprucePlains", 10) {
        addLayer(LayerTree(TreeSpruce.INSTANCE, 64))
    }
    decorator(BiomeGenerator.Biome.TAIGA, "SpruceForest", 10) {
        addLayer(LayerTree(TreeSpruce.INSTANCE, 16))
    }
    decorator(BiomeGenerator.Biome.TAIGA, "SequoiaForest", 2) {
        addLayer(LayerTree(TreeSequoia.INSTANCE, 256))
        addLayer(LayerTree(TreeSpruce.INSTANCE, 128))
    }

    // Wasteland
    decorator(BiomeGenerator.Biome.WASTELAND, "Waste", 10) { }
    decorator(BiomeGenerator.Biome.WASTELAND, "Shrubland", 4) {
        addShrubs(materials, 64)
    }

    // Steppe
    decorator(BiomeGenerator.Biome.STEPPE, "Waste", 3) { }
    decorator(BiomeGenerator.Biome.STEPPE, "BirchPlains", 5) {
        addLayer(LayerTree(TreeBirch.INSTANCE, 4096))
        addShrubs(materials, 128)
    }
    decorator(BiomeGenerator.Biome.STEPPE, "SprucePlains", 10) {
        addLayer(LayerTree(TreeSpruce.INSTANCE, 2048))
        addShrubs(materials, 128)
    }

    // Plains
    decorator(BiomeGenerator.Biome.PLAINS, "Waste", 1) { }
    decorator(BiomeGenerator.Biome.PLAINS, "Plains", 10) {
        addLayer(LayerTree(TreeOak.INSTANCE, 8192))
        addLayer(LayerTree(TreeBirch.INSTANCE, 8192))
        addLayer(LayerTree(TreeMaple.INSTANCE, 8192))
        addShrubs(materials, 256)
    }
    decorator(BiomeGenerator.Biome.PLAINS, "BirchPlains", 5) {
        addLayer(LayerTree(TreeBirch.INSTANCE, 1024))
        addShrubs(materials, 256)
    }
    decorator(BiomeGenerator.Biome.PLAINS, "SprucePlains", 10) {
        addLayer(LayerTree(TreeSpruce.INSTANCE, 512))
        addShrubs(materials, 256)
    }
    decorator(BiomeGenerator.Biome.PLAINS, "DeciduousForest", 4) {
        addLayer(LayerTree(TreeOak.INSTANCE, 256))
        addLayer(LayerTree(TreeBirch.INSTANCE, 512))
        addLayer(LayerTree(TreeMaple.INSTANCE, 512))
        addShrubs(materials, 256)
    }

    // Forest
    decorator(BiomeGenerator.Biome.FOREST, "DeciduousForest", 10) {
        addLayer(LayerTree(TreeOak.INSTANCE, 96))
        addLayer(LayerTree(TreeBirch.INSTANCE, 128))
        addLayer(LayerTree(TreeMaple.INSTANCE, 96))
        addShrubs(materials, 128)
    }
    decorator(BiomeGenerator.Biome.FOREST, "BirchForest", 10) {
        addLayer(LayerTree(TreeBirch.INSTANCE, 48))
        addShrubs(materials, 128)
    }
    decorator(BiomeGenerator.Biome.FOREST, "SpruceForest", 10) {
        addLayer(LayerTree(TreeSpruce.INSTANCE, 16))
        addShrubs(materials, 128)
    }
    decorator(BiomeGenerator.Biome.FOREST, "WillowForest", 10) {
        addLayer(LayerTree(TreeWillow.INSTANCE, 64))
        addShrubs(materials, 128)
    }
    decorator(BiomeGenerator.Biome.FOREST, "SequoiaForest", 2) {
        addLayer(LayerTree(TreeSequoia.INSTANCE, 256))
        addShrubs(materials, 64)
    }

    // Desert
    decorator(BiomeGenerator.Biome.DESERT, "Waste", 10) { }

    // Xeric Shrubland
    decorator(BiomeGenerator.Biome.XERIC_SHRUBLAND, "Waste", 4) { }
    decorator(BiomeGenerator.Biome.XERIC_SHRUBLAND, "Shrubland", 10) {
        addShrubs(materials, 64)
    }

    // Dry Savanna
    decorator(BiomeGenerator.Biome.DRY_SAVANNA, "Waste", 1) { }
    decorator(BiomeGenerator.Biome.DRY_SAVANNA, "Shrubland", 10) {
        addShrubs(materials, 256)
    }

    // Wet Savanna
    decorator(BiomeGenerator.Biome.WET_SAVANNA, "DeciduousForest", 10) {
        addLayer(LayerTree(TreeOak.INSTANCE, 256))
        addLayer(LayerTree(TreeBirch.INSTANCE, 512))
        addLayer(LayerTree(TreePalm.INSTANCE, 512))
        addShrubs(materials, 128)
    }

    // Oasis
    decorator(BiomeGenerator.Biome.OASIS, "PalmForest", 10) {
        addLayer(LayerTree(TreePalm.INSTANCE, 128))
        addShrubs(materials, 512)
    }

    // Rainforest
    decorator(BiomeGenerator.Biome.RAINFOREST, "DeciduousForest", 10) {
        addLayer(LayerTree(TreeOak.INSTANCE, 48))
        addLayer(LayerTree(TreeBirch.INSTANCE, 96))
        addLayer(LayerTree(TreePalm.INSTANCE, 128))
        addShrubs(materials, 256)
    }
    decorator(BiomeGenerator.Biome.RAINFOREST, "WillowForest", 4) {
        addLayer(LayerTree(TreeWillow.INSTANCE, 24))
        addLayer(LayerTree(TreePalm.INSTANCE, 256))
        addShrubs(materials, 256)
    }
}

fun BiomeDecorator.addShrubs(m: VanillaMaterial,
                             density: Int) {
    for (i in 0..19) {
        addLayer(LayerPatch(m.flower, i, 16, density shr 4,
                (1 shl 23) / density
        ) { terrain, x, y, z -> terrain.type(x, y, z - 1) === m.grass })
    }
    addLayer(
            LayerPatch(m.bush, 0, 16, density shr 3, (1 shl 18) / density
            ) { terrain, x, y, z -> terrain.type(x, y, z - 1) === m.grass })
}