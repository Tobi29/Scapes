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
package org.tobi29.scapes.client.gui.touch;

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiEvent;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiTouchShaderSettings extends GuiTouchMenu {
    public GuiTouchShaderSettings(GameState state, Gui previous,
            GuiStyle style) {
        super(state, "Shader Settings", previous, style);
        TagStructure scapesTag =
                state.engine().tagStructure().getStructure("Scapes");
        GuiComponentTextButton animations;
        if (scapesTag.getBoolean("Animations")) {
            animations = row(pane, p -> button(p, "Animations: ON"));
        } else {
            animations = row(pane, p -> button(p, "Animations: OFF"));
        }
        GuiComponentTextButton bloom;
        if (scapesTag.getBoolean("Bloom")) {
            bloom = row(pane, p -> button(p, "Bloom: ON"));
        } else {
            bloom = row(pane, p -> button(p, "Bloom: OFF"));
        }
        GuiComponentTextButton autoExposure;
        if (scapesTag.getBoolean("AutoExposure")) {
            autoExposure = row(pane, p -> button(p, "Auto Exposure: ON"));
        } else {
            autoExposure = row(pane, p -> button(p, "Auto Exposure: OFF"));
        }
        GuiComponentTextButton fxaa;
        if (scapesTag.getBoolean("FXAA")) {
            fxaa = row(pane, p -> button(p, "FXAA: ON"));
        } else {
            fxaa = row(pane, p -> button(p, "FXAA: OFF"));
        }

        selection(animations);
        selection(bloom);
        selection(autoExposure);
        selection(fxaa);

        animations.on(GuiEvent.CLICK_LEFT, event -> {
            if (!scapesTag.getBoolean("Animations")) {
                animations.setText("Animations: ON");
                scapesTag.setBoolean("Animations", true);
            } else {
                animations.setText("Animations: OFF");
                scapesTag.setBoolean("Animations", false);
            }
        });
        bloom.on(GuiEvent.CLICK_LEFT, event -> {
            if (!scapesTag.getBoolean("Bloom")) {
                bloom.setText("Bloom: ON");
                scapesTag.setBoolean("Bloom", true);
            } else {
                bloom.setText("Bloom: OFF");
                scapesTag.setBoolean("Bloom", false);
            }
        });
        autoExposure.on(GuiEvent.CLICK_LEFT, event -> {
            if (!scapesTag.getBoolean("AutoExposure")) {
                autoExposure.setText("Auto Exposure: ON");
                scapesTag.setBoolean("AutoExposure", true);
            } else {
                autoExposure.setText("Auto Exposure: OFF");
                scapesTag.setBoolean("AutoExposure", false);
            }
        });
        fxaa.on(GuiEvent.CLICK_LEFT, event -> {
            if (!scapesTag.getBoolean("FXAA")) {
                fxaa.setText("FXAA: ON");
                scapesTag.setBoolean("FXAA", true);
            } else {
                fxaa.setText("FXAA: OFF");
                scapesTag.setBoolean("FXAA", false);
            }
        });
    }
}
