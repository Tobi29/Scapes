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
    private final int rarity, chance, rockChance, rockDistance;
    private final double size;

    public OreType(BlockType type, int rarity, double size, int chance,
            int rockChance, int rockDistance, List<Integer> stoneTypes) {
        this.type = type;
        this.rarity = rarity;
        this.size = size;
        this.chance = chance;
        this.rockChance = rockChance;
        this.rockDistance = rockDistance;
        this.stoneTypes = stoneTypes;
    }

    public int rarity() {
        return rarity;
    }

    public double size() {
        return size;
    }

    public int chance() {
        return chance;
    }

    public int rockChance() {
        return rockChance;
    }

    public int rockDistance() {
        return rockDistance;
    }

    public BlockType type() {
        return type;
    }

    public List<Integer> stoneTypes() {
        return stoneTypes;
    }
}
