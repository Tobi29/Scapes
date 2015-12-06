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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.SaveStorage;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.*;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipFile;

public class GuiCreateWorld extends GuiMenuDouble {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiCreateWorld.class);
    private static final String SAVE_EXISTS =
            "This save already exists!\n" + "Please choose a different name.";
    public final List<PluginFile> addons = new ArrayList<>();
    private int environmentID;

    public GuiCreateWorld(GameState state, GuiSaveSelect previous,
            List<PluginFile> worldTypes, List<PluginFile> plugins,
            GuiStyle style) {
        super(state, "New World", previous, style);
        ScapesClient game = (ScapesClient) state.engine().game();
        SaveStorage saves = game.saves();
        pane.addVert(16, 5, p -> new GuiComponentText(p, 18, "Name:"));
        GuiComponentTextField name = pane.addVert(16, 5,
                p -> new GuiComponentTextField(p, 368, 30, 18, "New World"));
        pane.addVert(16, 5, p -> new GuiComponentText(p, 18, "Seed:"));
        GuiComponentTextField seed = pane.addVert(16, 5,
                p -> new GuiComponentTextField(p, 368, 30, 18, ""));
        GuiComponentTextButton environment = pane.addVert(16, 5,
                p -> button(p, 368,
                        "Generator: " + worldTypes.get(environmentID).name()));
        GuiComponentTextButton addonsButton =
                pane.addVert(16, 5, p -> button(p, 368, "Addons"));

        environment.onClickLeft(event -> {
            environmentID++;
            if (environmentID >= worldTypes.size()) {
                environmentID = 0;
            }
            addons.clear();
            environment.setText(
                    "Generator: " + worldTypes.get(environmentID).name());
        });
        addonsButton.onClickLeft(event -> {
            state.engine().guiStack().add("10-Menu", new GuiAddons(state, this,
                    worldTypes.get(environmentID).name(), plugins, style));
        });
        save.onClickLeft(event -> {
            if (name.text().isEmpty()) {
                name.setText("New World");
            }
            try {
                String saveName = name.text();
                if (saves.exists(saveName)) {
                    state.engine().guiStack().add("10-Menu",
                            new GuiMessage(state, this, "Error", SAVE_EXISTS,
                                    style));
                    return;
                }
                long randomSeed;
                if (seed.text().isEmpty()) {
                    randomSeed = new Random().nextLong();
                } else {
                    try {
                        randomSeed = Long.valueOf(seed.text());
                    } catch (NumberFormatException e) {
                        randomSeed = StringUtil.hash(seed.text());
                    }
                }
                List<FilePath> pluginFiles = new ArrayList<>();
                PluginFile worldType = worldTypes.get(environmentID);
                pluginFiles.add(worldType.file());
                Streams.of(addons).map(PluginFile::file)
                        .forEach(pluginFiles::add);
                try (WorldSource source = saves.get(saveName)) {
                    source.init(randomSeed, pluginFiles);
                }
                previous.updateSaves();
                state.engine().guiStack().add("10-Menu", previous);
            } catch (IOException e) {
                LOGGER.error("Failed to create world: {}", e.toString());
            }
        });
    }

    @SuppressWarnings("AmbiguousFieldAccess")
    private class GuiAddons extends GuiMenu {
        public GuiAddons(GameState state, GuiCreateWorld previous,
                String parent, List<PluginFile> plugins, GuiStyle style) {
            super(state, "Apply", previous, style);
            pane.add(16, 16, p -> new GuiComponentText(p, 32, "Addons"));
            pane.add(24, 64, p -> new GuiComponentSeparator(p, 352, 2));
            GuiComponentScrollPaneViewport scrollPane = pane.add(16, 80,
                    p -> new GuiComponentScrollPane(p, 368, 350, 70))
                    .viewport();
            Streams.of(plugins).filter(plugin -> plugin.parent().equals(parent))
                    .forEach(plugin -> scrollPane
                            .addVert(0, 0, p -> new Element(p, plugin)));
        }

        private class Element extends GuiComponentPane {
            @SuppressWarnings("FieldMayBeFinal")
            private boolean active;

            public Element(GuiLayoutData parent, PluginFile addon) {
                super(parent, 378, 70);
                GuiComponentIcon icon =
                        add(15, 15, p -> new GuiComponentIcon(p, 40, 40));
                add(70, 20, p -> button(p, 200, addon.name()));
                GuiComponentTextButton edit =
                        add(280, 20, p -> button(p, 30, active ? "X" : ""));

                active = addons.contains(addon);
                edit.onClickLeft(event -> {
                    active = !active;
                    if (active) {
                        edit.setText("X");
                        addons.add(addon);
                    } else {
                        edit.setText("");
                        addons.remove(addon);
                    }
                });
                try (ZipFile zip = FileUtil.zipFile(addon.file())) {
                    Texture texture = new TextureFile(
                            zip.getInputStream(zip.getEntry("Icon.png")), 0,
                            TextureFilter.LINEAR, TextureFilter.LINEAR,
                            TextureWrap.CLAMP, TextureWrap.CLAMP);
                    icon.setIcon(texture);
                } catch (IOException e) {
                }
            }
        }
    }
}
