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

import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.vanilla.basics.generator.tree.Tree;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Random;

public class LayerTree implements BiomeDecorator.Layer {
    private final Tree tree;
    private final int chance;

    public LayerTree(Tree tree, int chance) {
        this.tree = tree;
        this.chance = chance;
    }

    @Override
    public void decorate(TerrainServer.TerrainMutable terrain, int x, int y,
            VanillaMaterial materials, Random random) {
        if (random.nextInt(chance) == 0) {
            int z = terrain.highestTerrainBlockZAt(x, y);
            tree.gen(terrain, x, y, z, materials, random);
        }
    }
}
