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

import org.tobi29.scapes.vanilla.basics.generator.StoneType

class VanillaBasicsStones(reg: (String, (Int) -> StoneType) -> StoneType) {
    val DIRT_STONE = reg("vanilla.basics.stone.DirtStone") {
        StoneType(it, "Dirt Stone", ROOT, 0.1)
    }
    val FLINT = reg("vanilla.basics.stone.Flint") {
        StoneType(it, "Flint", ROOT, 2.1)
    }
    val CHALK = reg("vanilla.basics.stone.Chalk") {
        StoneType(it, "Chalk", ROOT, 0.4)
    }
    val CHERT = reg("vanilla.basics.stone.Chert") {
        StoneType(it, "Chert", ROOT, 0.4)
    }
    val CLAYSTONE = reg("vanilla.basics.stone.Claystone") {
        StoneType(it, "Claystone", ROOT, 0.4)
    }
    val CONGLOMERATE = reg("vanilla.basics.stone.Conglomerate") {
        StoneType(it, "Conglomerate", ROOT, 0.4)
    }
    val MARBLE = reg("vanilla.basics.stone.Marble") {
        StoneType(it, "Marble", ROOT, 0.6)
    }
    val ANDESITE = reg("vanilla.basics.stone.Andesite") {
        StoneType(it, "Andesite", ROOT, 1.4)
    }
    val BASALT = reg("vanilla.basics.stone.Basalt") {
        StoneType(it, "Basalt", ROOT, 1.4)
    }
    val DACITE = reg("vanilla.basics.stone.Dacite") {
        StoneType(it, "Dacite", ROOT, 1.4)
    }
    val RHYOLITE = reg("vanilla.basics.stone.Rhyolite") {
        StoneType(it, "Rhyolite", ROOT, 1.4)
    }
    val DIORITE = reg("vanilla.basics.stone.Diorite") {
        StoneType(it, "Diorite", ROOT, 1.2)
    }
    val GABBRO = reg("vanilla.basics.stone.Gabbro") {
        StoneType(it, "Gabbro", ROOT, 1.3)
    }
    val GRANITE = reg("vanilla.basics.stone.Granite") {
        StoneType(it, "Granite", ROOT, 1.5)
    }
}

private const val ROOT = "VanillaBasics:image/terrain/stone"
