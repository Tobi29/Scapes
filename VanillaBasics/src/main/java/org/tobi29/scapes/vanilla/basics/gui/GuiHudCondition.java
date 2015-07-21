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

import org.tobi29.scapes.client.gui.GuiComponentBar;
import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class GuiHudCondition extends GuiComponent {
    private final MobPlayerClientMain player;

    public GuiHudCondition(MobPlayerClientMain player) {
        super(8, 418, 560, 24);
        this.player = player;
        add(new GuiComponentBar(0, 0, 560, 8, 0.0f, 1.0f, 0.0f, 0.6f,
                () -> value("Stamina")));
        add(new GuiComponentBar(280, 8, 280, 8, 1.0f, 0.5f, 0.0f, 0.6f,
                () -> value("Hunger")));
        add(new GuiComponentBar(280, 16, 280, 8, 0.0f, 0.2f, 1.0f, 0.6f,
                () -> value("Thirst")));
    }

    private double value(String name) {
        TagStructure conditionTag =
                player.metaData("Vanilla").getStructure("Condition");
        return conditionTag.getDouble(name);
    }
}
