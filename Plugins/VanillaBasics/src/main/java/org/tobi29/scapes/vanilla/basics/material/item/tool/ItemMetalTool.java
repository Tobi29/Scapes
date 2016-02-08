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

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.WieldMode;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.server.MobServer;
import org.tobi29.scapes.packets.PacketUpdateInventory;
import org.tobi29.scapes.vanilla.basics.material.AlloyType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemMetal;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;
import org.tobi29.scapes.vanilla.basics.util.MetalUtil;
import org.tobi29.scapes.vanilla.basics.util.ToolUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ItemMetalTool extends VanillaItem implements ItemMetal {
    private final Map<AlloyType, ItemModel> modelsHead =
            new ConcurrentHashMap<>();
    private final Map<AlloyType, ItemModel> modelsBuilt =
            new ConcurrentHashMap<>();
    private TerrainTexture textureHead, textureBuilt, textureHandle;
    private ItemModel modelHandle;

    protected ItemMetalTool(VanillaMaterial materials, String nameID) {
        super(materials, nameID);
    }

    @Override
    public ItemStack example(int data) {
        ItemStack item = materials.ingot.example(data);
        ToolUtil.createTool(plugin, item, type());
        return item;
    }

    @Override
    public void click(MobPlayerServer entity, ItemStack item) {
        if (item.data() == 0) {
            ItemStack itemHandle = new ItemStack(materials.stick, 0);
            Inventory inventory = entity.inventory("Container");
            if (inventory.canTake(itemHandle)) {
                inventory.take(itemHandle);
                item.setData(1);
                entity.connection()
                        .send(new PacketUpdateInventory(entity, "Container"));
            }
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
                "VanillaBasics:image/terrain/tools/head/metal/" +
                        type() + ".png");
        textureBuilt = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/built/metal/" +
                        type() + ".png");
        textureHandle = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/handle/metal/" +
                        type() + ".png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelHandle =
                new ItemModelSimple(textureHandle, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        AlloyType alloyType = alloy(item).type(plugin);
        if (item.data() > 0) {
            modelHandle.render(gl, shader);
            modelBuilt(alloyType).render(gl, shader);
        } else {
            modelHead(alloyType).render(gl, shader);
        }
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        AlloyType alloyType = alloy(item).type(plugin);
        if (item.data() > 0) {
            modelHandle.renderInventory(gl, shader);
            modelBuilt(alloyType).renderInventory(gl, shader);
        } else {
            modelHead(alloyType).renderInventory(gl, shader);
        }
    }

    @Override
    public String name(ItemStack item) {
        StringBuilder name = new StringBuilder(100);
        MetalUtil.Alloy alloy = alloy(item);
        AlloyType alloyType = alloy.type(plugin);
        name.append(alloyType.name()).append(' ').append(type());
        float temperature = temperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
            if (temperature > meltingPoint(item)) {
                name.append("\n - Liquid");
            }
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

    @Override
    public void heat(ItemStack item, float temperature) {
        float currentTemperature = temperature(item);
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            currentTemperature += (temperature - currentTemperature) / 400.0f;
            item.metaData("Vanilla")
                    .setFloat("Temperature", currentTemperature);
            if (currentTemperature >= meltingPoint(item)) {
                item.setMaterial(materials.ingot);
                item.setData(0);
            }
        }
    }

    @Override
    public void cool(ItemStack item) {
        float currentTemperature = temperature(item);
        if (currentTemperature < 1) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.metaData("Vanilla")
                    .setFloat("Temperature", currentTemperature / 1.002f);
        }
    }

    @Override
    public void cool(MobItemServer item) {
        float currentTemperature = temperature(item.item());
        if (currentTemperature < 1) {
            item.item().metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            if (item.isInWater()) {
                item.item().metaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 4.0f);
            } else {
                item.item().metaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 1.002f);
            }
        }
    }

    @Override
    public float temperature(ItemStack item) {
        return item.metaData("Vanilla").getFloat("Temperature");
    }

    @Override
    public MetalUtil.Alloy alloy(ItemStack item) {
        return MetalUtil
                .read(plugin, item.metaData("Vanilla").getStructure("Alloy"));
    }

    @Override
    public void setAlloy(ItemStack item, MetalUtil.Alloy alloy) {
        item.metaData("Vanilla").setStructure("Alloy", MetalUtil.write(alloy));
    }

    private ItemModel modelHead(AlloyType alloyType) {
        ItemModel model = modelsHead.get(alloyType);
        if (model == null) {
            float r = alloyType.r();
            float g = alloyType.g();
            float b = alloyType.b();
            model = new ItemModelSimple(textureHead, r, g, b, 1.0f);
            modelsHead.put(alloyType, model);
        }
        return model;
    }

    private ItemModel modelBuilt(AlloyType alloyType) {
        ItemModel model = modelsBuilt.get(alloyType);
        if (model == null) {
            float r = alloyType.r();
            float g = alloyType.g();
            float b = alloyType.b();
            model = new ItemModelSimple(textureBuilt, r, g, b, 1.0f);
            modelsBuilt.put(alloyType, model);
        }
        return model;
    }
}
