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

internal fun VanillaBasics.registerOres() {
    ore {
        type = materials.oreCoal
        rarity = 9
        size = 16.0
        chance = 3
        rockChance = 20
        rockDistance = 16
        stones.add(stoneTypes.DIRT_STONE)
    }
    ore {
        type = materials.oreCoal
        rarity = 3
        size = 12.0
        chance = 3
        rockChance = 20
        rockDistance = 32
        stones.add(stoneTypes.CHALK)
        stones.add(stoneTypes.CHERT)
        stones.add(stoneTypes.CLAYSTONE)
        stones.add(stoneTypes.CONGLOMERATE)
        stones.add(stoneTypes.MARBLE)
        stones.add(stoneTypes.ANDESITE)
        stones.add(stoneTypes.BASALT)
        stones.add(stoneTypes.DACITE)
        stones.add(stoneTypes.RHYOLITE)
        stones.add(stoneTypes.DIORITE)
        stones.add(stoneTypes.GABBRO)
        stones.add(stoneTypes.GRANITE)
    }
    ore {
        type = materials.oreCassiterite
        rarity = 2
        size = 24.0
        chance = 64
        rockChance = 12
        rockDistance = 32
        stones.add(stoneTypes.ANDESITE)
        stones.add(stoneTypes.BASALT)
        stones.add(stoneTypes.DACITE)
        stones.add(stoneTypes.RHYOLITE)
        stones.add(stoneTypes.GRANITE)
    }
    ore {
        type = materials.oreSphalerite
        rarity = 6
        size = 6.0
        chance = 3
        rockChance = 4
        rockDistance = 16
        stones.add(stoneTypes.MARBLE)
    }
    ore {
        type = materials.oreBismuthinite
        rarity = 2
        size = 3.0
        chance = 8
        rockChance = 9
        rockDistance = 128
        stones.add(stoneTypes.CHALK)
        stones.add(stoneTypes.CHERT)
        stones.add(stoneTypes.CLAYSTONE)
        stones.add(stoneTypes.CONGLOMERATE)
        stones.add(stoneTypes.MARBLE)
        stones.add(stoneTypes.DIORITE)
        stones.add(stoneTypes.GABBRO)
        stones.add(stoneTypes.GRANITE)
    }
    ore {
        type = materials.oreChalcocite
        rarity = 12
        size = 8.0
        chance = 2
        rockChance = 1
        rockDistance = 24
        stones.add(stoneTypes.CHALK)
        stones.add(stoneTypes.CHERT)
        stones.add(stoneTypes.CLAYSTONE)
        stones.add(stoneTypes.CONGLOMERATE)
    }
    ore {
        type = materials.oreMagnetite
        rarity = 4
        size = 64.0
        chance = 12
        rockChance = 10
        rockDistance = 96
        stones.add(stoneTypes.CHALK)
        stones.add(stoneTypes.CHERT)
        stones.add(stoneTypes.CLAYSTONE)
        stones.add(stoneTypes.CONGLOMERATE)
    }
    ore {
        type = materials.orePyrite
        rarity = 3
        size = 4.0
        chance = 11
        rockChance = 13
        rockDistance = 48
        stones.add(stoneTypes.CHALK)
        stones.add(stoneTypes.CHERT)
        stones.add(stoneTypes.CLAYSTONE)
        stones.add(stoneTypes.CONGLOMERATE)
        stones.add(stoneTypes.MARBLE)
    }
    ore {
        type = materials.oreSilver
        rarity = 2
        size = 3.0
        chance = 4
        rockChance = 8
        rockDistance = 64
        stones.add(stoneTypes.GRANITE)
    }
    ore {
        type = materials.oreGold
        rarity = 1
        size = 2.0
        chance = 4
        rockChance = 96
        rockDistance = 256
        stones.add(stoneTypes.ANDESITE)
        stones.add(stoneTypes.BASALT)
        stones.add(stoneTypes.DACITE)
        stones.add(stoneTypes.RHYOLITE)
        stones.add(stoneTypes.DIORITE)
        stones.add(stoneTypes.GABBRO)
        stones.add(stoneTypes.GRANITE)
    }
}
