/*
 * Copyright 2012-2018 Tobi29
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

package org.tobi29.scapes.vanilla.basics.gui

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.tobi29.coroutines.Timer
import org.tobi29.coroutines.loopUntilCancel
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.vanilla.basics.entity.client.EntityBloomeryClient
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB
import org.tobi29.stdex.math.floorToInt

class GuiBloomeryInventory(
    container: EntityBloomeryClient,
    player: MobPlayerClientMainVB,
    style: GuiStyle
) : GuiContainerInventory<EntityBloomeryClient>(
    "Bloomery", player, container, style
) {
    private val temperatureText: GuiComponentText
    private val bellowsText: GuiComponentText
    private var updateJob: Job? = null

    init {
        topPane.spacer()
        val bar1 = topPane.addVert(32.0, 0.0, -1.0, 40.0) {
            GuiComponentGroupSlab(it)
        }
        bar1.addHori(5.0, 5.0, 30.0, 30.0) {
            buttonContainer(it, "Container", 4)
        }
        val bar2 = topPane.addVert(32.0, 0.0, -1.0, 40.0) {
            GuiComponentGroupSlab(it)
        }
        for (i in 5..13) {
            bar2.addHori(5.0, 5.0, 30.0, 30.0) {
                buttonContainer(it, "Container", i)
            }
        }
        val bar3 = topPane.addVert(32.0, 0.0, -1.0, 40.0) {
            GuiComponentGroupSlab(it)
        }
        temperatureText = bar3.addHori(10.0, 10.0, 100.0, 16.0) {
            GuiComponentText(it, "")
        }
        bellowsText = bar3.addHori(10.0, 10.0, -1.0, 16.0) {
            GuiComponentText(it, "No bellows attached!")
        }
        val bar4 = topPane.addVert(32.0, 0.0, -1.0, 40.0) {
            GuiComponentGroupSlab(it)
        }
        for (i in 0..3) {
            bar4.addHori(5.0, 5.0, 30.0, 30.0) {
                buttonContainer(it, "Container", i)
            }
        }
        topPane.spacer()
        updateTemperatureText()
    }

    override fun updateVisible() {
        synchronized(this) {
            updateJob?.cancel()
            if (!isVisible) return@synchronized
            updateJob = launch(engine.taskExecutor) {
                Timer().apply { init() }.loopUntilCancel(Timer.toDiff(60.0)) {
                    updateTemperatureText()
                    bellowsText.visible = !container.hasBellows
                }
            }
        }
    }

    private fun updateTemperatureText() {
        temperatureText.text = "${container.temperature.floorToInt()}°C"
    }
}
