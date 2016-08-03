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

package org.tobi29.scapes.vanilla.basics.material.item.tool;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.WieldMode;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.server.MobServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;
import org.tobi29.scapes.vanilla.basics.util.ToolUtil;

public abstract class ItemFlintTool extends VanillaItem {
    private TerrainTexture textureHead, textureBuilt;
    private ItemModel modelHead, modelBuilt;

    protected ItemFlintTool(VanillaMaterial materials, String nameID) {
        super(materials, nameID);
    }

    @Override
    public ItemStack example(int data) {
        ItemStack item = super.example(data);
        ToolUtil.createStoneTool(plugin, item, type());
        return item;
    }

    @Override
    public void click(MobPlayerServer entity, ItemStack item) {
        if (item.data() == 0) {
            ItemStack itemHandle = new ItemStack(materials.stick, 0);
            entity.inventories().modify("Container", inventory -> {
                if (inventory.canTake(itemHandle)) {
                    inventory.take(itemHandle);
                    item.setData(1);
                }
            });
        }
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item,
            TerrainServer terrain, int x, int y, int z, Face face) {
        if (item.data() > 0) {
            double damage = item.metaData("Vanilla").getDouble("ToolDamage");
            double modifier = entity.wieldMode() == WieldMode.DUAL ? 1.0 : 2.1;
            item.metaData("Vanilla").setDouble("ToolDamage", damage +
                    item.metaData("Vanilla").getDouble("ToolDamageAdd"));
            return item.metaData("Vanilla").getDouble("ToolEfficiency") *
                    (1.0 - FastMath.tanh(damage)) * modifier;
        } else {
            return 0;
        }
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item, MobServer hit) {
        if (item.data() > 0) {
            double damage = item.metaData("Vanilla").getDouble("ToolDamage");
            double modifier = entity.wieldMode() == WieldMode.DUAL ? 1.0 : 2.1;
            item.metaData("Vanilla").setDouble("ToolDamage", damage);
            return item.metaData("Vanilla").getDouble("ToolStrength") *
                    (1.0 - FastMath.tanh(damage)) * modifier;
        } else {
            return 0;
        }
    }

    @Override
    public int toolLevel(ItemStack item) {
        return item.metaData("Vanilla").getInteger("ToolLevel");
    }

    @Override
    public String toolType(ItemStack item) {
        if (item.data() > 0) {
            return type();
        } else {
            return type() + "Head";
        }
    }

    @Override
    public boolean isTool(ItemStack item) {
        return true;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureHead = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/head/stone/" +
                        type() + ".png");
        textureBuilt = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/built/stone/" +
                        type() + ".png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelHead = new ItemModelSimple(textureHead, 1.0f, 1.0f, 1.0f, 1.0f);
        modelBuilt = new ItemModelSimple(textureBuilt, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader) {
        if (item.data() > 0) {
            modelBuilt.render(gl, shader);
        } else {
            modelHead.render(gl, shader);
        }
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader) {
        if (item.data() > 0) {
            modelBuilt.renderInventory(gl, shader);
        } else {
            modelHead.renderInventory(gl, shader);
        }
    }

    @Override
    public String name(ItemStack item) {
        StringBuilder name = new StringBuilder(100);
        name.append("Flint ").append(type());
        if (item.data() == 0) {
            name.append(" Head");
        }
        double damage = (1.0 - FastMath.tanh(
                item.metaData("Vanilla").getDouble("ToolDamage"))) * 100.0;
        if (damage > 0.1) {
            name.append("\nDamage: ").append(FastMath.floor(damage));
        }
        return name.toString();
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 1;
    }

    public abstract String type();
}
