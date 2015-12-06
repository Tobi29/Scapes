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

package org.tobi29.scapes.vanilla.basics.generator.decorator;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Random;

public class LayerGround implements BiomeDecorator.Layer {
    private final BlockType material;
    private final int chance;
    private final GroundCheck check;
    private final DataProducer data;

    public LayerGround(BlockType material, DataProducer data, int chance,
            GroundCheck check) {
        this.material = material;
        this.data = data;
        this.chance = chance;
        this.check = check;
    }

    @Override
    public void decorate(TerrainServer.TerrainMutable terrain, int x, int y,
            VanillaMaterial materials, Random random) {
        if (random.nextInt(chance) == 0) {
            int z = terrain.highestTerrainBlockZAt(x, y);
            if (check.canPlace(terrain, x, y, z)) {
                terrain.typeData(x, y, z, material,
                        data.data(terrain, x, y, z, random));
            }
        }
    }

    public interface GroundCheck {
        boolean canPlace(TerrainServer.TerrainMutable terrain, int x, int y,
                int z);
    }

    public interface DataProducer {
        int data(TerrainServer.TerrainMutable terrain, int x, int y, int z,
                Random random);
    }
}
