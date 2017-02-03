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

package scapes.plugin.tobi29.vanilla.basics

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.block.Update
import scapes.plugin.tobi29.vanilla.basics.material.update.*

internal fun registerUpdates(registry: GameRegistry) {
    registry.getSupplier<GameRegistry, Update>("Core", "Update").run {
        reg({ UpdateWaterFlow() }, UpdateWaterFlow::class.java,
                "vanilla.basics.update.WaterFlow")
        reg({ UpdateLavaFlow() }, UpdateLavaFlow::class.java,
                "vanilla.basics.update.LavaFlow")
        reg({ UpdateGrassGrowth() }, UpdateGrassGrowth::class.java,
                "vanilla.basics.update.GrassGrowth")
        reg({ UpdateFlowerGrowth() }, UpdateFlowerGrowth::class.java,
                "vanilla.basics.update.FlowerGrowth")
        reg({ UpdateSaplingGrowth() }, UpdateSaplingGrowth::class.java,
                "vanilla.basics.update.SaplingGrowth")
        reg({ UpdateStrawDry() }, UpdateStrawDry::class.java,
                "vanilla.basics.update.StrawDry")
    }
}