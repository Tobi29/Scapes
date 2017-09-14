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
package org.tobi29.scapes.vanilla.basics.gui

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.gui.GuiComponentGroup
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAlloyClient
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB

class GuiAlloyInventory(container: EntityAlloyClient,
                        player: MobPlayerClientMainVB,
                        style: GuiStyle) : GuiContainerInventory<EntityAlloyClient>(
        "Alloy Mold", player, container, style) {
    private val infoText: GuiComponentText

    init {
        topPane.spacer()
        val bar = topPane.addVert(32.0, 0.0, -1.0, 80.0, ::GuiComponentGroupSlab)
        bar.addHori(0.0, 0.0, 40.0, -1.0, ::GuiComponentGroup).apply {
            addVert(5.0, 5.0, 30.0, 30.0) {
                buttonContainer(it, "Container", 0)
            }
            addVert(5.0, 5.0, 30.0, 30.0) {
                buttonContainer(it, "Container", 1)
            }
        }
        infoText = bar.addHori(16.0, 5.0, -1.0, 16.0) {
            GuiComponentText(it, "")
        }
        topPane.spacer()
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
        val alloy = container.alloy
        if (alloy.metals.isNotEmpty()) {
            text.append("Metal: ").append(alloy.type(
                    container.world.plugins.plugin(
                            "VanillaBasics") as VanillaBasics).name)
            alloy.metals.forEach {
                text.append('\n').append(it.key.name).append(" - ").append(
                        round(it.value * 100.0) / 100.0)
            }
        } else {
            text.append("Insert molten metal on top\nslot, extract below.")
        }
        infoText.text = text.toString()
    }
}
