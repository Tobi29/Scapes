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

package scapes.plugin.tobi29.vanilla.basics

import org.tobi29.scapes.block.GameRegistry
import scapes.plugin.tobi29.vanilla.basics.material.TreeType

internal fun registerTreeTypes(registry: GameRegistry) {
    registry.get<TreeType>("VanillaBasics", "TreeType").run {
        reg(TreeType.OAK, "vanilla.basics.tree.Oak")
        reg(TreeType.BIRCH, "vanilla.basics.tree.Birch")
        reg(TreeType.SPRUCE, "vanilla.basics.tree.Spruce")
        reg(TreeType.PALM, "vanilla.basics.tree.Palm")
        reg(TreeType.MAPLE, "vanilla.basics.tree.Maple")
        reg(TreeType.SEQUOIA, "vanilla.basics.tree.Sequoia")
        reg(TreeType.WILLOW, "vanilla.basics.tree.Willow")
    }
}
