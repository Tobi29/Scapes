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
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.opengl.texture.*;
import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            List<PluginFile> worldTypes, List<PluginFile> plugins, Path path) {
        super(state, "New World", previous);
        new GuiComponentText(pane, 16, 80, 18, "Name:");
        GuiComponentTextField name =
                new GuiComponentTextField(pane, 16, 100, 368, 30, 18,
                        "New World");
        new GuiComponentText(pane, 16, 140, 18, "Seed:");
        GuiComponentTextField seed =
                new GuiComponentTextField(pane, 16, 160, 368, 30, 18, "");
        GuiComponentTextButton environment =
                new GuiComponentTextButton(pane, 16, 200, 368, 30, 18,
                        "Generator: " + worldTypes.get(environmentID).name());
        GuiComponentTextButton addonsButton =
                new GuiComponentTextButton(pane, 16, 240, 368, 30, 18,
                        "Addons");

        environment.addLeftClick(event -> {
            environmentID++;
            if (environmentID >= worldTypes.size()) {
                environmentID = 0;
            }
            addons.clear();
            environment.setText(
                    "Generator: " + worldTypes.get(environmentID).name());
        });
        addonsButton.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiAddons(state, this,
                    worldTypes.get(environmentID).name(), plugins));
        });
        save.addLeftClick(event -> {
            if (name.text().isEmpty()) {
                name.setText("New World");
            }
            try {
                Path save = path.resolve(
                        name.text() + WorldFormat.filenameExtension());
                if (Files.exists(save)) {
                    state.remove(this);
                    state.add(
                            new GuiMessage(state, this, "Error", SAVE_EXISTS));
                    return;
                }
                Files.createDirectories(save);
                Path data = save.resolve("Data.stag");
                TagStructure tagStructure = new TagStructure();
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
                tagStructure.setLong("Seed", randomSeed);
                FileUtil.write(data, stream -> TagStructureBinary
                        .write(tagStructure, stream));
                Path pluginsDir = save.resolve("plugins");
                Files.createDirectories(pluginsDir);
                PluginFile worldType = worldTypes.get(environmentID);
                Files.copy(worldType.file(),
                        pluginsDir.resolve(worldType.file().getFileName()));
                for (PluginFile addon : addons) {
                    Files.copy(addon.file(),
                            pluginsDir.resolve(addon.file().getFileName()));
                }
                state.remove(this);
                previous.updateSaves();
                state.add(previous);
            } catch (IOException e) {
                LOGGER.error("Failed to create world: {}", e.toString());
            }
        });
    }

    private class GuiAddons extends Gui {
        public GuiAddons(GameState state, GuiCreateWorld prev, String parent,
                List<PluginFile> plugins) {
            super(GuiAlignment.CENTER);
            new GuiComponentText(pane, 16, 16, 32, "Addons");
            new GuiComponentSeparator(pane, 24, 64, 352, 2);
            GuiComponentVisiblePane pane =
                    new GuiComponentVisiblePane(this, 200, 0, 400, 512);
            GuiComponentScrollPaneViewport scrollPane =
                    new GuiComponentScrollPaneList(pane, 16, 80, 368, 350, 70)
                            .viewport();
            GuiComponentTextButton back =
                    new GuiComponentTextButton(pane, 112, 466, 176, 30, 18,
                            "Back");
            plugins.stream().filter(plugin -> plugin.parent().equals(parent))
                    .forEach(plugin -> new Element(scrollPane, plugin));
            new GuiComponentSeparator(pane, 24, 448, 352, 2);

            back.addLeftClick(event -> {
                state.remove(this);
                state.add(prev);
            });
        }

        private class Element extends GuiComponentPane {
            @SuppressWarnings("CanBeFinal")
            private boolean active;
            private Texture texture;

            public Element(GuiComponent parent, PluginFile addon) {
                super(parent, 0, 0, 378, 70);
                try (ZipFile zip = new ZipFile(addon.file().toFile())) {
                    texture = new TextureFile(
                            zip.getInputStream(zip.getEntry("Icon.png")), 0,
                            TextureFilter.LINEAR, TextureFilter.LINEAR,
                            TextureWrap.CLAMP, TextureWrap.CLAMP);
                } catch (IOException e) {
                    texture = new TextureCustom(1, 1);
                }
                new GuiComponentTextButton(this, 70, 20, 200, 30, 18,
                        addon.name());
                active = addons.contains(addon);
                GuiComponentTextButton edit =
                        new GuiComponentTextButton(this, 280, 20, 30, 30, 18,
                                active ? "X" : "");
                edit.addLeftClick(event -> {
                    active = !active;
                    if (active) {
                        edit.setText("X");
                        addons.add(addon);
                    } else {
                        edit.setText("");
                        addons.remove(addon);
                    }
                });
                new GuiComponentIcon(this, 15, 15, 40, 40, texture);
            }
        }
    }
}
