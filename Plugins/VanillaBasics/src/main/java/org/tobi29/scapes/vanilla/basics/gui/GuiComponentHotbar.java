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

import org.tobi29.scapes.client.gui.GuiComponentHotbarButton;
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;

public class GuiComponentHotbar extends GuiComponentGroupSlab {
    public GuiComponentHotbar(GuiLayoutData parent,
            MobPlayerClientMainVB player) {
        super(parent);
        player.inventories().access("Container", inventory -> {
            for (int i = 0; i < 10; i++) {
                int j = i;
                addHori(5, 5, -1, -1,
                        p -> new GuiComponentHotbarButton(p, inventory.item(j),
                                player, j));
            }
        });
    }
}
