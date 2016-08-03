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

package org.tobi29.scapes.vanilla.basics.generator.decorator;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Random;

public class LayerRock implements BiomeDecorator.Layer {
    private final BlockType material, stone;
    private final int chance, depthMin, depthDelta;
    private final LayerGround.GroundCheck check;

    public LayerRock(BlockType material, BlockType stone, int chance,
            LayerGround.GroundCheck check) {
        this(material, stone, chance, check, 4, 24);
    }

    public LayerRock(BlockType material, BlockType stone, int chance,
            LayerGround.GroundCheck check, int depthMin, int depthMax) {
        this.material = material;
        this.stone = stone;
        this.chance = chance;
        this.depthMin = depthMin;
        depthDelta = depthMax - depthMin + 1;
        this.check = check;
    }

    @Override
    public void decorate(TerrainServer.TerrainMutable terrain, int x, int y,
            VanillaMaterial materials, Random random) {
        if (random.nextInt(chance) == 0) {
            int z = terrain.highestTerrainBlockZAt(x, y);
            int zz = z - random.nextInt(depthDelta) - depthMin;
            BlockType ground = terrain.type(x, y, zz);
            if (ground == stone) {
                if (check.canPlace(terrain, x, y, z)) {
                    terrain.typeData(x, y, z, material, terrain.data(x, y, zz));
                }
            }
        }
    }
}
