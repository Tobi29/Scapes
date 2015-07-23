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

package org.tobi29.scapes.server.format;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerStatistics {
    private final List<StatisticMaterial> statisticMaterials =
            new ArrayList<>();

    public void blockBreak(Material type, int data) {
        for (StatisticMaterial statisticMaterial : statisticMaterials) {
            if (statisticMaterial.type() == type &&
                    statisticMaterial.data() == data) {
                statisticMaterial.blockBreak();
                return;
            }
        }
        statisticMaterials.add(new StatisticMaterial(type, data, 1, 0, 0));
    }

    public void blockCraft(Material type, int data) {
        for (StatisticMaterial statisticMaterial : statisticMaterials) {
            if (statisticMaterial.type() == type &&
                    statisticMaterial.data() == data) {
                statisticMaterial.blockCraft();
                return;
            }
        }
        statisticMaterials.add(new StatisticMaterial(type, data, 0, 0, 1));
    }

    public void blockPlace(Material type, int data) {
        for (StatisticMaterial statisticMaterial : statisticMaterials) {
            if (statisticMaterial.type() == type &&
                    statisticMaterial.data() == data) {
                statisticMaterial.blockPlace();
                return;
            }
        }
        statisticMaterials.add(new StatisticMaterial(type, data, 0, 1, 0));
    }

    public List<StatisticMaterial> statisticMaterials() {
        return Collections.unmodifiableList(statisticMaterials);
    }

    public void load(GameRegistry registry, List<TagStructure> tagStructures) {
        statisticMaterials.addAll(tagStructures.stream()
                .map(tagStructure -> new StatisticMaterial(
                        registry.material(tagStructure.getInteger("ID"))
                                .orElse(registry.air()),
                        tagStructure.getShort("Data"),
                        tagStructure.getInteger("BreakAmount"),
                        tagStructure.getInteger("PlaceAmount"),
                        tagStructure.getInteger("CraftAmount")))
                .collect(Collectors.toList()));
    }

    public List<TagStructure> save() {
        List<TagStructure> tagStructures = new ArrayList<>();
        for (StatisticMaterial statisticMaterial : statisticMaterials) {
            TagStructure tagStructure = new TagStructure();
            tagStructure.setInteger("ID", statisticMaterial.type().itemID());
            tagStructure.setInteger("Data", statisticMaterial.data);
            tagStructure
                    .setInteger("BreakAmount", statisticMaterial.breakAmount);
            tagStructure
                    .setInteger("PlaceAmount", statisticMaterial.placeAmount);
            tagStructure
                    .setInteger("CraftAmount", statisticMaterial.craftAmount);
            tagStructures.add(tagStructure);
        }
        return tagStructures;
    }

    public static class StatisticMaterial {
        private final int data;
        private final Material type;
        private int breakAmount, placeAmount, craftAmount;

        public StatisticMaterial(Material type, int data, int breakAmount,
                int placeAmount, int craftAmount) {
            this.type = type;
            this.data = data;
            this.breakAmount = breakAmount;
            this.placeAmount = placeAmount;
            this.craftAmount = craftAmount;
        }

        public void blockBreak() {
            breakAmount++;
        }

        public void blockCraft() {
            craftAmount++;
        }

        public void blockPlace() {
            placeAmount++;
        }

        public int breakAmount() {
            return breakAmount;
        }

        public int craftAmount() {
            return craftAmount;
        }

        public int data() {
            return data;
        }

        public int placeAmount() {
            return placeAmount;
        }

        public Material type() {
            return type;
        }
    }
}
