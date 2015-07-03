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
import org.tobi29.scapes.engine.utils.StringLongHash;
import org.tobi29.scapes.engine.utils.io.FileUtil;
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
        GuiComponentTextField name =
                new GuiComponentTextField(16, 100, 368, 30, 18, "New World");
        GuiComponentTextField seed =
                new GuiComponentTextField(16, 160, 368, 30, 18, "");
        GuiComponentTextButton environment =
                new GuiComponentTextButton(16, 200, 368, 30, 18, "Generator: " +
                        worldTypes.get(environmentID).getName());
        environment.addLeftClick(event -> {
            environmentID++;
            if (environmentID >= worldTypes.size()) {
                environmentID = 0;
            }
            addons.clear();
            environment.setText(
                    "Generator: " + worldTypes.get(environmentID).getName());
        });
        GuiComponentTextButton addonsButton =
                new GuiComponentTextButton(16, 240, 368, 30, 18, "Addons");
        addonsButton.addLeftClick(event -> {
            state.remove(this);
            state.add(new GuiAddons(state, this,
                    worldTypes.get(environmentID).getName(), plugins));
        });
        save.addLeftClick(event -> {
            if (name.getText().isEmpty()) {
                name.setText("New World");
            }
            try {
                Path save = path.resolve(
                        name.getText() + WorldFormat.getFilenameExtension());
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
                if (seed.getText().isEmpty()) {
                    randomSeed = new Random().nextLong();
                } else {
                    try {
                        randomSeed = Long.valueOf(seed.getText());
                    } catch (NumberFormatException e) {
                        randomSeed = StringLongHash.hash(seed.getText());
                    }
                }
                tagStructure.setLong("Seed", randomSeed);
                FileUtil.write(data, stream -> TagStructureBinary
                        .write(tagStructure, stream));
                Path pluginsDir = save.resolve("plugins");
                Files.createDirectories(pluginsDir);
                PluginFile worldType = worldTypes.get(environmentID);
                Files.copy(worldType.getFile(),
                        pluginsDir.resolve(worldType.getFile().getFileName()));
                for (PluginFile addon : addons) {
                    Files.copy(addon.getFile(),
                            pluginsDir.resolve(addon.getFile().getFileName()));
                }
                state.remove(this);
                previous.updateSaves();
                state.add(previous);
            } catch (IOException e) {
                LOGGER.error("Failed to create world: {}", e.toString());
            }
        });
        pane.add(new GuiComponentText(16, 80, 18, "Name:"));
        pane.add(name);
        pane.add(new GuiComponentText(16, 140, 18, "Seed:"));
        pane.add(seed);
        pane.add(environment);
        pane.add(addonsButton);
    }

    private class GuiAddons extends Gui {
        public GuiAddons(GameState state, GuiCreateWorld prev, String parent,
                List<PluginFile> plugins) {
            super(GuiAlignment.CENTER);
            GuiComponentVisiblePane pane =
                    new GuiComponentVisiblePane(200, 0, 400, 512);
            GuiComponentScrollPaneList scrollPane =
                    new GuiComponentScrollPaneList(16, 80, 368, 350, 70);
            GuiComponentTextButton back =
                    new GuiComponentTextButton(112, 466, 176, 30, 18, "Back");
            back.addLeftClick(event -> {
                state.remove(this);
                state.add(prev);
            });
            plugins.stream().filter(plugin -> plugin.getParent().equals(parent))
                    .forEach(plugin -> {
                        Element element = new Element(plugin);
                        scrollPane.add(element);
                    });
            pane.add(new GuiComponentText(16, 16, 32, "Addons"));
            pane.add(new GuiComponentSeparator(24, 64, 352, 2));
            pane.add(scrollPane);
            pane.add(new GuiComponentSeparator(24, 448, 352, 2));
            pane.add(back);
            add(pane);
        }

        private class Element extends GuiComponentPane {
            @SuppressWarnings("CanBeFinal")
            private boolean active;
            private Texture texture;

            public Element(PluginFile addon) {
                super(0, 0, 378, 70);
                try (ZipFile zip = new ZipFile(addon.getFile().toFile())) {
                    texture = new TextureFile(
                            zip.getInputStream(zip.getEntry("Icon.png")), 0,
                            TextureFilter.LINEAR, TextureFilter.LINEAR,
                            TextureWrap.CLAMP, TextureWrap.CLAMP);
                } catch (IOException e) {
                    texture = new TextureCustom(1, 1);
                }
                GuiComponentTextButton label =
                        new GuiComponentTextButton(70, 20, 200, 30, 18,
                                addon.getName());
                active = addons.contains(addon);
                GuiComponentTextButton edit =
                        new GuiComponentTextButton(280, 20, 30, 30, 18,
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
                add(new GuiComponentIcon(15, 15, 40, 40, texture));
                add(label);
                add(edit);
            }
        }
    }
}
