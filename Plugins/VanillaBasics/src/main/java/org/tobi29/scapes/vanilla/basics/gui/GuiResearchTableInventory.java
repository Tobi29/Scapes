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

package org.tobi29.scapes.vanilla.basics.gui;

import org.tobi29.scapes.client.gui.GuiContainerInventory;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityResearchTableClient;
import org.tobi29.scapes.vanilla.basics.packet.PacketResearch;

public class GuiResearchTableInventory extends GuiContainerInventory {
    public GuiResearchTableInventory(EntityResearchTableClient container,
            MobPlayerClientMain player) {
        super("Research Table", player, container);
        buttonContainer(185, 90, 30, 30, 0);
        GuiComponentTextButton research =
                new GuiComponentTextButton(pane, 27, 210, 120, 30, 18,
                        "Research");
        research.addLeftClick(event -> player.connection()
                .send(new PacketResearch(container)));
    }
}
