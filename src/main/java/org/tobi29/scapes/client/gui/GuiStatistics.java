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

public class GuiStatistics extends GuiMenu {
    public GuiStatistics(GameStateGameMP state,
            List<PlayerStatistics.StatisticMaterial> statisticMaterials,
            GuiStyle style) {
        super(state, "Statistics", style);
        GuiComponentScrollPane scrollPane = pane.addVert(16, 5,
                p -> new GuiComponentScrollPane(p, 368, 350, 70));
        for (PlayerStatistics.StatisticMaterial statisticMaterial : statisticMaterials) {
            scrollPane.addVert(0, 0, p -> new Element(p, statisticMaterial));
        }

        back.addLeftClick(event -> state.client().entity().closeGui());
    }

    private static class Element extends GuiComponentPane {
        public Element(GuiLayoutData parent,
                PlayerStatistics.StatisticMaterial statisticMaterial) {
            super(parent, 378, 70);
            add(70, 20, p -> new GuiComponentTextButton(p, 50, 30, 18,
                    String.valueOf(statisticMaterial.breakAmount())));
            add(130, 20, p -> new GuiComponentTextButton(p, 50, 30, 18,
                    String.valueOf(statisticMaterial.placeAmount())));
            add(190, 20, p -> new GuiComponentTextButton(p, 50, 30, 18,
                    String.valueOf(statisticMaterial.craftAmount())));
            add(15, 15, p -> new GuiComponentItemButton(p, 40, 40,
                    statisticMaterial.type()
                            .example(statisticMaterial.data())));
        }
    }
}
