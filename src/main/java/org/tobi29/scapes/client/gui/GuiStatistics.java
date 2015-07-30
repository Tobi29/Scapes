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

import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.server.format.PlayerStatistics;

import java.util.List;

public class GuiStatistics extends Gui {
    public GuiStatistics(GameStateGameMP state,
            List<PlayerStatistics.StatisticMaterial> statisticMaterials) {
        super(GuiAlignment.CENTER);
        GuiComponentVisiblePane pane =
                new GuiComponentVisiblePane(this, 200, 0, 400, 512);
        new GuiComponentText(pane, 16, 16, 32, "Statistics");
        new GuiComponentSeparator(pane, 24, 64, 352, 2);
        GuiComponentScrollPaneList scrollPane =
                new GuiComponentScrollPaneList(pane, 16, 80, 368, 350, 70);
        GuiComponentTextButton back =
                new GuiComponentTextButton(pane, 112, 466, 176, 30, 18, "Back");
        for (PlayerStatistics.StatisticMaterial statisticMaterial : statisticMaterials) {
            new Element(scrollPane, statisticMaterial);
        }
        new GuiComponentSeparator(pane, 24, 448, 352, 2);

        back.addLeftClick(event -> state.client().entity().closeGui());
    }

    private static class Element extends GuiComponentPane {
        public Element(GuiComponent parent,
                PlayerStatistics.StatisticMaterial statisticMaterial) {
            super(parent, 0, 0, 378, 70);
            new GuiComponentTextButton(this, 70, 20, 50, 30, 18,
                    String.valueOf(statisticMaterial.breakAmount()));
            new GuiComponentTextButton(this, 130, 20, 50, 30, 18,
                    String.valueOf(statisticMaterial.placeAmount()));
            new GuiComponentTextButton(this, 190, 20, 50, 30, 18,
                    String.valueOf(statisticMaterial.craftAmount()));
            new GuiComponentItemButton(this, 15, 15, 40, 40,
                    statisticMaterial.type().example(statisticMaterial.data()));
        }
    }
}
