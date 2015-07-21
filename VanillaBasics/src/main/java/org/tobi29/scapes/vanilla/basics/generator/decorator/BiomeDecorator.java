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

import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BiomeDecorator {
    private final List<Layer> layers = new ArrayList<>();
    private int weight, weightCount;

    public void addWeight(int weight) {
        this.weight += weight;
        weightCount++;
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
    }

    public int weight() {
        if (weightCount == 0) {
            return 0;
        }
        return weight / weightCount;
    }

    public void decorate(TerrainServer.TerrainMutable terrain, int x, int y,
            VanillaMaterial materials, Random random) {
        layers.forEach(
                layer -> layer.decorate(terrain, x, y, materials, random));
    }

    @FunctionalInterface
    public interface Layer {
        void decorate(TerrainServer.TerrainMutable terrain, int x, int y,
                VanillaMaterial materials, Random random);
    }
}
