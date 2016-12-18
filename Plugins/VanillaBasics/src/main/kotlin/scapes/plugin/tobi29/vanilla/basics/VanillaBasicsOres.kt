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

package scapes.plugin.tobi29.vanilla.basics

import scapes.plugin.tobi29.vanilla.basics.material.StoneType

internal fun VanillaBasics.registerOres() {
    ore {
        type = materials.oreCoal
        rarity = 9
        size = 16.0
        chance = 3
        rockChance = 20
        rockDistance = 16
        stoneTypes.add(StoneType.DIRT_STONE)
    }
    ore {
        type = materials.oreCoal
        rarity = 3
        size = 12.0
        chance = 3
        rockChance = 20
        rockDistance = 32
        stoneTypes.add(StoneType.CHALK)
        stoneTypes.add(StoneType.CHERT)
        stoneTypes.add(StoneType.CLAYSTONE)
        stoneTypes.add(StoneType.CONGLOMERATE)
        stoneTypes.add(StoneType.MARBLE)
        stoneTypes.add(StoneType.ANDESITE)
        stoneTypes.add(StoneType.BASALT)
        stoneTypes.add(StoneType.DACITE)
        stoneTypes.add(StoneType.RHYOLITE)
        stoneTypes.add(StoneType.DIORITE)
        stoneTypes.add(StoneType.GABBRO)
        stoneTypes.add(StoneType.GRANITE)
    }
    ore {
        type = materials.oreCassiterite
        rarity = 2
        size = 24.0
        chance = 64
        rockChance = 12
        rockDistance = 32
        stoneTypes.add(StoneType.ANDESITE)
        stoneTypes.add(StoneType.BASALT)
        stoneTypes.add(StoneType.DACITE)
        stoneTypes.add(StoneType.RHYOLITE)
        stoneTypes.add(StoneType.GRANITE)
    }
    ore {
        type = materials.oreSphalerite
        rarity = 6
        size = 6.0
        chance = 3
        rockChance = 4
        rockDistance = 16
        stoneTypes.add(StoneType.MARBLE)
    }
    ore {
        type = materials.oreBismuthinite
        rarity = 2
        size = 3.0
        chance = 8
        rockChance = 9
        rockDistance = 128
        stoneTypes.add(StoneType.CHALK)
        stoneTypes.add(StoneType.CHERT)
        stoneTypes.add(StoneType.CLAYSTONE)
        stoneTypes.add(StoneType.CONGLOMERATE)
        stoneTypes.add(StoneType.MARBLE)
        stoneTypes.add(StoneType.DIORITE)
        stoneTypes.add(StoneType.GABBRO)
        stoneTypes.add(StoneType.GRANITE)
    }
    ore {
        type = materials.oreChalcocite
        rarity = 12
        size = 8.0
        chance = 2
        rockChance = 1
        rockDistance = 24
        stoneTypes.add(StoneType.CHALK)
        stoneTypes.add(StoneType.CHERT)
        stoneTypes.add(StoneType.CLAYSTONE)
        stoneTypes.add(StoneType.CONGLOMERATE)
    }
    ore {
        type = materials.oreMagnetite
        rarity = 4
        size = 64.0
        chance = 12
        rockChance = 10
        rockDistance = 96
        stoneTypes.add(StoneType.CHALK)
        stoneTypes.add(StoneType.CHERT)
        stoneTypes.add(StoneType.CLAYSTONE)
        stoneTypes.add(StoneType.CONGLOMERATE)
    }
    ore {
        type = materials.orePyrite
        rarity = 3
        size = 4.0
        chance = 11
        rockChance = 13
        rockDistance = 48
        stoneTypes.add(StoneType.CHALK)
        stoneTypes.add(StoneType.CHERT)
        stoneTypes.add(StoneType.CLAYSTONE)
        stoneTypes.add(StoneType.CONGLOMERATE)
        stoneTypes.add(StoneType.MARBLE)
    }
    ore {
        type = materials.oreSilver
        rarity = 2
        size = 3.0
        chance = 4
        rockChance = 8
        rockDistance = 64
        stoneTypes.add(StoneType.GRANITE)
    }
    ore {
        type = materials.oreGold
        rarity = 1
        size = 2.0
        chance = 4
        rockChance = 96
        rockDistance = 256
        stoneTypes.add(StoneType.ANDESITE)
        stoneTypes.add(StoneType.BASALT)
        stoneTypes.add(StoneType.DACITE)
        stoneTypes.add(StoneType.RHYOLITE)
        stoneTypes.add(StoneType.DIORITE)
        stoneTypes.add(StoneType.GABBRO)
        stoneTypes.add(StoneType.GRANITE)
    }
}
