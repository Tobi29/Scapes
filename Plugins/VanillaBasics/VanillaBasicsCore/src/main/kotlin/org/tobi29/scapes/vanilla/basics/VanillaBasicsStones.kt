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

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.vanilla.basics.material.StoneType

internal fun registerStoneTypes(registry: GameRegistry) {
    registry.get<StoneType>("VanillaBasics", "StoneType").run {
        reg(StoneType.DIRT_STONE, "vanilla.basics.stone.DirtStone")
        reg(StoneType.FLINT, "vanilla.basics.stone.Flint")
        reg(StoneType.CHALK, "vanilla.basics.stone.Chalk")
        reg(StoneType.CHERT, "vanilla.basics.stone.Chert")
        reg(StoneType.CLAYSTONE, "vanilla.basics.stone.Claystone")
        reg(StoneType.CONGLOMERATE, "vanilla.basics.stone.Conglomerate")
        reg(StoneType.MARBLE, "vanilla.basics.stone.Marble")
        reg(StoneType.ANDESITE, "vanilla.basics.stone.Andesite")
        reg(StoneType.BASALT, "vanilla.basics.stone.Basalt")
        reg(StoneType.DACITE, "vanilla.basics.stone.Dacite")
        reg(StoneType.RHYOLITE, "vanilla.basics.stone.Rhyolite")
        reg(StoneType.DIORITE, "vanilla.basics.stone.Diorite")
        reg(StoneType.GABBRO, "vanilla.basics.stone.Gabbro")
        reg(StoneType.GRANITE, "vanilla.basics.stone.Granite")
    }
}