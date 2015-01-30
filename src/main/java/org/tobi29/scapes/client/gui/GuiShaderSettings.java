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
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class GuiShaderSettings extends Gui {
    public GuiShaderSettings(GameState state, Gui previous) {
        super(GuiAlignment.CENTER);
        TagStructure scapesTag =
                state.getEngine().getTagStructure().getStructure("Scapes");
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentSlider animationDistance =
                new GuiComponentSlider(16, 80, 368, 30, 18,
                        "Animation Distance",
                        scapesTag.getFloat("AnimationDistance"));
        animationDistance.addHover(event -> scapesTag
                .setFloat("AnimationDistance",
                        (float) animationDistance.value));
        GuiComponentTextButton bloom;
        if (scapesTag.getBoolean("Bloom")) {
            bloom = new GuiComponentTextButton(16, 120, 368, 30, 18,
                    "Bloom: ON");
        } else {
            bloom = new GuiComponentTextButton(16, 120, 368, 30, 18,
                    "Bloom: OFF");
        }
        bloom.addLeftClick(event -> {
            if (!scapesTag.getBoolean("Bloom")) {
                bloom.setText("Bloom: ON");
                scapesTag.setBoolean("Bloom", true);
            } else {
                bloom.setText("Bloom: OFF");
                scapesTag.setBoolean("Bloom", false);
            }
        });
        GuiComponentTextButton fxaa;
        if (scapesTag.getBoolean("FXAA")) {
            fxaa = new GuiComponentTextButton(16, 160, 368, 30, 18, "FXAA: ON");
        } else {
            fxaa = new GuiComponentTextButton(16, 160, 368, 30, 18,
                    "FXAA: OFF");
        }
        fxaa.addLeftClick(event -> {
            if (!scapesTag.getBoolean("FXAA")) {
                fxaa.setText("FXAA: ON");
                scapesTag.setBoolean("FXAA", true);
            } else {
                fxaa.setText("FXAA: OFF");
                scapesTag.setBoolean("FXAA", false);
            }
        });
        GuiComponentTextButton back =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Back");
        back.addLeftClick(event -> {
            state.remove(this);
            state.add(previous);
        });
        pane.add(new GuiComponentText(16, 16, 32, "Shader settings"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(animationDistance);
        pane.add(bloom);
        pane.add(fxaa);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(back);
        add(pane);
    }
}
