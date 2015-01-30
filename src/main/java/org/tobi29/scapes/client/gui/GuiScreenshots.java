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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureFile;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;

import java.io.IOException;
import java.util.List;

public class GuiScreenshots extends Gui {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiScreenshots.class);
    private final GameState state;
    private final GuiComponentScrollPaneList scrollPane;
    private List<File> files;
    private int i;

    public GuiScreenshots(GameState state, Gui prev) {
        super(GuiAlignment.CENTER);
        this.state = state;
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(200, 0, 400, 512);
        scrollPane = new GuiComponentScrollPaneList(16, 80, 368, 350, 70);
        GuiComponentTextButton back =
                new GuiComponentTextButton(112, 466, 176, 30, 18, "Back");
        back.addLeftClick(event -> {
            state.remove(this);
            state.add(prev);
        });
        pane.add(new GuiComponentText(16, 16, 32, "Screenshots"));
        pane.add(new GuiComponentSeparator(24, 64, 352, 2));
        pane.add(scrollPane);
        pane.add(new GuiComponentSeparator(24, 448, 352, 2));
        pane.add(back);
        add(pane);
        try {
            Directory directory = state.getEngine().getFiles()
                    .getDirectory("File:screenshots");
            files = directory
                    .listFiles(file -> file.getName().endsWith(".png"));
        } catch (IOException e) {
            LOGGER.warn("Failed to read screenshots: {}", e.toString());
        }
    }

    @Override
    public void update(double mouseX, double mouseY, boolean mouseInside,
            ScapesEngine engine) {
        super.update(mouseX, mouseY, mouseInside, engine);
        if (files != null) {
            if (i == files.size()) {
                files = null;
            } else {
                File file = files.get(i++);
                try {
                    Element element =
                            new Element(file, new TextureFile(file.read(), 0),
                                    this);
                    scrollPane.add(element);
                } catch (IOException e) {
                    LOGGER.warn("Failed to load screenshot: {}", e.toString());
                }
            }
        }
    }

    private class Element extends GuiComponentPane {
        public Element(File file, Texture texture, GuiScreenshots gui) {
            super(0, 0, 378, 70);
            GuiComponentIcon icon =
                    new GuiComponentIcon(15, 20, 40, 30, texture);
            icon.addLeftClick(event -> {
                state.remove(gui);
                state.add(new GuiScreenshot(state, gui, texture));
            });
            GuiComponentTextButton label =
                    new GuiComponentTextButton(70, 20, 100, 30, 18, "Save");
            label.addLeftClick(event -> {
                try {
                    file.exportToUser(new PlatformDialogs.Extension[]{
                                    new PlatformDialogs.Extension("*.png",
                                            "PNG Picture")},
                            "Export screenshot",
                            state.getEngine().getGraphics().getContainer());
                } catch (IOException e) {
                    LOGGER.warn("Failed to export screenshot: {}",
                            e.toString());
                }
            });
            GuiComponentTextButton edit =
                    new GuiComponentTextButton(180, 20, 100, 30, 18, "Delete");
            edit.addLeftClick(event -> {
                try {
                    file.delete();
                    scrollPane.remove(this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete screenshot: {}",
                            e.toString());
                }
            });
            add(icon);
            add(label);
            add(edit);
        }
    }
}
