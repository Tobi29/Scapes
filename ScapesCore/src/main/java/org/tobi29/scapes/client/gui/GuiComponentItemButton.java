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

package org.tobi29.scapes.client.gui;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.engine.gui.GuiComponentButton;
import org.tobi29.scapes.engine.gui.GuiLayoutData;

public class GuiComponentItemButton extends GuiComponentButton {
    private final GuiComponentItem item;

    public GuiComponentItemButton(GuiLayoutData parent, ItemStack item) {
        super(parent);
        this.item =
                addSubHori(0, 0, -1, -1, p -> new GuiComponentItem(p, item));
    }

    public ItemStack item() {
        return item.item();
    }

    public void setItem(ItemStack item) {
        this.item.setItem(item);
    }
}
