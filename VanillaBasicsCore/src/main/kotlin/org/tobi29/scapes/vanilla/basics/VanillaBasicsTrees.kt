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

import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.vanilla.basics.material.TreeType
import org.tobi29.scapes.vanilla.basics.world.tree.*

class VanillaBasicsTrees(reg: (String, (Int) -> TreeType) -> TreeType) {
    private val ROOT = "VanillaBasics:image/terrain/tree"

    val OAK = reg("vanilla.basics.tree.Oak") {
        TreeType(it, "Oak", ROOT, Vector3d(0.5, 0.9, 0.4),
                Vector3d(0.5, 0.8, 0.0), Vector3d(1.0, 0.7, 0.0), 80, TreeOak)
    }
    val BIRCH = reg("vanilla.basics.tree.Birch") {
        TreeType(it, "Birch", ROOT, Vector3d(0.6, 0.9, 0.5),
                Vector3d(0.6, 0.8, 0.1), Vector3d(1.0, 0.8, 0.0), 20,
                TreeBirch)
    }
    val SPRUCE = reg("vanilla.basics.tree.Spruce") {
        TreeType(it, "Spruce", ROOT, Vector3d(0.2, 0.5, 0.2),
                Vector3d(0.2, 0.5, 0.0), 10, TreeSpruce)
    }
    val PALM = reg("vanilla.basics.tree.Palm") {
        TreeType(it, "Palm", ROOT, Vector3d(0.5, 0.9, 0.4),
                Vector3d(0.5, 0.8, 0.0), 3, TreePalm)
    }
    val MAPLE = reg("vanilla.basics.tree.Maple") {
        TreeType(it, "Maple", ROOT, Vector3d(0.5, 0.9, 0.4),
                Vector3d(0.5, 0.8, 0.0), Vector3d(1.0, 0.4, 0.0), 70,
                TreeMaple)
    }
    val SEQUOIA = reg("vanilla.basics.tree.Sequoia") {
        TreeType(it, "Sequoia", ROOT, Vector3d(0.2, 0.5, 0.2),
                Vector3d(0.2, 0.5, 0.0), 200, TreeSequoia)
    }
    val WILLOW = reg("vanilla.basics.tree.Willow") {
        TreeType(it, "Willow", ROOT, Vector3d(0.5, 0.9, 0.4),
                Vector3d(0.5, 0.8, 0.0), Vector3d(0.9, 0.7, 0.0), 100,
                TreeWillow)
    }
}
