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
package scapes.plugin.tobi29.vanilla.basics.gui

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.entity.client.EntityAlloyClient
import scapes.plugin.tobi29.vanilla.basics.entity.client.MobPlayerClientMainVB

class GuiAlloyInventory(container: EntityAlloyClient, player: MobPlayerClientMainVB, style: GuiStyle) : GuiContainerInventory<EntityAlloyClient>(
        "Alloy Mold", player, container, style) {
    private val infoText: GuiComponentText

    init {
        selection(buttonContainer(16, 120, 30, 30, 0))
        selection(buttonContainer(16, 160, 30, 30, 1))
        infoText = pane.add(60.0, 80.0, -1.0, 24.0) {
            GuiComponentText(it, "")
        }
        updateInfoText()
    }

    override fun renderOverlay(gl: GL,
                               shader: Shader,
                               pixelSize: Vector2d) {
        super.renderOverlay(gl, shader, pixelSize)
        updateInfoText()
    }

    private fun updateInfoText() {
        val text = StringBuilder(64)
        val alloy = container.alloy()
        if (alloy.metals.isNotEmpty()) {
            text.append("Metal: ").append(alloy.type(
                    container.world.plugins.plugin(
                            "VanillaBasics") as VanillaBasics).name())
            alloy.metals.forEach { entry ->
                text.append('\n').append(entry.key.name()).append(
                        " - ").append(
                        round(entry.value * 100.0) / 100.0)
            }
        } else {
            text.append("Insert molten metal on top\nslot, extract below.")
        }
        infoText.text = text.toString()
    }
}