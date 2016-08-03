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

package org.tobi29.scapes.vanilla.basics.util;

import java8.util.stream.Stream;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.AlloyType;
import org.tobi29.scapes.vanilla.basics.material.MetalType;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetalUtil {
    public static Alloy read(VanillaBasics plugin, TagStructure tagStructure) {
        Alloy alloy = new Alloy();
        Streams.of(tagStructure.getTagEntrySet())
                .filter(entry -> entry.getValue() instanceof Number).forEach(
                entry -> alloy.metals.put(plugin.metalType(entry.getKey()),
                        (double) entry.getValue()));
        return alloy;
    }

    public static TagStructure write(Alloy alloy) {
        TagStructure tagStructure = new TagStructure();
        alloy.metals().forEach(
                entry -> tagStructure.setDouble(entry.a.id(), entry.b));
        return tagStructure;
    }

    public static class Alloy {
        private final Map<MetalType, Double> metals = new ConcurrentHashMap<>();

        public void add(MetalType metal, double amount) {
            Double containing = metals.get(metal);
            if (containing == null) {
                containing = amount;
            } else {
                containing += amount;
            }
            metals.put(metal, containing);
        }

        public double drain(MetalType metal, double maxAmount) {
            Double containing = metals.get(metal);
            if (containing == null) {
                return 0.0;
            }
            double drain;
            if (containing <= maxAmount) {
                drain = containing;
                containing = 0.0;
            } else {
                drain = maxAmount;
                containing -= maxAmount;
            }
            if (containing < 0.00001) {
                metals.remove(metal);
            } else {
                metals.put(metal, containing);
            }
            return drain;
        }

        public Alloy drain(double maxAmount) {
            double amount = amount();
            Alloy alloy = new Alloy();
            for (Map.Entry<MetalType, Double> entry : metals.entrySet()) {
                double drain = entry.getValue() / amount * maxAmount;
                alloy.add(entry.getKey(), drain(entry.getKey(), drain));
            }
            return alloy;
        }

        public Stream<Pair<MetalType, Double>> metals() {
            return Streams.of(metals.entrySet())
                    .map(entry -> new Pair<>(entry.getKey(), entry.getValue()));
        }

        public AlloyType type(VanillaBasics plugin) {
            AlloyType bestAlloyType = plugin.alloyType("");
            if (metals.isEmpty()) {
                return bestAlloyType;
            }
            double amount = amount();
            double bestOffset = Double.POSITIVE_INFINITY;
            Iterator<AlloyType> iterator = plugin.alloyTypes().iterator();
            while (iterator.hasNext()) {
                AlloyType alloyType = iterator.next();
                double offset = 0.0;
                for (Map.Entry<MetalType, Double> entry : metals.entrySet()) {
                    Double required =
                            alloyType.ingredients().get(entry.getKey());
                    if (required == null) {
                        offset = Double.POSITIVE_INFINITY;
                        break;
                    }
                    offset +=
                            FastMath.abs(entry.getValue() / amount - required);
                }
                if (offset < bestOffset) {
                    bestAlloyType = alloyType;
                    bestOffset = offset;
                }
            }
            return bestAlloyType;
        }

        public double amount() {
            double amount = 0.0;
            for (Map.Entry<MetalType, Double> entry : metals.entrySet()) {
                amount += entry.getValue();
            }
            return amount;
        }

        public double meltingPoint() {
            double amount = 0.0, temperature = 0.0;
            for (Map.Entry<MetalType, Double> entry : metals.entrySet()) {
                double metal = entry.getValue();
                amount += metal;
                temperature += metal * entry.getKey().meltingPoint();
            }
            return temperature / amount;
        }
    }
}
