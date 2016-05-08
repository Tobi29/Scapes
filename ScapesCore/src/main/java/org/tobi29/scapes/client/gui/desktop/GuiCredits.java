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
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;

import java.io.BufferedReader;
import java.io.IOException;

public class GuiCredits extends GuiDesktop {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GuiCredits.class);

    public GuiCredits(GameState state, Gui previous, GuiStyle style) {
        super(state, style);
        StringBuilder credits = new StringBuilder(200);
        try (BufferedReader reader = state.engine().files()
                .get("Scapes:Readme.txt").reader()) {
            String line = reader.readLine();
            while (line != null) {
                credits.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            LOGGER.error("Error reading Readme.txt: {}", e.toString());
        }
        addHori(0, 0, 120, -1, GuiComponentGroup::new);
        addHori(0, 0, -1, 18,
                p -> new GuiComponentCredits(p, credits.toString()));
        GuiComponentVisiblePane pane =
                addHori(0, 0, 96, -1, GuiComponentVisiblePane::new);
        GuiComponentTextButton back =
                pane.addVert(13, 64, -1, 30, p -> button(p, "Back"));
        back.onClickLeft(event -> {
            state.engine().sounds().stop("music.Credits");
            state.engine().guiStack().add("10-Menu", previous);
        });
        state.engine().sounds().stop("music");
        state.engine().sounds()
                .playMusic("Scapes:sound/Credits.ogg", "music.Credits", 1.0f,
                        1.0f, true);
    }
}
