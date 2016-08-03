/*
 * Copyright 2012-2016 Tobi29
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

import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityChestClient;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

import java.util.ArrayList;
import java.util.List;

public class GuiChestInventory extends GuiContainerInventory {
    public GuiChestInventory(EntityChestClient container,
            MobPlayerClientMainVB player, GuiStyle style) {
        super("Chest", player, container, style);
        container.inventories().access("Container", inventory -> {
            int x = -1, y = 0, xx, yy = 91;
            List<GuiComponent> buttons = new ArrayList<>(10);
            for (int i = 0; i < inventory.size(); i++) {
                if (++x >= 10) {
                    y++;
                    yy = y * 35 + 91;
                    x = 0;
                    selection(buttons);
                    buttons.clear();
                }
                xx = x * 35 + 27;
                buttons.add(buttonContainer(xx, yy, 30, 30, i));
            }
            selection(buttons);
        });
    }
}
