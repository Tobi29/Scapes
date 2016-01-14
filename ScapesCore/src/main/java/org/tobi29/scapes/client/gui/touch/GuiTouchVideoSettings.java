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
package org.tobi29.scapes.client.gui.touch;

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiComponentSlider;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiTouchVideoSettings extends GuiTouchMenu {
    public GuiTouchVideoSettings(GameState state, Gui previous,
            GuiStyle style) {
        super(state, "Video Settings", previous, style);
        TagStructure scapesTag =
                state.engine().tagStructure().getStructure("Scapes");
        GuiComponentSlider viewDistance = row(pane,
                p -> slider(p, "View distance",
                        (scapesTag.getDouble("RenderDistance") - 10.0) / 246.0,
                        (text, value) -> text + ": " +
                                FastMath.round(10.0 + value * 246.0) + 'm'));
        GuiComponentSlider animationDistance = row(pane,
                p -> slider(p, "Animation Distance",
                        scapesTag.getFloat("AnimationDistance")));
        GuiComponentSlider resolutionMultiplier = row(pane,
                p -> slider(p, "Resolution", reverseResolution(
                        state.engine().config().resolutionMultiplier()),
                        (text, value) -> text + ": " +
                                FastMath.round(resolution(value) * 100.0) +
                                '%'));

        viewDistance.onDragLeft(event -> scapesTag.setDouble("RenderDistance",
                10.0 + viewDistance.value() * 246.0));
        animationDistance.onDragLeft(event -> scapesTag
                .setFloat("AnimationDistance",
                        (float) animationDistance.value()));
        resolutionMultiplier.onDragLeft(event -> state.engine().config()
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
