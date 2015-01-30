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
import org.tobi29.scapes.engine.utils.math.FastMath;

public class GuiVideoSettings extends Gui {
    public GuiVideoSettings(GameState state, Gui prev) {
        super(GuiAlignment.CENTER);
        TagStructure scapesTag =
                state.getEngine().getTagStructure().getStructure("Scapes");
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(200, 0, 400, 512);
        GuiComponentSlider viewDistance =
                new GuiComponentSlider(16, 80, 368, 30, 18, "View distance",
                        scapesTag.getFloat("RenderDistance"),
                        (text, value) -> text + ": " +
                                (int) (10 + value * 246) +
                                'm');
        viewDistance.addHover(event -> scapesTag
                .setFloat("RenderDistance", (float) viewDistance.value));
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
                        1.0f / state.getEngine().getConfig()
                                .getResolutionMultiplier());
        resolutionMultiplier.addHover(event -> {
            state.getEngine().getConfig().setResolutionMultiplier((int) (1.0f /
                    FastMath.clamp(resolutionMultiplier.value, 0.1, 1)));
            resolutionMultiplier.value = 1.0f /
                    state.getEngine().getConfig().getResolutionMultiplier();
        });
        GuiComponentTextButton back =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Back");
        back.addLeftClick(event -> {
            state.remove(this);
            state.add(prev);
        });
        pane.add(new GuiComponentText(16, 16, 32, "Video settings"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(viewDistance);
        pane.add(shader);
        pane.add(fullscreen);
        pane.add(keepInvisibleVbos);
        pane.add(resolutionMultiplier);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(back);
        add(pane);
    }
}
