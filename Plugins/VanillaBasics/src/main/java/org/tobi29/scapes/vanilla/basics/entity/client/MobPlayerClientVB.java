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
package org.tobi29.scapes.vanilla.basics.entity.client;

import java8.util.Optional;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.InventoryContainer;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.graphics.Texture;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.WieldMode;
import org.tobi29.scapes.entity.client.EntityContainerClient;
import org.tobi29.scapes.entity.client.MobPlayerClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.model.MobLivingModelHuman;
import org.tobi29.scapes.entity.model.MobModel;

public class MobPlayerClientVB extends MobPlayerClient
        implements EntityContainerClient {
    protected final InventoryContainer inventories;

    public MobPlayerClientVB(WorldClient world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO, 0.0, 0.0, "");
    }

    public MobPlayerClientVB(WorldClient world, Vector3 pos, Vector3 speed,
            double xRot, double zRot, String nickname) {
        super(world, pos, speed, new AABB(-0.4, -0.4, -1, 0.4, 0.4, 0.9), 100,
                100, nickname);
        inventories = new InventoryContainer();
        inventories.add("Container", new Inventory(registry, 40));
        inventories.add("Hold", new Inventory(registry, 1));
        rot.setX(xRot);
        rot.setZ(zRot);
    }

    @Override
    public ItemStack leftWeapon() {
        return inventories.accessReturn("Container",
                inventory -> inventory.item(inventorySelectLeft));
    }

    @Override
    public ItemStack rightWeapon() {
        return inventories.accessReturn("Container",
                inventory -> inventory.item(inventorySelectRight));
    }

    @Override
    public WieldMode wieldMode() {
        return inventorySelectLeft == inventorySelectRight ? WieldMode.RIGHT :
                WieldMode.DUAL;
    }

    @Override
    public Vector3 viewOffset() {
        return new Vector3d(0.0, 0.0, 0.63);
    }

    @Override
    public Optional<Gui> gui(MobPlayerClientMain player) {
        // TODO: Trade or steal UI maybe?
        return Optional.empty();
    }

    @Override
    public InventoryContainer inventories() {
        return inventories;
    }

    @Override
    public Optional<MobModel> createModel() {
        Texture texture = world.scene().skinStorage().get(skin);
        return Optional
                .of(new MobLivingModelHuman(world.game().modelHumanShared(),
                        this, texture));
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        inventories.forEach((id, inventory) -> inventory
                .load(inventoryTag.getStructure(id)));
    }
}
