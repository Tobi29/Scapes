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
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiVideoSettings extends GuiMenu {
    public GuiVideoSettings(GameState state, Gui previous) {
        super(state, "Video Settings", previous);
        TagStructure scapesTag =
                state.getEngine().getTagStructure().getStructure("Scapes");
        GuiComponentSlider viewDistance =
                new GuiComponentSlider(16, 80, 368, 30, 18, "View distance",
                        (scapesTag.getDouble("RenderDistance") - 10.0) / 246.0,
                        (text, value) -> text + ": " +
                                FastMath.round(10.0 + value * 246.0) +
                                'm');
        viewDistance.addHover(event -> scapesTag.setDouble("RenderDistance",
                10.0 + viewDistance.value * 246.0));
        GuiComponentTextButton shader =
                new GuiComponentTextButton(16, 120, 368, 30, 18, "Shaders");
        shader.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiShaderSettings(state, this));
        });
        GuiComponentTextButton fullscreen;
        if (state.getEngine().getConfig().isFullscreen()) {
            fullscreen = new GuiComponentTextButton(16, 160, 368, 30, 18,
                    "Fullscreen: ON");
        } else {
            fullscreen = new GuiComponentTextButton(16, 160, 368, 30, 18,
                    "Fullscreen: OFF");
        }
        fullscreen.addLeftClick(event -> {
            if (!state.getEngine().getConfig().isFullscreen()) {
                fullscreen.setText("Fullscreen: ON");
                state.getEngine().getConfig().setFullscreen(true);
                state.getEngine().getGraphics().getContainer()
                        .setFullscreen(true);
            } else {
                fullscreen.setText("Fullscreen: OFF");
                state.getEngine().getConfig().setFullscreen(false);
                state.getEngine().getGraphics().getContainer()
                        .setFullscreen(false);
            }
        });
        GuiComponentTextButton keepInvisibleVbos;
        if (scapesTag.getBoolean("KeepInvisibleChunkVbos")) {
            keepInvisibleVbos = new GuiComponentTextButton(16, 200, 368, 30, 18,
                    "Unload unused Chunk-Geom: OFF");
        } else {
            keepInvisibleVbos = new GuiComponentTextButton(16, 200, 368, 30, 18,
                    "Unload unused Chunk-Geom: ON");
        }
        keepInvisibleVbos.addLeftClick(event -> {
            if (scapesTag.getBoolean("KeepInvisibleChunkVbos")) {
                keepInvisibleVbos.setText("Unload unused Chunk-Geom: ON");
                scapesTag.setBoolean("KeepInvisibleChunkVbos", false);
            } else {
                keepInvisibleVbos.setText("Unload unused Chunk-Geom: OFF");
                scapesTag.setBoolean("KeepInvisibleChunkVbos", true);
            }
        });
        GuiComponentSlider resolutionMultiplier =
                new GuiComponentSlider(16, 240, 368, 30, 18, "Resolution",
                        reverseResolution(state.getEngine().getConfig()
                                .getResolutionMultiplier()),
                        (text, value) -> text + ": " +
                                FastMath.round(resolution(value) * 100.0) +
                                '%');
        resolutionMultiplier.addHover(event -> state.getEngine().getConfig()
                .setResolutionMultiplier(
                        (float) resolution(resolutionMultiplier.value)));
        pane.add(viewDistance);
        pane.add(shader);
        pane.add(fullscreen);
        pane.add(keepInvisibleVbos);
        pane.add(resolutionMultiplier);
    }

    private static double resolution(double value) {
        return 1.0 / FastMath.round(11.0 - value * 10.0);
    }

    private static double reverseResolution(double value) {
        return 1.1 - 0.1 / value;
    }
}
