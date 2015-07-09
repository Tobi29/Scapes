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

package org.tobi29.scapes.vanilla.basics.material.item.tool;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.server.MobServer;
import org.tobi29.scapes.packets.PacketUpdateInventory;
import org.tobi29.scapes.vanilla.basics.material.MetalType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemHeatable;
import org.tobi29.scapes.vanilla.basics.material.item.ItemMetal;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;
import org.tobi29.scapes.vanilla.basics.util.ToolUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ItemTool extends VanillaItem
        implements ItemHeatable, ItemMetal {
    private final Map<MetalType, ItemModel> modelsHead =
            new ConcurrentHashMap<>();
    private TerrainTexture textureHandle, textureHead;
    private ItemModel modelHandle;

    protected ItemTool(VanillaMaterial materials, String nameID) {
        super(materials, nameID);
    }

    @Override
    public ItemStack getExampleStack(int data) {
        ItemStack item = super.getExampleStack((short) 1);
        item.getMetaData("Vanilla").setString("MetalType", "Iron");
        ToolUtil.createTool(plugin, item, getType());
        return item;
    }

    @Override
    public void click(MobPlayerServer entity, ItemStack item) {
        if (item.getData() == 0) {
            ItemStack itemHandle = new ItemStack(materials.stick, (short) 0);
            if (entity.getInventory().canTake(itemHandle)) {
                entity.getInventory().take(itemHandle);
                item.setData((short) 1);
                entity.getConnection().send(new PacketUpdateInventory(entity));
            }
        }
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item,
            TerrainServer terrain, int x, int y, int z, Face face) {
        if (item.getData() > 0) {
            double damage = item.getMetaData("Vanilla").getDouble("ToolDamage");
            item.getMetaData("Vanilla").setDouble("ToolDamage", damage +
                    item.getMetaData("Vanilla").getDouble("ToolDamageAdd"));
            return item.getMetaData("Vanilla").getDouble("ToolEfficiency") *
                    (1.0 - FastMath.tanh(damage));
        } else {
            return 0;
        }
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item, MobServer hit) {
        if (item.getData() > 0) {
            double damage = item.getMetaData("Vanilla").getDouble("ToolDamage");
            item.getMetaData("Vanilla").setDouble("ToolDamage", damage);
            return item.getMetaData("Vanilla").getDouble("ToolStrength") *
                    (1.0 - FastMath.tanh(damage));
        } else {
            return 0;
        }
    }

    @Override
    public int getToolLevel(ItemStack item) {
        return item.getMetaData("Vanilla").getInteger("ToolLevel");
    }

    @Override
    public String getToolType(ItemStack item) {
        if (item.getData() > 0) {
            return getType();
        } else {
            return getType() + "Head";
        }
    }

    @Override
    public boolean isTool(ItemStack item) {
        return true;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureHandle = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/handle/" +
                        getType() + ".png");
        textureHead = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/head/" +
                        getType() + ".png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelHandle =
                new ItemModelSimple(textureHandle, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GraphicsSystem graphics, Shader shader,
            float r, float g, float b, float a) {
        if (item.getData() > 0) {
            modelHandle.render(graphics, shader);
        }
        MetalType metal = plugin.getMetalType(
                item.getMetaData("Vanilla").getString("MetalType"));
        if (metal == null) {
            metal = plugin.getMetalType("Iron");
        }
        getModelHead(metal).render(graphics, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GraphicsSystem graphics,
            Shader shader, float r, float g, float b, float a) {
        if (item.getData() > 0) {
            modelHandle.renderInventory(graphics, shader);
        }
        MetalType metal = plugin.getMetalType(
                item.getMetaData("Vanilla").getString("MetalType"));
        if (metal == null) {
            metal = plugin.getMetalType("Iron");
        }
        getModelHead(metal).renderInventory(graphics, shader);
    }

    @Override
    public String getName(ItemStack item) {
        StringBuilder name = new StringBuilder(100);
        MetalType metalType = getMetalType(item);
        name.append(metalType.getName()).append(' ').append(getType());
        float temperature = getTemperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
            if (temperature > getMeltingPoint(item)) {
                name.append("\n - Liquid");
            }
        }
        double damage = (1.0 - FastMath.tanh(
                item.getMetaData("Vanilla").getDouble("ToolDamage"))) * 100.0;
        if (damage > 0.1) {
            name.append("\nDamage: ").append(FastMath.floor(damage));
        }
        return name.toString();
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 1;
    }

    public abstract String getType();

    @Override
    public void heat(ItemStack item, float temperature) {
        float currentTemperature = getTemperature(item);
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.getMetaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.getMetaData("Vanilla").setFloat("Temperature", FastMath.max(
                    currentTemperature +
                            (temperature - currentTemperature) / 400.0f, 1.1f));
            if (currentTemperature >= getMeltingPoint(item)) {
                item.setMaterial(materials.ingot);
                item.setData((short) 0);
            }
        }
    }

    @Override
    public void cool(ItemStack item) {
        float currentTemperature = getTemperature(item);
        if (currentTemperature < 1) {
            item.getMetaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.getMetaData("Vanilla")
                    .setFloat("Temperature", currentTemperature / 1.002f);
        }
    }

    @Override
    public void cool(MobItemServer item) {
        float currentTemperature = getTemperature(item.getItem());
        if (currentTemperature < 1) {
            item.getItem().getMetaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            if (item.isInWater()) {
                item.getItem().getMetaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 4.0f);
            } else {
                item.getItem().getMetaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 1.002f);
            }
        }
    }

    @Override
    public float getMeltingPoint(ItemStack item) {
        return materials.ingot.getMeltingPoint(item);
    }

    @Override
    public float getTemperature(ItemStack item) {
        return item.getMetaData("Vanilla").getFloat("Temperature");
    }

    @Override
    public MetalType getMetalType(ItemStack item) {
        return plugin.getMetalType(
                item.getMetaData("Vanilla").getString("MetalType"));
    }

    private ItemModel getModelHead(MetalType metal) {
        ItemModel model = modelsHead.get(metal);
        if (model == null) {
            float r = metal.getR();
            float g = metal.getG();
            float b = metal.getB();
            model = new ItemModelSimple(textureHead, r, g, b, 1.0f);
            modelsHead.put(metal, model);
        }
        return model;
    }
}
