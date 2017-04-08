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

package org.tobi29.scapes.vanilla.basics.material

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.vanilla.basics.world.tree.Tree

class TreeType internal constructor(val id: Int,
                                    val name: String,
                                    textureRoot: String,
                                    val colorCold: Vector3d,
                                    val colorWarm: Vector3d,
                                    val colorAutumn: Vector3d,
                                    val dropChance: Int,
                                    val generator: Tree,
                                    val isEvergreen: Boolean) {
    val texture = "$textureRoot/${name.replace(" ", "").toLowerCase()}"

    constructor(id: Int,
                name: String,
                textureRoot: String,
                colorCold: Vector3d,
                colorWarm: Vector3d,
                dropChance: Int,
                generator: Tree) : this(id, name, textureRoot, colorCold,
            colorWarm, Vector3d.ZERO, dropChance, generator, true)

    constructor(id: Int,
                name: String,
                textureRoot: String,
                colorCold: Vector3d,
                colorWarm: Vector3d,
                colorAutumn: Vector3d,
                dropChance: Int,
                generator: Tree) : this(id, name, textureRoot, colorCold,
            colorWarm, colorAutumn, dropChance, generator, false)

    companion object {
        operator fun get(registry: Registries,
                         data: Int): TreeType {
            return registry.get<TreeType>("VanillaBasics", "TreeType")[data]
        }
    }
}
