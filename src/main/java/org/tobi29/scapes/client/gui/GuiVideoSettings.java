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
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiVideoSettings extends GuiMenu {
    public GuiVideoSettings(GameState state, Gui previous, GuiStyle style) {
        super(state, "Video Settings", previous, style);
        TagStructure scapesTag =
                state.engine().tagStructure().getStructure("Scapes");
        GuiComponentSlider viewDistance = pane.addVert(16, 5,
                p -> new GuiComponentSlider(p, 368, 30, 18, "View distance",
                        (scapesTag.getDouble("RenderDistance") - 10.0) / 246.0,
                        (text, value) -> text + ": " +
                                FastMath.round(10.0 + value * 246.0) + 'm'));
        GuiComponentTextButton shader = pane.addVert(16, 5,
                p -> new GuiComponentTextButton(p, 368, 30, 18, "Shaders"));
        GuiComponentTextButton fullscreen;
        if (state.engine().config().isFullscreen()) {
            fullscreen = pane.addVert(16, 5,
                    p -> new GuiComponentTextButton(p, 368, 30, 18,
                            "Fullscreen: ON"));
        } else {
            fullscreen = pane.addVert(16, 5,
                    p -> new GuiComponentTextButton(p, 368, 30, 18,
                            "Fullscreen: OFF"));
        }
        GuiComponentSlider resolutionMultiplier = pane.addVert(16, 5,
                p -> new GuiComponentSlider(p, 368, 30, 18, "Resolution",
                        reverseResolution(
                                state.engine().config().resolutionMultiplier()),
                        (text, value) -> text + ": " +
                                FastMath.round(resolution(value) * 100.0) +
                                '%'));

        viewDistance.addHover(event -> scapesTag.setDouble("RenderDistance",
                10.0 + viewDistance.value() * 246.0));
        shader.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiShaderSettings(state, this, style));
        });
        fullscreen.addLeftClick(event -> {
            if (!state.engine().config().isFullscreen()) {
                fullscreen.setText("Fullscreen: ON");
                state.engine().config().setFullscreen(true);
            } else {
                fullscreen.setText("Fullscreen: OFF");
                state.engine().config().setFullscreen(false);
            }
            state.engine().container().updateContainer();
        });
        resolutionMultiplier.addHover(event -> state.engine().config()
                .setResolutionMultiplier(
                        (float) resolution(resolutionMultiplier.value())));
    }

    private static double resolution(double value) {
        return 1.0 / FastMath.round(11.0 - value * 10.0);
    }

    private static double reverseResolution(double value) {
        return 1.1 - 0.1 / value;
    }
}
