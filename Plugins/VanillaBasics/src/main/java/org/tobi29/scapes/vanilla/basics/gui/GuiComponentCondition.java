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

import org.tobi29.scapes.client.gui.GuiComponentBar;
import org.tobi29.scapes.engine.gui.GuiComponentGroup;
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

public class GuiComponentCondition extends GuiComponentGroup {
    private final MobPlayerClientMain player;

    public GuiComponentCondition(GuiLayoutData parent,
            MobPlayerClientMain player) {
        super(parent);
        this.player = player;
        addVert(0, 0, -1, -1,
                p -> new GuiComponentBar(p, 0.0f, 1.0f, 0.0f, 0.6f, 1.0,
                        () -> value("Stamina")));
        GuiComponentGroupSlab bottom =
                addVert(0, 0, -1, -2, GuiComponentGroupSlab::new);
        bottom.addHori(0, 0, -1, -1,
                p -> new GuiComponentBar(p, 1.0f, 0.0f, 0.0f, 0.6f, 1.0,
                        () -> player.health() / player.maxHealth()));
        GuiComponentGroup bottomRight =
                bottom.addHori(0, 0, -1, -1, GuiComponentGroup::new);
        bottomRight.addVert(0, 0, -1, -1,
                p -> new GuiComponentBar(p, 1.0f, 0.5f, 0.0f, 0.6f, 1.0,
                        () -> value("Hunger")));
        bottomRight.addVert(0, 0, -1, -1,
                p -> new GuiComponentBar(p, 0.0f, 0.2f, 1.0f, 0.6f, 1.0,
                        () -> value("Thirst")));
    }

    private double value(String name) {
        TagStructure conditionTag =
                player.metaData("Vanilla").getStructure("Condition");
        return conditionTag.getDouble(name);
    }
}
