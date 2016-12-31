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

package scapes.plugin.tobi29.vanilla.basics.material

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import scapes.plugin.tobi29.vanilla.basics.generator.tree.*

class TreeType internal constructor(private val name: String, textureRoot: String, private val colorCold: Vector3d,
                                    private val colorWarm: Vector3d, private val colorAutumn: Vector3d, private val dropChance: Int,
                                    private val generator: Tree, val isEvergreen: Boolean) {
    private val texture: String

    constructor(name: String, textureRoot: String, colorCold: Vector3d,
                colorWarm: Vector3d, dropChance: Int, generator: Tree) : this(
            name, textureRoot, colorCold, colorWarm, Vector3d.ZERO, dropChance,
            generator, true)

    constructor(name: String, textureRoot: String, colorCold: Vector3d,
                colorWarm: Vector3d, colorAutumn: Vector3d, dropChance: Int,
                generator: Tree) : this(name, textureRoot, colorCold, colorWarm,
            colorAutumn, dropChance,
            generator, false)

    init {
        texture = "$textureRoot/${name.replace(" ", "").toLowerCase()}"
    }

    fun name(): String {
        return name
    }

    fun texture(): String {
        return texture
    }

    fun colorCold(): Vector3d {
        return colorCold
    }

    fun colorWarm(): Vector3d {
        return colorWarm
    }

    fun colorAutumn(): Vector3d {
        return colorAutumn
    }

    fun dropChance(): Int {
        return dropChance
    }

    fun generator(): Tree {
        return generator
    }

    fun data(registry: GameRegistry): Int {
        return registry.get<Any>("VanillaBasics", "TreeType")[this]
    }

    companion object {
        private val ROOT = "VanillaBasics:image/terrain/tree"
        val OAK = TreeType("Oak", ROOT, Vector3d(0.5, 0.9, 0.4),
                Vector3d(0.5, 0.8, 0.0), Vector3d(1.0, 0.7, 0.0), 80, TreeOak())
        val BIRCH = TreeType("Birch", ROOT, Vector3d(0.6, 0.9, 0.5),
                Vector3d(0.6, 0.8, 0.1), Vector3d(1.0, 0.8, 0.0), 20,
                TreeBirch())
        val SPRUCE = TreeType("Spruce", ROOT, Vector3d(0.2, 0.5, 0.2),
                Vector3d(0.2, 0.5, 0.0), 10, TreeSpruce())
        val PALM = TreeType("Palm", ROOT, Vector3d(0.5, 0.9, 0.4),
                Vector3d(0.5, 0.8, 0.0), 3, TreePalm())
        val MAPLE = TreeType("Maple", ROOT, Vector3d(0.5, 0.9, 0.4),
                Vector3d(0.5, 0.8, 0.0), Vector3d(1.0, 0.4, 0.0), 70,
                TreeMaple())
        val SEQUOIA = TreeType("Sequoia", ROOT, Vector3d(0.2, 0.5, 0.2),
                Vector3d(0.2, 0.5, 0.0), 200, TreeSequoia())
        val WILLOW = TreeType("Willow", ROOT, Vector3d(0.5, 0.9, 0.4),
                Vector3d(0.5, 0.8, 0.0), Vector3d(0.9, 0.7, 0.0), 100,
                TreeWillow())

        operator fun get(registry: GameRegistry,
                         data: Int): TreeType {
            return registry.get<TreeType>("VanillaBasics", "TreeType")[data]
        }
    }
}
