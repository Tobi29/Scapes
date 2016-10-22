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
package org.tobi29.scapes.server.format

import java8.util.stream.Collectors
import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.Material
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getInt
import org.tobi29.scapes.engine.utils.io.tag.setInt
import org.tobi29.scapes.engine.utils.stream
import java.util.*

class PlayerStatistics {
    private val statisticMaterials = ArrayList<StatisticMaterial>()

    fun blockBreak(type: Material,
                   data: Int) {
        for (statisticMaterial in statisticMaterials) {
            if (statisticMaterial.type() === type && statisticMaterial.data() == data) {
                statisticMaterial.blockBreak()
                return
            }
        }
        statisticMaterials.add(StatisticMaterial(type, data, 1, 0, 0))
    }

    fun blockCraft(type: Material,
                   data: Int) {
        for (statisticMaterial in statisticMaterials) {
            if (statisticMaterial.type() === type && statisticMaterial.data() == data) {
                statisticMaterial.blockCraft()
                return
            }
        }
        statisticMaterials.add(StatisticMaterial(type, data, 0, 0, 1))
    }

    fun blockPlace(type: Material,
                   data: Int) {
        for (statisticMaterial in statisticMaterials) {
            if (statisticMaterial.type() === type && statisticMaterial.data() == data) {
                statisticMaterial.blockPlace()
                return
            }
        }
        statisticMaterials.add(StatisticMaterial(type, data, 0, 1, 0))
    }

    fun statisticMaterials(): List<StatisticMaterial> {
        return Collections.unmodifiableList(statisticMaterials)
    }

    fun load(registry: GameRegistry,
             tagStructures: List<TagStructure>) {
        statisticMaterials.addAll(
                tagStructures.stream().map { tagStructure ->
                    StatisticMaterial(
                            registry.material(
                                    tagStructure.getInt(
                                            "ID")) ?: registry.air(),
                            tagStructure.getInt("Data") ?: 0,
                            tagStructure.getInt("BreakAmount") ?: 0,
                            tagStructure.getInt("PlaceAmount") ?: 0,
                            tagStructure.getInt("CraftAmount") ?: 0)
                }.collect(Collectors.toList<StatisticMaterial>()))
    }

    fun save(): List<TagStructure> {
        val tagStructures = ArrayList<TagStructure>()
        for (statisticMaterial in statisticMaterials) {
            val tagStructure = TagStructure()
            tagStructure.setInt("ID", statisticMaterial.type().itemID())
            tagStructure.setInt("Data", statisticMaterial.data)
            tagStructure.setInt("BreakAmount",
                    statisticMaterial.breakAmount)
            tagStructure.setInt("PlaceAmount",
                    statisticMaterial.placeAmount)
            tagStructure.setInt("CraftAmount",
                    statisticMaterial.craftAmount)
            tagStructures.add(tagStructure)
        }
        return tagStructures
    }

    class StatisticMaterial(val type: Material, val data: Int, var breakAmount: Int,
                            var placeAmount: Int, var craftAmount: Int) {

        fun blockBreak() {
            breakAmount++
        }

        fun blockCraft() {
            craftAmount++
        }

        fun blockPlace() {
            placeAmount++
        }

        fun breakAmount(): Int {
            return breakAmount
        }

        fun craftAmount(): Int {
            return craftAmount
        }

        fun data(): Int {
            return data
        }

        fun placeAmount(): Int {
            return placeAmount
        }

        fun type(): Material {
            return type
        }
    }
}
