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
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.vanilla.basics.material.MetalType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemIngot extends VanillaItem implements ItemHeatable, ItemMetal {
    private final Map<MetalType, ItemModel> modelsShaped =
            new ConcurrentHashMap<>(), modelsRaw = new ConcurrentHashMap<>();
    private TerrainTexture textureShaped, textureRaw, textureMold, textureStone;
    private ItemModel modelMold, modelStone;

    public ItemIngot(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.Ingot");
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureShaped = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Shaped.png");
        textureRaw = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Raw.png");
        textureMold = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/MoldFilled.png");
        textureStone = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Stone.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelMold = new ItemModelSimple(textureMold, 1.0f, 1.0f, 1.0f, 1.0f);
        modelStone = new ItemModelSimple(textureStone, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GraphicsSystem graphics, Shader shader,
            float r, float g, float b, float a) {
        if ("Stone"
                .equals(item.getMetaData("Vanilla").getString("MetalType"))) {
            modelStone.render(graphics, shader);
        } else {
            MetalType metal = plugin.getMetalType(
                    item.getMetaData("Vanilla").getString("MetalType"));
            if (metal == null) {
                metal = plugin.getMetalType("Iron");
            }
            switch (item.getData()) {
                case 1:
                    getModelShaped(metal).render(graphics, shader);
                    break;
                default:
                    getModelRaw(metal).render(graphics, shader);
                    modelMold.render(graphics, shader);
                    break;
            }
        }
    }

    @Override
    public void renderInventory(ItemStack item, GraphicsSystem graphics,
            Shader shader, float r, float g, float b, float a) {
        if ("Stone"
                .equals(item.getMetaData("Vanilla").getString("MetalType"))) {
            modelStone.renderInventory(graphics, shader);
        } else {
            MetalType metal = plugin.getMetalType(
                    item.getMetaData("Vanilla").getString("MetalType"));
            if (metal == null) {
                metal = plugin.getMetalType("Iron");
            }
            switch (item.getData()) {
                case 1:
                    getModelShaped(metal).renderInventory(graphics, shader);
                    break;
                default:
                    getModelRaw(metal).renderInventory(graphics, shader);
                    modelMold.renderInventory(graphics, shader);
                    break;
            }
        }
    }

    @Override
    public String getName(ItemStack item) {
        StringBuilder name = new StringBuilder(50);
        if (item.getData() == 0) {
            name.append("Unshaped ");
        }
        MetalType metalType = getMetalType(item);
        name.append(metalType.getIngotName());
        float temperature = getTemperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
            if (temperature > getMeltingPoint(item)) {
                name.append("\n - Liquid");
            }
        }
        return name.toString();
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 1;
    }

    @Override
    public void heat(ItemStack item, float temperature) {
        float currentTemperature = getTemperature(item);
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.getMetaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.getMetaData("Vanilla").setFloat("Temperature", FastMath.max(
                    currentTemperature +
                            (temperature - currentTemperature) / 400.0f, 1.1f));
            if (currentTemperature >= getMeltingPoint(item) &&
                    item.getData() == 1) {
                item.setAmount(0);
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
                        .setFloat("Temperature", currentTemperature / 1.1f);
            } else {
                item.getItem().getMetaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 1.002f);
            }
        }
    }

    @Override
    public float getMeltingPoint(ItemStack item) {
        MetalType metal = plugin.getMetalType(
                item.getMetaData("Vanilla").getString("MetalType"));
        if (metal != null) {
            return metal.getMeltingPoint();
        }
        return 100.0f;
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

    private ItemModel getModelRaw(MetalType metal) {
        ItemModel model = modelsRaw.get(metal);
        if (model == null) {
            float r = metal.getR();
            float g = metal.getG();
            float b = metal.getB();
            model = new ItemModelSimple(textureRaw, r, g, b, 1.0f);
            modelsRaw.put(metal, model);
        }
        return model;
    }

    private ItemModel getModelShaped(MetalType metal) {
        ItemModel model = modelsShaped.get(metal);
        if (model == null) {
            float r = metal.getR();
            float g = metal.getG();
            float b = metal.getB();
            model = new ItemModelSimple(textureShaped, r, g, b, 1.0f);
            modelsShaped.put(metal, model);
        }
        return model;
    }
}
