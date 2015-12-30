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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.client.SaveStorage;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.gui.desktop.GuiMessage;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiComponentTextField;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.plugins.PluginFile;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GuiTouchCreateWorld extends GuiTouchMenuDouble {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiTouchCreateWorld.class);
    private static final String SAVE_EXISTS =
            "This save already exists!\n" + "Please choose a different name.";

    public GuiTouchCreateWorld(GameState state, GuiTouchSaveSelect previous,
            PluginFile worldType, GuiStyle style) {
        super(state, "New World", previous, style);
        ScapesClient game = (ScapesClient) state.engine().game();
        SaveStorage saves = game.saves();
        pane.addVert(112, 10, p -> new GuiComponentText(p, 36, "Name:"));
        GuiComponentTextField name = pane.addVert(112, 10,
                p -> new GuiComponentTextField(p, 736, 60, 36, "New World"));
        pane.addVert(112, 10, p -> new GuiComponentText(p, 36, "Seed:"));
        GuiComponentTextField seed = pane.addVert(112, 10,
                p -> new GuiComponentTextField(p, 736, 60, 36, ""));

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
                List<FilePath> pluginFiles =
                        Collections.singletonList(worldType.file());
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
}
