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

package org.tobi29.scapes.vanilla.basics.generator.structure;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.vanilla.basics.generator.tree.Tree;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Random;

public final class StructurePlantPatch implements Tree {
    private final BlockType type;
    private final short data;
    private final int size, amount, sizeDiameter;

    public StructurePlantPatch(BlockType type, short data, int size,
            int amount) {
        this.type = type;
        this.data = data;
        this.size = size;
        this.amount = amount;
        sizeDiameter = size << 1;
    }

    @Override
    public void gen(TerrainServer.TerrainMutable terrain, int x, int y, int z,
            VanillaMaterial materials, Random random) {
        for (int i = 0; i < amount; i++) {
            int xx = x + random.nextInt(sizeDiameter) - size;
            int yy = y + random.nextInt(sizeDiameter) - size;
            int zz = terrain.getHighestTerrainBlockZAt(xx, yy);
            if (terrain.type(xx, yy, zz) == materials.air &&
                    terrain.type(xx, yy, zz - 1) == materials.grass) {
                terrain.typeData(xx, yy, zz, type, data);
            }
        }
    }
}
