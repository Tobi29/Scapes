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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiComponentTextButton
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle

class GuiShaderSettings(state: GameState,
                        previous: Gui,
                        style: GuiStyle) : GuiMenuSingle(
        state, "Shader Settings", previous, style) {
    init {
        val scapes = engine.component(ScapesClient.COMPONENT)
        val animations: GuiComponentTextButton
        if (scapes.animations) {
            animations = row(pane) { button(it, "Animations: ON") }
        } else {
            animations = row(pane) { button(it, "Animations: OFF") }
        }
        val bloom: GuiComponentTextButton
        if (scapes.bloom) {
            bloom = row(pane) { button(it, "Bloom: ON") }
        } else {
            bloom = row(pane) { button(it, "Bloom: OFF") }
        }
        val autoExposure: GuiComponentTextButton
        if (scapes.autoExposure) {
            autoExposure = row(pane) { button(it, "Auto Exposure: ON") }
        } else {
            autoExposure = row(pane) { button(it, "Auto Exposure: OFF") }
        }
        val fxaa: GuiComponentTextButton
        if (scapes.fxaa) {
            fxaa = row(pane) { button(it, "FXAA: ON") }
        } else {
            fxaa = row(pane) { button(it, "FXAA: OFF") }
        }

        selection(animations)
        selection(bloom)
        selection(autoExposure)
        selection(fxaa)

        animations.on(GuiEvent.CLICK_LEFT) {
            if (scapes.animations) {
                animations.setText("Animations: OFF")
                scapes.animations = false
            } else {
                animations.setText("Animations: ON")
                scapes.animations = true
            }
        }
        bloom.on(GuiEvent.CLICK_LEFT) {
            if (scapes.bloom) {
                bloom.setText("Bloom: OFF")
                scapes.bloom = false
            } else {
                bloom.setText("Bloom: ON")
                scapes.bloom = true
            }
        }
        autoExposure.on(GuiEvent.CLICK_LEFT) {
            if (scapes.autoExposure) {
                autoExposure.setText("Auto Exposure: OFF")
                scapes.autoExposure = false
            } else {
                autoExposure.setText("Auto Exposure: ON")
                scapes.autoExposure = true
            }
        }
        fxaa.on(GuiEvent.CLICK_LEFT) {
            if (scapes.fxaa) {
                fxaa.setText("FXAA: OFF")
                scapes.fxaa = false
            } else {
                fxaa.setText("FXAA: ON")
                scapes.fxaa = true
            }
        }
    }
}
