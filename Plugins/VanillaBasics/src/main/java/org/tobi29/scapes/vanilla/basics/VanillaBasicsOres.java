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

package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.vanilla.basics.material.StoneType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

class VanillaBasicsOres {
    static void registerOres(VanillaBasics plugin) {
        VanillaMaterial materials = plugin.getMaterials();
        plugin.c.ore(o -> {
            o.type = materials.oreCoal;
            o.rarity = 9;
            o.size = 16.0;
            o.chance = 3;
            o.rockChance = 20;
            o.rockDistance = 16;
            o.stoneTypes.add(StoneType.DIRT_STONE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreCoal;
            o.rarity = 3;
            o.size = 12.0;
            o.chance = 3;
            o.rockChance = 20;
            o.rockDistance = 32;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
            o.stoneTypes.add(StoneType.MARBLE);
            o.stoneTypes.add(StoneType.ANDESITE);
            o.stoneTypes.add(StoneType.BASALT);
            o.stoneTypes.add(StoneType.DACITE);
            o.stoneTypes.add(StoneType.RHYOLITE);
            o.stoneTypes.add(StoneType.DIORITE);
            o.stoneTypes.add(StoneType.GABBRO);
            o.stoneTypes.add(StoneType.GRANITE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreCassiterite;
            o.rarity = 2;
            o.size = 24.0;
            o.chance = 64;
            o.rockChance = 12;
            o.rockDistance = 32;
            o.stoneTypes.add(StoneType.ANDESITE);
            o.stoneTypes.add(StoneType.BASALT);
            o.stoneTypes.add(StoneType.DACITE);
            o.stoneTypes.add(StoneType.RHYOLITE);
            o.stoneTypes.add(StoneType.GRANITE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreSphalerite;
            o.rarity = 6;
            o.size = 6.0;
            o.chance = 3;
            o.rockChance = 4;
            o.rockDistance = 16;
            o.stoneTypes.add(StoneType.MARBLE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreBismuthinite;
            o.rarity = 2;
            o.size = 3.0;
            o.chance = 8;
            o.rockChance = 9;
            o.rockDistance = 128;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
            o.stoneTypes.add(StoneType.MARBLE);
            o.stoneTypes.add(StoneType.DIORITE);
            o.stoneTypes.add(StoneType.GABBRO);
            o.stoneTypes.add(StoneType.GRANITE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreChalcocite;
            o.rarity = 12;
            o.size = 8.0;
            o.chance = 2;
            o.rockChance = 1;
            o.rockDistance = 24;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreMagnetite;
            o.rarity = 4;
            o.size = 64.0;
            o.chance = 12;
            o.rockChance = 10;
            o.rockDistance = 96;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
        });
        plugin.c.ore(o -> {
            o.type = materials.orePyrite;
            o.rarity = 3;
            o.size = 4.0;
            o.chance = 11;
            o.rockChance = 13;
            o.rockDistance = 48;
            o.stoneTypes.add(StoneType.CHALK);
            o.stoneTypes.add(StoneType.CHERT);
            o.stoneTypes.add(StoneType.CLAYSTONE);
            o.stoneTypes.add(StoneType.CONGLOMERATE);
            o.stoneTypes.add(StoneType.MARBLE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreSilver;
            o.rarity = 2;
            o.size = 3.0;
            o.chance = 4;
            o.rockChance = 8;
            o.rockDistance = 64;
            o.stoneTypes.add(StoneType.GRANITE);
        });
        plugin.c.ore(o -> {
            o.type = materials.oreGold;
            o.rarity = 1;
            o.size = 2.0;
            o.chance = 4;
            o.rockChance = 96;
            o.rockDistance = 256;
            o.stoneTypes.add(StoneType.ANDESITE);
            o.stoneTypes.add(StoneType.BASALT);
            o.stoneTypes.add(StoneType.DACITE);
            o.stoneTypes.add(StoneType.RHYOLITE);
            o.stoneTypes.add(StoneType.DIORITE);
            o.stoneTypes.add(StoneType.GABBRO);
            o.stoneTypes.add(StoneType.GRANITE);
        });
    }
}
