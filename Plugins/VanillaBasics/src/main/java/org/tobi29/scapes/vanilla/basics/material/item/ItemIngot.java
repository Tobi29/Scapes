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
package org.tobi29.scapes.vanilla.basics.material.item;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.vanilla.basics.material.AlloyType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.util.MetalUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemIngot extends VanillaItem implements ItemMetal {
    private final Map<AlloyType, ItemModel> modelsShaped =
            new ConcurrentHashMap<>(), modelsRaw = new ConcurrentHashMap<>();
    private TerrainTexture textureShaped, textureRaw, textureMold;
    private ItemModel modelMold;

    public ItemIngot(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.Ingot");
    }

    @Override
    public ItemStack example(int data) {
        ItemStack item = super.example(data);
        MetalUtil.Alloy alloy = new MetalUtil.Alloy();
        alloy.add(plugin.metalType("Iron"), 1.0);
        setAlloy(item, alloy);
        return item;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureShaped = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Shaped.png");
        textureRaw = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Raw.png");
        textureMold = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/MoldFilled.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelMold = new ItemModelSimple(textureMold, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader) {
        AlloyType alloyType = alloy(item).type(plugin);
        switch (item.data()) {
            case 1:
                modelShaped(alloyType).render(gl, shader);
                break;
            default:
                modelRaw(alloyType).render(gl, shader);
                modelMold.render(gl, shader);
                break;
        }
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader) {
        AlloyType alloyType = alloy(item).type(plugin);
        switch (item.data()) {
            case 1:
                modelShaped(alloyType).renderInventory(gl, shader);
                break;
            default:
                modelRaw(alloyType).renderInventory(gl, shader);
                modelMold.renderInventory(gl, shader);
                break;
        }
    }

    @Override
    public String name(ItemStack item) {
        StringBuilder name = new StringBuilder(50);
        if (item.data() == 0) {
            name.append("Unshaped ");
        }
        MetalUtil.Alloy alloy = alloy(item);
        AlloyType alloyType = alloy.type(plugin);
        name.append(alloyType.ingotName());
        float temperature = temperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
            if (temperature > meltingPoint(item)) {
                name.append("\n - Liquid");
            }
        }
        return name.toString();
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 1;
    }

    @Override
    public void heat(ItemStack item, float temperature) {
        float currentTemperature = temperature(item);
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            currentTemperature += (temperature - currentTemperature) / 400.0f;
            item.metaData("Vanilla")
                    .setFloat("Temperature", currentTemperature);
            if (currentTemperature >= meltingPoint(item) && item.data() == 1) {
                item.setAmount(0);
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
                        .setFloat("Temperature", currentTemperature / 1.1f);
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

    private ItemModel modelRaw(AlloyType alloy) {
        ItemModel model = modelsRaw.get(alloy);
        if (model == null) {
            float r = alloy.r();
            float g = alloy.g();
            float b = alloy.b();
            model = new ItemModelSimple(textureRaw, r, g, b, 1.0f);
            modelsRaw.put(alloy, model);
        }
        return model;
    }

    private ItemModel modelShaped(AlloyType alloy) {
        ItemModel model = modelsShaped.get(alloy);
        if (model == null) {
            float r = alloy.r();
            float g = alloy.g();
            float b = alloy.b();
            model = new ItemModelSimple(textureShaped, r, g, b, 1.0f);
            modelsShaped.put(alloy, model);
        }
        return model;
    }
}
