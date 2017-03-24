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

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiComponentTextButton
import org.tobi29.scapes.engine.gui.GuiEvent
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.utils.tag.mapMut
import org.tobi29.scapes.engine.utils.tag.set
import org.tobi29.scapes.engine.utils.tag.toBoolean

class GuiShaderSettings(state: GameState,
                        previous: Gui,
                        style: GuiStyle) : GuiMenuSingle(
        state, "Shader Settings", previous, style) {
    init {
        val scapesTag = state.engine.configMap.mapMut("Scapes")
        val animations: GuiComponentTextButton
        if (scapesTag["Animations"]?.toBoolean() ?: false) {
            animations = row(pane) { button(it, "Animations: ON") }
        } else {
            animations = row(pane) { button(it, "Animations: OFF") }
        }
        val bloom: GuiComponentTextButton
        if (scapesTag["Bloom"]?.toBoolean() ?: false) {
            bloom = row(pane) { button(it, "Bloom: ON") }
        } else {
            bloom = row(pane) { button(it, "Bloom: OFF") }
        }
        val autoExposure: GuiComponentTextButton
        if (scapesTag["AutoExposure"]?.toBoolean() ?: false) {
            autoExposure = row(pane) { button(it, "Auto Exposure: ON") }
        } else {
            autoExposure = row(pane) { button(it, "Auto Exposure: OFF") }
        }
        val fxaa: GuiComponentTextButton
        if (scapesTag["FXAA"]?.toBoolean() ?: false) {
            fxaa = row(pane) { button(it, "FXAA: ON") }
        } else {
            fxaa = row(pane) { button(it, "FXAA: OFF") }
        }

        selection(animations)
        selection(bloom)
        selection(autoExposure)
        selection(fxaa)

        animations.on(GuiEvent.CLICK_LEFT) { event ->
            if (scapesTag["Animations"]?.toBoolean() ?: false) {
                animations.setText("Animations: OFF")
                scapesTag["Animations"] = false
            } else {
                animations.setText("Animations: ON")
                scapesTag["Animations"] = true
            }
        }
        bloom.on(GuiEvent.CLICK_LEFT) { event ->
            if (scapesTag["Bloom"]?.toBoolean() ?: false) {
                bloom.setText("Bloom: OFF")
                scapesTag["Bloom"] = false
            } else {
                bloom.setText("Bloom: ON")
                scapesTag["Bloom"] = true
            }
        }
        autoExposure.on(GuiEvent.CLICK_LEFT) { event ->
            if (scapesTag["AutoExposure"]?.toBoolean() ?: false) {
                autoExposure.setText("Auto Exposure: OFF")
                scapesTag["AutoExposure"] = false
            } else {
                autoExposure.setText("Auto Exposure: ON")
                scapesTag["AutoExposure"] = true
            }
        }
        fxaa.on(GuiEvent.CLICK_LEFT) { event ->
            if (scapesTag["FXAA"]?.toBoolean() ?: false) {
                fxaa.setText("FXAA: OFF")
                scapesTag["FXAA"] = false
            } else {
                fxaa.setText("FXAA: ON")
                scapesTag["FXAA"] = true
            }
        }
    }
}
