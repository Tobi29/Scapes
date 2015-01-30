/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.vanilla.basics.material;

import org.tobi29.scapes.block.BlockType;

import java.util.List;

public class OreType {
    private final BlockType type;
    private final List<Integer> stoneTypes;
    private final int rarity, chance, rockChance;
    private final double size;

    public OreType(BlockType type, int rarity, double size, int chance,
            int rockChance, List<Integer> stoneTypes) {
        this.type = type;
        this.rarity = rarity;
        this.size = size;
        this.chance = chance;
        this.rockChance = rockChance;
        this.stoneTypes = stoneTypes;
    }

    public int getRarity() {
        return rarity;
    }

    public double getSize() {
        return size;
    }

    public int getChance() {
        return chance;
    }

    public int getRockChance() {
        return rockChance;
    }

    public BlockType getBlockType() {
        return type;
    }

    public List<Integer> getStoneTypes() {
        return stoneTypes;
    }
}
