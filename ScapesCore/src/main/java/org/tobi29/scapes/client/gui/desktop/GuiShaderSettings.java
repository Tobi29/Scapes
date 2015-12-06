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
package org.tobi29.scapes.client.gui.desktop;

import org.tobi29.scapes.client.gui.desktop.GuiMenu;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentSlider;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiShaderSettings extends GuiMenu {
    public GuiShaderSettings(GameState state, Gui previous, GuiStyle style) {
        super(state, "Shader Settings", previous, style);
        TagStructure scapesTag =
                state.engine().tagStructure().getStructure("Scapes");
        GuiComponentSlider animationDistance = pane.addVert(16, 5,
                p -> new GuiComponentSlider(p, 368, 30, 18,
                        "Animation Distance",
                        scapesTag.getFloat("AnimationDistance")));
        GuiComponentTextButton bloom;
        if (scapesTag.getBoolean("Bloom")) {
            bloom = pane.addVert(16, 5, p -> button(p, 368, "Bloom: ON"));
        } else {
            bloom = pane.addVert(16, 5, p -> button(p, 368, "Bloom: OFF"));
        }
        GuiComponentTextButton fxaa;
        if (scapesTag.getBoolean("FXAA")) {
            fxaa = pane.addVert(16, 5, p -> button(p, 368, "FXAA: ON"));
        } else {
            fxaa = pane.addVert(16, 5, p -> button(p, 368, "FXAA: OFF"));
        }

        animationDistance.onDragLeft(event -> scapesTag
                .setFloat("AnimationDistance",
                        (float) animationDistance.value()));
        bloom.onClickLeft(event -> {
            if (!scapesTag.getBoolean("Bloom")) {
                bloom.setText("Bloom: ON");
                scapesTag.setBoolean("Bloom", true);
            } else {
                bloom.setText("Bloom: OFF");
                scapesTag.setBoolean("Bloom", false);
            }
        });
        fxaa.onClickLeft(event -> {
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
