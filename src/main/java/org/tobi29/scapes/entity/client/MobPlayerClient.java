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

package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.gui.GuiPlayerInventory;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.model.MobLivingModelHuman;
import org.tobi29.scapes.entity.model.MobModel;

import java.util.Optional;

public class MobPlayerClient extends MobLivingEquippedClient
        implements EntityContainerClient {
    protected final Inventory inventory;
    protected int inventorySelectLeft, inventorySelectRight = 9, healWait;
    protected String nickname;
    private byte[] skin;

    public MobPlayerClient(WorldClient world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO, 0.0, 0.0, "");
    }

    public MobPlayerClient(WorldClient world, Vector3 pos, Vector3 speed,
            double xRot, double zRot, String nickname) {
        super(world, pos, speed, new AABB(-0.4, -0.4, -1, 0.4, 0.4, 0.9), 100,
                100, new Frustum(90, 1, 0.1, 24), new Frustum(50, 1, 0.1, 2));
        rot.setX(xRot);
        rot.setZ(zRot);
        this.nickname = nickname;
        inventory = new Inventory(registry, 44);
    }

    @Override
    public ItemStack leftWeapon() {
        return inventory.item(inventorySelectLeft);
    }

    @Override
    public ItemStack rightWeapon() {
        return inventory.item(inventorySelectRight);
    }

    public int inventorySelectLeft() {
        return inventorySelectLeft;
    }

    public void setInventorySelectLeft(int select) {
        int c = 1;
        if (select < inventorySelectLeft) {
            c = -1;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        if (select == inventorySelectRight) {
            select += c;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        inventorySelectLeft = select;
    }

    public int inventorySelectRight() {
        return inventorySelectRight;
    }

    public void setInventorySelectRight(int select) {
        int c = 1;
        if (select < inventorySelectRight) {
            c = -1;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        if (select == inventorySelectLeft) {
            select += c;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        inventorySelectRight = select;
    }

    public String nickname() {
        return nickname;
    }

    public PointerPane selectedBlock() {
        return block(6);
    }

    @Override
    public CreatureType creatureType() {
        return CreatureType.CREATURE;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        healWait = tagStructure.getInteger("HealWait");
        inventory.load(tagStructure.getStructure("Inventory"));
        if (tagStructure.has("Nickname")) {
            nickname = tagStructure.getString("Nickname");
        }
        if (tagStructure.has("SkinChecksum")) {
            skin = tagStructure.getByteArray("SkinChecksum");
        }
    }

    @Override
    public Vector3 viewOffset() {
        return new Vector3d(0.0, 0.0, 0.63);
    }

    @Override
    public Gui gui(MobPlayerClientMain player) {
        return new GuiPlayerInventory(player);
    }

    @Override
    public Inventory inventory() {
        return inventory;
    }

    @Override
    public Optional<MobModel> createModel() {
        Texture texture = world.scene().skinStorage().get(skin);
        return Optional.of(new MobLivingModelHuman(this, texture));
    }
}
