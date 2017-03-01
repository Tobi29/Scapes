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

package org.tobi29.scapes.vanilla.basics.viewer.generator

import org.tobi29.scapes.engine.swt.util.framework.Document
import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar
import org.tobi29.scapes.engine.utils.version
import org.tobi29.scapes.vanilla.basics.generator.BiomeGenerator
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator
import org.tobi29.scapes.vanilla.basics.generator.TerrainGenerator
import java.util.*

fun main(args: Array<String>) {
    TerrainViewer().run()
}

class TerrainViewer : MultiDocumentApplication("Scapes Terrain Viewer",
        "TerrainViewer", version("0.0.0")) {
    override fun init() {
        openTab(openTerrain())
    }

    override fun dispose() {
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar) {
        val view = menu.menu("View")
        view.action("Terrain") { openNewTab(composite, openTerrain()) }
        view.action("Biome") { openNewTab(composite, openBiome()) }
        view.action("Climate") { openNewTab(composite, openClimate()) }
    }

    fun openTerrain(): Document {
        val random = Random()
        val terrainGenerator = TerrainGenerator(random)
        return TerrainViewerDocument({ terrain(terrainGenerator) })
    }

    fun openBiome(): Document {
        val random = Random()
        val terrainGenerator = TerrainGenerator(random)
        val climateGenerator = ClimateGenerator(random, terrainGenerator)
        val biomeGenerator = BiomeGenerator(climateGenerator, terrainGenerator)
        return TerrainViewerDocument({
            mix(biome(biomeGenerator), terrain(terrainGenerator), 0.3)
        })
    }

    fun openClimate(): Document {
        val random = Random()
        val terrainGenerator = TerrainGenerator(random)
        val climateGenerator = ClimateGenerator(random, terrainGenerator)
        return TerrainViewerAnimatedDocument({ climate(climateGenerator) },
                { climateGenerator.add(1.0) })
    }
}
