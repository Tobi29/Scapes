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

package org.tobi29.scapes.client.gui;

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentSlider;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiShaderSettings extends GuiMenu {
    public GuiShaderSettings(GameState state, Gui previous) {
        super(state, "Shader Settings", previous);
        TagStructure scapesTag =
                state.engine().tagStructure().getStructure("Scapes");
        GuiComponentSlider animationDistance =
                new GuiComponentSlider(pane, 16, 80, 368, 30, 18,
                        "Animation Distance",
                        scapesTag.getFloat("AnimationDistance"));
        GuiComponentTextButton bloom;
        if (scapesTag.getBoolean("Bloom")) {
            bloom = new GuiComponentTextButton(pane, 16, 120, 368, 30, 18,
                    "Bloom: ON");
        } else {
            bloom = new GuiComponentTextButton(pane, 16, 120, 368, 30, 18,
                    "Bloom: OFF");
        }
        GuiComponentTextButton fxaa;
        if (scapesTag.getBoolean("FXAA")) {
            fxaa = new GuiComponentTextButton(pane, 16, 160, 368, 30, 18,
                    "FXAA: ON");
        } else {
            fxaa = new GuiComponentTextButton(pane, 16, 160, 368, 30, 18,
                    "FXAA: OFF");
        }

        animationDistance.addHover(event -> scapesTag
                .setFloat("AnimationDistance",
                        (float) animationDistance.value));
        bloom.addLeftClick(event -> {
            if (!scapesTag.getBoolean("Bloom")) {
                bloom.setText("Bloom: ON");
                scapesTag.setBoolean("Bloom", true);
            } else {
                bloom.setText("Bloom: OFF");
                scapesTag.setBoolean("Bloom", false);
            }
        });
        fxaa.addLeftClick(event -> {
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
