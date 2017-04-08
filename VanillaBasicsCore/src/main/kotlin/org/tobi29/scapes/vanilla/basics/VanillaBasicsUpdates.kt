/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.vanilla.basics

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.block.UpdateType
import org.tobi29.scapes.vanilla.basics.material.update.*

internal fun registerUpdates(registry: Registries) {
    registry.get<UpdateType>("Core", "Update").run {
        reg("vanilla.basics.update.WaterFlow") {
            UpdateType(it, ::UpdateWaterFlow)
        }
        reg("vanilla.basics.update.LavaFlow") {
            UpdateType(it, ::UpdateLavaFlow)
        }
        reg("vanilla.basics.update.GrassGrowth") {
            UpdateType(it, ::UpdateGrassGrowth)
        }
        reg("vanilla.basics.update.FlowerGrowth") {
            UpdateType(it, ::UpdateFlowerGrowth)
        }
        reg("vanilla.basics.update.SaplingGrowth") {
            UpdateType(it, ::UpdateSaplingGrowth)
        }
        reg("vanilla.basics.update.StrawDry") {
            UpdateType(it, ::UpdateStrawDry)
        }
    }
}