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

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.client.gui.GuiComponentItemButton;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.client.EntityAnvilClient;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.packet.PacketAnvil;

public class GuiAnvilInventory extends GuiContainerInventory {
    private final EntityAnvilClient container;

    public GuiAnvilInventory(EntityAnvilClient container,
            MobPlayerClientMainVB player, GuiStyle style) {
        super("Anvil", player, container, style);
        this.container = container;
        VanillaBasics plugin = (VanillaBasics) player.connection().plugins()
                .plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        buttonContainer(16, 120, 30, 30, 0);
        buttonContainer(16, 210, 30, 30, 1);
        addAction(66, 120, materials.ingot, 0);
        addAction(106, 120, materials.pickaxe, 1);
        addAction(106, 160, materials.axe, 2);
        addAction(106, 200, materials.shovel, 3);
        addAction(146, 120, materials.hammer, 4);
        addAction(146, 160, materials.saw, 5);
        addAction(146, 200, materials.hoe, 6);
        addAction(186, 120, materials.sword, 7);
    }

    private void addAction(int x, int y, Material material, int i) {
        ItemStack icon = new ItemStack(material, 1);
        icon.metaData("Vanilla").setString("MetalType", "Iron");
        GuiComponentItemButton item = pane.add(x, y,
                p -> new GuiComponentItemButton(p, 30, 30, icon));
        item.onClickLeft(event -> player.connection()
                .send(new PacketAnvil(container, i)));
    }
}
